package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.log.annotation.Log;
import com.hmoob.common.log.enums.BusinessType;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.vo.KbFileVo;
import com.hmoob.doc.service.IKbFileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * KB文件管理
 *
 * @author hmoob
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/file")
public class KbFileController extends BaseController {

    private final IKbFileService fileService;

    /**
     * 获取文件详细信息
     */
    @SaCheckPermission("kb:file:query")
    @GetMapping("/{fileId}")
    public R<KbFileVo> getInfo(@NotNull(message = "文件ID不能为空") @PathVariable Long fileId) {
        return R.ok(fileService.selectFileById(fileId));
    }

    /**
     * 根据SHA256查询文件
     */
    @SaCheckPermission("kb:file:query")
    @GetMapping("/sha256/{sha256}")
    public R<KbFileVo> getBySha256(@PathVariable String sha256) {
        return R.ok(fileService.selectBySha256(sha256));
    }

    /**
     * 上传文件
     */
    @SaCheckPermission("kb:file:upload")
    @Log(title = "文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public R<KbFileVo> upload(@RequestParam("file") MultipartFile file) {
        KbFileVo fileVo = fileService.uploadFile(file);
        return R.ok(fileVo);
    }

    /**
     * 上传文件并校验SHA256
     */
    @SaCheckPermission("kb:file:upload")
    @Log(title = "文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/uploadWithCheck")
    public R<KbFileVo> uploadWithCheck(
        @RequestParam("file") MultipartFile file,
        @RequestParam("sha256") String expectedSha256) {
        KbFileVo fileVo = fileService.uploadFile(file);
        if (fileVo != null && fileVo.getSha256() != null) {
            if (!fileVo.getSha256().equals(expectedSha256)) {
                return R.fail("文件SHA256校验失败,上传文件与预期不一致");
            }
        }
        return R.ok(fileVo);
    }

    /**
     * 删除文件
     */
    @SaCheckPermission("kb:file:remove")
    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{fileId}")
    public R<Void> remove(@PathVariable Long fileId) {
        return toAjax(fileService.deleteFileById(fileId));
    }

    // ========== 文件内容访问（支持本地存储） ==========

    /**
     * 获取文件内容
     * 本地存储: 从磁盘读取并流式输出（浏览器可预览PDF/图片）
     * OSS存储: 重定向到OSS的URL
     */
    @SaIgnore
    @GetMapping("/content/{fileId}")
    public void getFileContent(@PathVariable Long fileId, HttpServletResponse response) {
        fileService.serveFile(fileId, response, false);
    }

    /**
     * 下载文件（强制下载而非浏览器内预览）
     */
    @SaIgnore
    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable Long fileId, HttpServletResponse response) {
        fileService.serveFile(fileId, response, true);
    }

}
