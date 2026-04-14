package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.log.annotation.Log;
import com.hmoob.common.log.enums.BusinessType;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.vo.KbDocVersionVo;
import com.hmoob.doc.service.IKbDocVersionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KB文档版本管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/version")
public class KbDocVersionController extends BaseController {

    private final IKbDocVersionService versionService;

    /**
     * 获取文档版本列表
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:version:list")
    @GetMapping("/list/{docId}")
    public R<List<KbDocVersionVo>> list(@NotNull(message = "文档ID不能为空") @PathVariable Long docId) {
        return R.ok(versionService.selectVersionList(docId));
    }

    /**
     * 获取版本详细信息
     *
     * @param versionId 版本ID
     */
    @SaCheckPermission("kb:version:query")
    @GetMapping("/{versionId}")
    public R<KbDocVersionVo> getInfo(@NotNull(message = "版本ID不能为空") @PathVariable Long versionId) {
        return R.ok(versionService.selectVersionById(versionId));
    }

    /**
     * 获取文档指定版本
     *
     * @param docId     文档ID
     * @param versionNo 版本号
     */
    @SaCheckPermission("kb:version:query")
    @GetMapping("/{docId}/{versionNo}")
    public R<KbDocVersionVo> getVersionByVersionNo(
        @PathVariable Long docId,
        @PathVariable Integer versionNo) {
        return R.ok(versionService.selectVersionByVersionNo(docId, versionNo));
    }

    /**
     * 获取文档最新版本号
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:version:query")
    @GetMapping("/latest/{docId}")
    public R<Integer> getLatestVersionNo(@PathVariable Long docId) {
        return R.ok(versionService.getLatestVersionNo(docId));
    }

    /**
     * 回滚到指定版本
     *
     * @param docId     文档ID
     * @param versionNo 目标版本号
     */
    @SaCheckPermission("kb:version:rollback")
    @Log(title = "版本管理", businessType = BusinessType.UPDATE)
    @PostMapping("/rollback/{docId}/{versionNo}")
    public R<Void> rollback(
        @PathVariable Long docId,
        @PathVariable Integer versionNo) {
        return toAjax(versionService.rollbackToVersion(docId, versionNo));
    }

    /**
     * 删除版本
     *
     * @param versionId 版本ID
     */
    @SaCheckPermission("kb:version:remove")
    @Log(title = "版本管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{versionId}")
    public R<Void> remove(@PathVariable Long versionId) {
        return toAjax(versionService.deleteVersion(versionId));
    }

}