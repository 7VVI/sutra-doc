package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.KbFileWatchConfig;
import com.hmoob.doc.domain.vo.KbFileChangeLogVo;
import com.hmoob.doc.domain.vo.KbFileWatchConfigVo;
import com.hmoob.doc.service.impl.KbFileWatchService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * KB文件监控管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/file-watch")
public class KbFileWatchController extends BaseController {

    private final KbFileWatchService fileWatchService;

    // ==================== 监控配置管理 ====================

    /**
     * 查询监控配置列表
     */
    @SaCheckPermission("kb:fileWatch:list")
    @GetMapping("/config/list")
    public R<List<KbFileWatchConfigVo>> listConfigs() {
        return R.ok(fileWatchService.listConfigs());
    }

    /**
     * 获取单个监控配置详情
     */
    @SaCheckPermission("kb:fileWatch:query")
    @GetMapping("/config/{configId}")
    public R<KbFileWatchConfigVo> getConfig(@PathVariable Long configId) {
        return R.ok(fileWatchService.getConfig(configId));
    }

    /**
     * 新增监控配置
     */
    @SaCheckPermission("kb:fileWatch:add")
    @PostMapping("/config")
    public R<Void> addConfig(@RequestBody KbFileWatchConfig config) {
        fileWatchService.addConfig(config);
        return R.ok();
    }

    /**
     * 修改监控配置
     */
    @SaCheckPermission("kb:fileWatch:edit")
    @PutMapping("/config")
    public R<Void> updateConfig(@RequestBody KbFileWatchConfig config) {
        fileWatchService.updateConfig(config);
        return R.ok();
    }

    /**
     * 删除监控配置
     */
    @SaCheckPermission("kb:fileWatch:remove")
    @DeleteMapping("/config/{configId}")
    public R<Void> deleteConfig(@PathVariable Long configId) {
        fileWatchService.deleteConfig(configId);
        return R.ok();
    }

    // ==================== 监控控制 ====================

    /**
     * 启动所有监控
     */
    @SaCheckPermission("kb:fileWatch:control")
    @PostMapping("/start-all")
    public R<Void> startAll() {
        fileWatchService.startAllWatchers();
        return R.ok();
    }

    /**
     * 停止所有监控
     */
    @SaCheckPermission("kb:fileWatch:control")
    @PostMapping("/stop-all")
    public R<Void> stopAll() {
        fileWatchService.stopAllWatchers();
        return R.ok();
    }

    /**
     * 启用/禁用单个监控
     */
    @SaCheckPermission("kb:fileWatch:control")
    @PostMapping("/toggle/{configId}")
    public R<Void> toggle(@PathVariable Long configId, @RequestParam boolean enable) throws IOException {
        fileWatchService.toggleConfig(configId, enable);
        return R.ok();
    }

    /**
     * 手动触发全量扫描
     */
    @SaCheckPermission("kb:fileWatch:control")
    @PostMapping("/scan/{configId}")
    public R<Integer> triggerScan(@PathVariable Long configId) throws IOException {
        int changes = fileWatchService.triggerFullScan(configId);
        return R.ok(changes);
    }

    /**
     * 获取监控状态
     */
    @SaCheckPermission("kb:fileWatch:query")
    @GetMapping("/status")
    public R<Map<String, Object>> getStatus() {
        return R.ok(fileWatchService.getWatchStatus());
    }

    // ==================== 变化记录 ====================

    /**
     * 分页查询文件变化记录
     */
    @SaCheckPermission("kb:fileWatch:list")
    @GetMapping("/change-log")
    public TableDataInfo<KbFileChangeLogVo> pageChangeLogs(
        @RequestParam(required = false) Long configId,
        @RequestParam(required = false) String changeType,
        @RequestParam(required = false) Integer processStatus,
        PageQuery pageQuery) {
        return fileWatchService.pageChangeLogs(configId, changeType, processStatus, pageQuery);
    }
}
