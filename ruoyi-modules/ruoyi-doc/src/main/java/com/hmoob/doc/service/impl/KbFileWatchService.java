package com.hmoob.doc.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.config.KbFileWatchProperties;
import com.hmoob.doc.domain.KbFileChangeLog;
import com.hmoob.doc.domain.KbFileWatchConfig;
import com.hmoob.doc.domain.vo.KbFileChangeLogVo;
import com.hmoob.doc.domain.vo.KbFileWatchConfigVo;
import com.hmoob.doc.mapper.KbFileChangeLogMapper;
import com.hmoob.doc.mapper.KbFileWatchConfigMapper;
import com.hmoob.doc.watch.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * KB文件监控服务
 * 整合WatchService实时监听和定时快照扫描的双轨制文件监控
 *
 * @author hmoob
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kb.file-watch", name = "enabled", havingValue = "true", matchIfMissing = false)
public class KbFileWatchService {

    private final KbFileWatchProperties watchProperties;
    private final KbFileWatchConfigMapper watchConfigMapper;
    private final KbFileChangeLogMapper changeLogMapper;
    private final SnapshotEngine snapshotEngine;
    private final FileChangeHandlerService changeHandlerService;

    /**
     * 各监控配置对应的Watcher实例
     */
    private final Map<Long, RealtimeWatcher> watcherMap = new ConcurrentHashMap<>();

    /**
     * 各监控配置对应的当前快照
     */
    private final Map<Long, FileSnapshot> snapshotMap = new ConcurrentHashMap<>();

    /**
     * 定时扫描调度器
     */
    private ScheduledExecutorService scanScheduler;

    /**
     * 应用启动后自动开始监控（非阻塞，失败不中断应用）
     */
    @PostConstruct
    public void init() {
        if (!watchProperties.isAutoStart()) {
            log.info("文件监控服务已禁用自动启动");
            return;
        }
        try {
            startAllWatchers();
        } catch (Exception e) {
            log.warn("文件监控服务启动失败，可通过API手动启动: {}", e.getMessage());
        }
    }

    /**
     * 应用关闭前停止监控
     */
    @PreDestroy
    public void destroy() {
        stopAllWatchers();
    }

    /**
     * 启动所有配置为启用状态的监控
     */
    public void startAllWatchers() {
        log.info("====== 开始启动文件监控服务 ======");

        List<KbFileWatchConfig> configs;
        try {
            configs = watchConfigMapper.selectList(
                Wrappers.<KbFileWatchConfig>lambdaQuery()
                    .eq(KbFileWatchConfig::getStatus, 1)
                    .eq(KbFileWatchConfig::getDelFlag, SystemConstants.NORMAL)
            );
        } catch (Exception e) {
            log.warn("查询监控配置失败（可能表未创建）: {}", e.getMessage());
            return;
        }

        if (configs.isEmpty()) {
            log.info("没有启用的监控配置，跳过启动");
            return;
        }

        // 初始化定时扫描器
        scanScheduler = Executors.newScheduledThreadPool(2);

        for (KbFileWatchConfig config : configs) {
            try {
                startWatcher(config);
            } catch (Exception e) {
                log.error("启动监控失败: configId={}, path={}", config.getConfigId(), config.getWatchPath(), e);
            }
        }

        log.info("====== 文件监控服务启动完成，共{}个监控任务 ======", watcherMap.size());
    }

    /**
     * 启动单个监控
     */
    public void startWatcher(KbFileWatchConfig config) throws IOException {
        Path watchPath = Path.of(config.getWatchPath());

        if (!Files.exists(watchPath) || !Files.isDirectory(watchPath)) {
            log.warn("监控路径不存在或不是目录: {}", config.getWatchPath());
            return;
        }

        // 已有监控实例则先停止
        stopWatcher(config.getConfigId());

        boolean recursive = config.getRecursive() != null && config.getRecursive() == 1;
        int debounceMs = config.getDebounceMs() != null ? config.getDebounceMs() : watchProperties.getDebounceMs();
        String excludePattern = StrUtil.isNotBlank(config.getExcludePattern())
            ? config.getExcludePattern()
            : watchProperties.getDefaultExcludePattern();

        // 1. 建立初始快照基线
        log.info("建立快照基线: {} (recursive={})", watchPath, recursive);
        FileSnapshot baseline = snapshotEngine.takeSnapshot(watchPath, recursive, excludePattern);
        snapshotMap.put(config.getConfigId(), baseline);
        log.info("快照基线建立完成: {} 个文件/目录", baseline.size());

        // 2. 启动实时WatchService
        RealtimeWatcher watcher = new RealtimeWatcher(debounceMs);
        watcher.register(watchPath, recursive);
        watcher.startWatching(event -> {
            changeHandlerService.handleChangeEvent(event, config);
        });
        watcherMap.put(config.getConfigId(), watcher);

        // 3. 定时全量扫描兜底
        int scanInterval = config.getScanInterval() != null ? config.getScanInterval() : watchProperties.getScanInterval();
        if (scanScheduler != null && !scanScheduler.isShutdown()) {
            scanScheduler.scheduleAtFixedRate(() -> {
                try {
                    performFullScan(config, excludePattern);
                } catch (Exception e) {
                    log.error("定时全量扫描失败: configId={}", config.getConfigId(), e);
                }
            }, scanInterval, scanInterval, TimeUnit.SECONDS);
        }

        log.info("监控启动成功: configId={}, path={}", config.getConfigId(), config.getWatchPath());
    }

    /**
     * 执行全量扫描（兜底机制）
     */
    private void performFullScan(KbFileWatchConfig config, String excludePattern) throws IOException {
        Path watchPath = Path.of(config.getWatchPath());
        if (!Files.exists(watchPath)) {
            return;
        }

        boolean recursive = config.getRecursive() != null && config.getRecursive() == 1;
        FileSnapshot currentSnapshot = snapshotMap.get(config.getConfigId());
        if (currentSnapshot == null) {
            currentSnapshot = FileSnapshot.empty();
        }

        FileSnapshot newSnapshot = snapshotEngine.takeSnapshot(watchPath, recursive, excludePattern);
        List<ChangeEvent> changes = snapshotEngine.diff(currentSnapshot, newSnapshot);
        snapshotMap.put(config.getConfigId(), newSnapshot);

        if (!changes.isEmpty()) {
            log.info("全量扫描发现 {} 个变化: configId={}", changes.size(), config.getConfigId());
            for (ChangeEvent change : changes) {
                changeHandlerService.handleChangeEvent(change, config);
            }
        }
    }

    /**
     * 停止单个监控
     */
    public void stopWatcher(Long configId) {
        RealtimeWatcher watcher = watcherMap.remove(configId);
        if (watcher != null) {
            watcher.stopWatching();
            log.info("已停止监控: configId={}", configId);
        }
        snapshotMap.remove(configId);
    }

    /**
     * 停止所有监控
     */
    public void stopAllWatchers() {
        log.info("停止所有文件监控...");
        watcherMap.forEach((id, watcher) -> watcher.stopWatching());
        watcherMap.clear();
        snapshotMap.clear();
        if (scanScheduler != null) {
            scanScheduler.shutdownNow();
        }
        log.info("所有文件监控已停止");
    }

    /**
     * 手动触发全量扫描
     */
    public int triggerFullScan(Long configId) throws IOException {
        KbFileWatchConfig config = watchConfigMapper.selectById(configId);
        if (config == null) {
            throw new IllegalArgumentException("监控配置不存在: " + configId);
        }
        String excludePattern = StrUtil.isNotBlank(config.getExcludePattern())
            ? config.getExcludePattern()
            : watchProperties.getDefaultExcludePattern();

        Path watchPath = Path.of(config.getWatchPath());
        boolean recursive = config.getRecursive() != null && config.getRecursive() == 1;

        FileSnapshot currentSnapshot = snapshotMap.getOrDefault(configId, FileSnapshot.empty());
        FileSnapshot newSnapshot = snapshotEngine.takeSnapshot(watchPath, recursive, excludePattern);
        List<ChangeEvent> changes = snapshotEngine.diff(currentSnapshot, newSnapshot);
        snapshotMap.put(configId, newSnapshot);

        for (ChangeEvent change : changes) {
            changeHandlerService.handleChangeEvent(change, config);
        }
        return changes.size();
    }

    // ==================== 配置管理 ====================

    /**
     * 查询监控配置列表
     */
    public List<KbFileWatchConfigVo> listConfigs() {
        return watchConfigMapper.selectVoList(
            Wrappers.<KbFileWatchConfig>lambdaQuery()
                .eq(KbFileWatchConfig::getDelFlag, SystemConstants.NORMAL)
                .orderByDesc(KbFileWatchConfig::getCreateTime)
        );
    }

    /**
     * 获取单个配置详情
     */
    public KbFileWatchConfigVo getConfig(Long configId) {
        return watchConfigMapper.selectVoById(configId);
    }

    /**
     * 新增监控配置
     */
    public int addConfig(KbFileWatchConfig config) {
        config.setDelFlag(SystemConstants.NORMAL);
        return watchConfigMapper.insert(config);
    }

    /**
     * 更新监控配置
     */
    public int updateConfig(KbFileWatchConfig config) {
        return watchConfigMapper.updateById(config);
    }

    /**
     * 删除监控配置（逻辑删除）
     */
    public int deleteConfig(Long configId) {
        stopWatcher(configId);
        return watchConfigMapper.deleteById(configId);
    }

    /**
     * 启用/禁用监控
     */
    public void toggleConfig(Long configId, boolean enable) throws IOException {
        KbFileWatchConfig config = watchConfigMapper.selectById(configId);
        if (config == null) {
            return;
        }
        config.setStatus(enable ? 1 : 0);
        watchConfigMapper.updateById(config);

        if (enable) {
            startWatcher(config);
        } else {
            stopWatcher(configId);
        }
    }

    // ==================== 变化记录查询 ====================

    /**
     * 分页查询变化记录
     */
    public TableDataInfo<KbFileChangeLogVo> pageChangeLogs(Long configId, String changeType, Integer processStatus, PageQuery pageQuery) {
        LambdaQueryWrapper<KbFileChangeLog> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(configId != null, KbFileChangeLog::getConfigId, configId);
        wrapper.eq(StrUtil.isNotBlank(changeType), KbFileChangeLog::getChangeType, changeType);
        wrapper.eq(processStatus != null, KbFileChangeLog::getProcessStatus, processStatus);
        wrapper.orderByDesc(KbFileChangeLog::getCreateTime);

        Page<KbFileChangeLogVo> page = changeLogMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 获取监控状态信息
     */
    public Map<String, Object> getWatchStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("enabled", watchProperties.isEnabled());
        status.put("activeWatchers", watcherMap.size());
        status.put("watcherDetails", watcherMap.entrySet().stream()
            .map(e -> Map.of("configId", e.getKey(), "running", e.getValue().isRunning()))
            .toList());
        return status;
    }
}
