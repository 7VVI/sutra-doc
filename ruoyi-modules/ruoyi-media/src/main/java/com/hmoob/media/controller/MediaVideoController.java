package com.hmoob.media.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.media.domain.bo.*;
import com.hmoob.media.domain.vo.*;
import com.hmoob.media.service.IMediaVideoAttachmentService;
import com.hmoob.media.service.IMediaVideoService;
import com.hmoob.media.service.IMediaVideoShareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 视频管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/media/video")
public class MediaVideoController extends BaseController {

    private final IMediaVideoService videoService;
    private final IMediaVideoAttachmentService attachmentService;
    private final IMediaVideoShareService shareService;

    /** 上传视频 */
    @SaCheckPermission("media:video:add")
    @PostMapping("/upload")
    public R<Long> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
        MediaVideoUploadBo bo) {
        return R.ok(videoService.uploadVideo(file, thumbnail, bo));
    }

    /** 分页查询视频列表 */
    @GetMapping("/page")
    public TableDataInfo<MediaVideoVo> page(MediaVideoQuery query) {
        return videoService.selectVideoPage(query);
    }

    /** 查询视频详情 */
    @GetMapping("/{videoId}")
    public R<MediaVideoDetailVo> detail(@PathVariable Long videoId) {
        return R.ok(videoService.selectVideoDetail(videoId));
    }

    /** 删除视频 */
    @SaCheckPermission("media:video:remove")
    @DeleteMapping("/{videoId}")
    public R<Void> remove(@PathVariable Long videoId) {
        return toAjax(videoService.deleteVideo(videoId));
    }

    /** Range 流式播放 */
    @GetMapping("/play/{videoId}")
    public void play(@PathVariable Long videoId, HttpServletRequest request, HttpServletResponse response) {
        videoService.playVideo(videoId, request, response);
    }

    /** 点赞 */
    @PostMapping("/like")
    public R<Boolean> like(@Validated @RequestBody MediaVideoLikeBo bo) {
        return R.ok(videoService.likeVideo(bo.getVideoId()));
    }

    /** 取消点赞 */
    @PostMapping("/unlike")
    public R<Boolean> unlike(@Validated @RequestBody MediaVideoLikeBo bo) {
        return R.ok(videoService.unlikeVideo(bo.getVideoId()));
    }

    /** 创建分享链接 */
    @PostMapping("/share")
    public R<MediaVideoShareVo> share(@Validated @RequestBody MediaVideoShareBo bo) {
        return R.ok(shareService.createShare(bo));
    }

    /** 通过分享令牌访问 */
    @GetMapping("/share/{shareToken}")
    public R<MediaVideoShareVo> accessShare(@PathVariable String shareToken) {
        return R.ok(shareService.accessByToken(shareToken));
    }

    /** 查询附件列表 */
    @GetMapping("/{videoId}/attachments")
    public R<List<MediaVideoAttachmentVo>> attachments(@PathVariable Long videoId) {
        return R.ok(attachmentService.selectAttachments(videoId));
    }

    /** 上传附件 */
    @SaCheckPermission("media:video:add")
    @PostMapping("/attachment")
    public R<Long> uploadAttachment(
        @RequestParam("file") MultipartFile file,
        @RequestParam("videoId") Long videoId) {
        return R.ok(attachmentService.uploadAttachment(videoId, file));
    }

    /** 下载附件 */
    @GetMapping("/attachment/{attachmentId}/download")
    public void downloadAttachment(@PathVariable Long attachmentId, HttpServletResponse response) {
        try (InputStream is = attachmentService.downloadAttachment(attachmentId);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
