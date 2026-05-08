package com.hmoob.doc.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.hmoob.doc.enums.ChangeProcessStatusEnum;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * KB文件监控服务
 * 采用纯快照对比方案（无 WatchService），支持 10w+ 文件
 *
 * 核心特性：
 * 1. 定时快照扫描（间隔可配置）
 * 2. Fork/Join 并发扫描
 * 3. 事件顺序处理（确保事件按正确顺序执行）
 * 4. 目录事件优化（目录删除/修改时，子项不单独触发）
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
     * 各监控配置对应的当前快照
     */
    private final Map<Long, FileSnapshot> snapshotMap = new ConcurrentHashMap<>();

    /**
     * 定时扫描调度器
     */
    private ScheduledExecutorService scanScheduler;

    /**
     * 事件处理队列（确保顺序处理）
     */
    private final Map<Long, ExecutorService> eventExecutors = new ConcurrentHashMap<>();

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
        log.info("====== 开始启动文件监控服务（纯快照对比方案）======");

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

        log.info("====== 文件监控服务启动完成，共{}个监控任务 ======", snapshotMap.size());
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
        String excludePattern = StrUtil.isNotBlank(config.getExcludePattern())
            ? config.getExcludePattern()
            : watchProperties.getDefaultExcludePattern();

        // 1. 建立初始快照基线
        log.info("建立快照基线: {} (recursive={})", watchPath, recursive);
        FileSnapshot baseline = snapshotEngine.takeSnapshot(watchPath, recursive, excludePattern);
        snapshotMap.put(config.getConfigId(), baseline);
        log.info("快照基线建立完成: {} 个文件/目录", baseline.size());

        // 2. 创建单线程事件处理器（确保顺序处理）
        ExecutorService eventExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "file-event-handler-" + config.getConfigId());
            t.setDaemon(true);
            return t;
        });
        eventExecutors.put(config.getConfigId(), eventExecutor);

        // 3. 启动定时快照扫描
        int scanInterval = config.getScanInterval() != null ? config.getScanInterval() : watchProperties.getScanInterval();
        int initialDelay = 10; // 初始延迟10秒，让基线建立完成后立即开始第一次扫描

        if (scanScheduler != null && !scanScheduler.isShutdown()) {
            scanScheduler.scheduleAtFixedRate(() -> {
                try {
                    performSnapshotScan(config, recursive, excludePattern);
                } catch (Exception e) {
                    log.error("定时快照扫描失败: configId={}", config.getConfigId(), e);
                }
            }, initialDelay, scanInterval, TimeUnit.SECONDS);
        }

        log.info("监控启动成功（纯快照方案）: configId={}, path={}, scanInterval={}s, initialDelay={}s",
            config.getConfigId(), config.getWatchPath(), scanInterval, initialDelay);
    }

    /**
     * 执行快照扫描并处理变化
     * 核心逻辑：
     * 1. 建立新快照
     * 2. 与旧快照对比，生成变化事件列表
     * 3. 按顺序提交事件处理（确保顺序处理）
     */
    private void performSnapshotScan(KbFileWatchConfig config, boolean recursive, String excludePattern) throws IOException {
        Path watchPath = Path.of(config.getWatchPath());
        if (!Files.exists(watchPath)) {
            log.warn("监控路径不存在，跳过扫描: {}", watchPath);
            return;
        }

        // 获取当前快照
        FileSnapshot currentSnapshot = snapshotMap.get(config.getConfigId());
        if (currentSnapshot == null) {
            currentSnapshot = FileSnapshot.empty();
        }

        // 建立新快照
        FileSnapshot newSnapshot = snapshotEngine.takeSnapshot(watchPath, recursive, excludePattern);

        // 对比差异
        List<ChangeEvent> changes = snapshotEngine.diff(currentSnapshot, newSnapshot);

        // 更新快照
        snapshotMap.put(config.getConfigId(), newSnapshot);

        // 处理变化事件（按顺序提交到单线程执行器）
        if (!changes.isEmpty()) {
            log.info("快照扫描发现 {} 个变化: configId={}", changes.size(), config.getConfigId());

            ExecutorService executor = eventExecutors.get(config.getConfigId());
            if (executor != null && !executor.isShutdown()) {
                // 提交事件处理任务（确保顺序处理）
                executor.submit(() -> {
                    try {
                        processChangesSequentially(changes, config);
                    } catch (Exception e) {
                        log.error("事件处理任务执行失败: configId={}, changes={}", config.getConfigId(), changes.size(), e);
                    }
                });
            } else {
                log.warn("事件处理器不可用，无法处理变化: configId={}", config.getConfigId());
            }
        } else {
            log.debug("快照扫描未发现变化: configId={}", config.getConfigId());
        }
    }

    /**
     * 按顺序处理变化事件
     * 重要：所有事件必须按顺序处理，确保数据一致性
     */
    private void processChangesSequentially(List<ChangeEvent> changes, KbFileWatchConfig config) {
        log.info("开始顺序处理 {} 个变化事件: configId={}", changes.size(), config.getConfigId());

        for (ChangeEvent event : changes) {
            try {
                log.info("处理事件: type={}, path={}, isDir={}", event.type(), event.path(), event.isDirectory());

                // 记录变化日志
                KbFileChangeLog changeLog = buildChangeLog(event, config);
                int insertResult = changeLogMapper.insert(changeLog);
                log.info("变化日志插入结果: logId={}, result={}", changeLog.getLogId(), insertResult);

                // 处理变化
                FileChangeHandlerService.ChangeProcessResult result = changeHandlerService.handleChangeEvent(event, config);

                // 回填 docId 和 folderId
                if (result.getDocId() != null) {
                    changeLog.setDocId(result.getDocId());
                }
                if (result.getFolderId() != null) {
                    changeLog.setFolderId(result.getFolderId());
                }

                // 更新处理状态为成功
                String successMsg = result.getMessage() != null ? result.getMessage() : "处理成功";
                updateChangeLogStatus(changeLog, ChangeProcessStatusEnum.SUCCESS, successMsg);
                log.info("事件处理完成: path={}, docId={}, folderId={}", event.path(), changeLog.getDocId(), changeLog.getFolderId());

            } catch (Exception e) {
                log.error("处理事件失败: type={}, path={}", event.type(), event.path(), e);
                // 尝试记录失败日志
                try {
                    KbFileChangeLog failLog = buildChangeLog(event, config);
                    changeLogMapper.insert(failLog);
                    updateChangeLogStatus(failLog, ChangeProcessStatusEnum.FAILED, e.getMessage());
                } catch (Exception logError) {
                    log.error("记录失败日志也失败: path={}", event.path(), logError);
                }
            }
        }
        log.info("所有变化事件处理完成: configId={}", config.getConfigId());
    }

    /**
     * 构建变化记录日志
     */
    private KbFileChangeLog buildChangeLog(ChangeEvent event, KbFileWatchConfig config) {
        KbFileChangeLog changeLog = new KbFileChangeLog();
        changeLog.setConfigId(config.getConfigId());
        changeLog.setFilePath(event.path());
        changeLog.setFileName(Path.of(event.path()).getFileName().toString());
        changeLog.setIsDirectory(event.isDirectory() ? 1 : 0);
        changeLog.setChangeType(event.type().getCode());
        changeLog.setFolderId(config.getFolderId());
        changeLog.setProcessStatus(ChangeProcessStatusEnum.PENDING.getCode());
        changeLog.setTenantId(config.getTenantId());
        changeLog.setCreateTime(new Date());

        // 填充文件大小和hash信息
        changeLog.setFileSize(event.fileSize());
        changeLog.setContentHash(event.contentHash());
        changeLog.setOldHash(event.oldHash());

        // 重命名事件，记录旧路径
        if (event.type() == com.hmoob.doc.enums.FileChangeTypeEnum.RENAMED && event.oldPath() != null) {
            changeLog.setProcessMsg("旧路径: " + event.oldPath());
        }

        return changeLog;
    }

    /**
     * 更新变化记录处理状态
     */
    private void updateChangeLogStatus(KbFileChangeLog changeLog, ChangeProcessStatusEnum status, String msg) {
        changeLog.setProcessStatus(status.getCode());
        changeLog.setProcessMsg(StrUtil.sub(msg, 0, 500));
        changeLog.setProcessTime(new Date());
        // 更新 docId 和 folderId（如果已设置）
        changeLogMapper.updateById(changeLog);
    }

    /**
     * 停止单个监控
     */
    public void stopWatcher(Long configId) {
        // 停止事件处理器
        ExecutorService executor = eventExecutors.remove(configId);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        // 清除快照
        snapshotMap.remove(configId);
        log.info("已停止监控: configId={}", configId);
    }

    /**
     * 停止所有监控
     */
    public void stopAllWatchers() {
        log.info("停止所有文件监控...");

        // 停止所有事件处理器
        eventExecutors.forEach((id, executor) -> {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        });
        eventExecutors.clear();

        // 清除快照
        snapshotMap.clear();

        // 停止扫描调度器
        if (scanScheduler != null) {
            scanScheduler.shutdown();
            try {
                if (!scanScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scanScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scanScheduler.shutdownNow();
            }
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

        // 按顺序处理变化
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
        status.put("activeWatchers", snapshotMap.size());
        status.put("watcherDetails", snapshotMap.entrySet().stream()
            .map(e -> Map.of("configId", e.getKey(), "snapshotSize", e.getValue().size()))
            .toList());
        return status;
    }
}