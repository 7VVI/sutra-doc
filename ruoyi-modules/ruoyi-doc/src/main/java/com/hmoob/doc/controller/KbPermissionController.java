package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.core.validate.AddGroup;
import com.hmoob.common.idempotent.annotation.RepeatSubmit;
import com.hmoob.common.log.annotation.Log;
import com.hmoob.common.log.enums.BusinessType;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.bo.KbDocPermissionBo;
import com.hmoob.doc.domain.bo.KbFolderPermissionBo;
import com.hmoob.doc.domain.dto.KbPermissionResult;
import com.hmoob.doc.domain.vo.KbFolderPermissionVo;
import com.hmoob.doc.service.IKbPermissionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KB权限管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/permission")
public class KbPermissionController extends BaseController {

    private final IKbPermissionService permissionService;

    /**
     * 获取目录权限列表
     *
     * @param folderId 目录ID
     */
    @SaCheckPermission("kb:permission:query")
    @GetMapping("/folder/{folderId}")
    public R<List<KbFolderPermissionVo>> getFolderPermissions(@NotNull(message = "目录ID不能为空") @PathVariable Long folderId) {
        return R.ok(permissionService.selectFolderPermissions(folderId));
    }

    /**
     * 获取继承权限列表
     *
     * @param folderId 目录ID
     */
    @SaCheckPermission("kb:permission:query")
    @GetMapping("/folder/inherited/{folderId}")
    public R<List<KbFolderPermissionVo>> getInheritedPermissions(@PathVariable Long folderId) {
        return R.ok(permissionService.getInheritedPermissions(folderId));
    }

    /**
     * 计算目录权限
     *
     * @param folderId 目录ID
     */
    @SaCheckPermission("kb:permission:query")
    @GetMapping("/folder/calculate/{folderId}")
    public R<KbPermissionResult> calculateFolderPermission(@PathVariable Long folderId) {
        return R.ok(permissionService.calculateFolderPermission(folderId, null));
    }

    /**
     * 计算文档权限
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:permission:query")
    @GetMapping("/doc/calculate/{docId}")
    public R<KbPermissionResult> calculateDocPermission(@PathVariable Long docId) {
        return R.ok(permissionService.calculateDocPermission(docId, null));
    }

    /**
     * 授予目录权限
     */
    @SaCheckPermission("kb:permission:grant")
    @Log(title = "权限管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/grant/folder")
    public R<Void> grantFolderPermission(@Validated(AddGroup.class) @RequestBody KbFolderPermissionBo bo) {
        return toAjax(permissionService.grantFolderPermission(bo));
    }

    /**
     * 授予文档权限
     */
    @SaCheckPermission("kb:permission:grant")
    @Log(title = "权限管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/grant/doc")
    public R<Void> grantDocPermission(@Validated(AddGroup.class) @RequestBody KbDocPermissionBo bo) {
        return toAjax(permissionService.grantDocPermission(bo));
    }

    /**
     * 删除目录权限
     *
     * @param permissionId 权限ID
     */
    @SaCheckPermission("kb:permission:remove")
    @Log(title = "权限管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/folder/{permissionId}")
    public R<Void> removeFolderPermission(@PathVariable Long permissionId) {
        return toAjax(permissionService.deleteFolderPermission(permissionId));
    }

    /**
     * 删除文档权限
     *
     * @param permissionId 权限ID
     */
    @SaCheckPermission("kb:permission:remove")
    @Log(title = "权限管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/doc/{permissionId}")
    public R<Void> removeDocPermission(@PathVariable Long permissionId) {
        return toAjax(permissionService.deleteDocPermission(permissionId));
    }

}