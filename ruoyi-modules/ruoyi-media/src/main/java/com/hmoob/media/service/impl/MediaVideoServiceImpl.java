package com.hmoob.media.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.oss.core.OssClient;
import com.hmoob.common.oss.entity.UploadResult;
import com.hmoob.common.oss.factory.OssFactory;
import com.hmoob.common.satoken.utils.LoginHelper;
import com.hmoob.media.domain.MediaVideo;
import com.hmoob.media.domain.MediaVideoAction;
import com.hmoob.media.domain.MediaVideoAttachment;
import com.hmoob.media.domain.MediaVideoTag;
import com.hmoob.media.domain.bo.MediaVideoQuery;
import com.hmoob.media.domain.bo.MediaVideoUploadBo;
import com.hmoob.media.domain.vo.MediaVideoAttachmentVo;
import com.hmoob.media.domain.vo.MediaVideoDetailVo;
import com.hmoob.media.domain.vo.MediaVideoVo;
import com.hmoob.media.mapper.MediaVideoActionMapper;
import com.hmoob.media.mapper.MediaVideoAttachmentMapper;
import com.hmoob.media.mapper.MediaVideoMapper;
import com.hmoob.media.mapper.MediaVideoTagMapper;
import com.hmoob.media.service.IMediaVideoAuthService;
import com.hmoob.media.service.IMediaVideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 视频管理服务实现
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MediaVideoServiceImpl implements IMediaVideoService {

    /**
     * 互动类型: 点赞
     */
    private static final int ACTION_TYPE_LIKE = 1;

    /**
     * 互动类型: 播放
     */
    private static final int ACTION_TYPE_PLAY = 3;

    private static final ConcurrentMap<Long, Object> USER_LOCK_MAP = new ConcurrentHashMap<>();

    private final MediaVideoMapper videoMapper;
    private final MediaVideoTagMapper videoTagMapper;
    private final MediaVideoActionMapper actionMapper;
    private final MediaVideoAttachmentMapper attachmentMapper;
    private final IMediaVideoAuthService authService;
    private final TransactionTemplate transactionTemplate;

    // ==================== 上传 ====================

    /**
     * 上传视频
     *
     * @param file       视频文件
     * @param thumbnail  缩略图文件（可选）
     * @param bo         上传参数
     * @return 视频ID
     */
    @Override
    public Long uploadVideo(MultipartFile file, MultipartFile thumbnail, MediaVideoUploadBo bo) {
        try {
            OssClient ossClient = OssFactory.instance();
            UploadResult videoResult = ossClient.uploadSuffix(file.getBytes(), file.getOriginalFilename(), file.getContentType());
            String videoPath = videoResult.getUrl();

            String thumbnailPath = null;
            if (thumbnail != null && !thumbnail.isEmpty()) {
                UploadResult thumbResult = ossClient.uploadSuffix(thumbnail.getBytes(), thumbnail.getOriginalFilename(), thumbnail.getContentType());
                thumbnailPath = thumbResult.getUrl();
            }

            MediaVideo video = new MediaVideo();
            video.setTitle(bo.getTitle());
            video.setDescription(bo.getDescription());
            video.setAuthorId(bo.getAuthorId());
            video.setVideoPath(videoPath);
            video.setThumbnail(thumbnailPath);
            video.setFileSize(file.getSize());
            video.setFileType(getFileExtension(file.getOriginalFilename()));
            video.setViewCount(0L);
            video.setLikeCount(0L);
            video.setAuthType(bo.getAuthType() != null ? bo.getAuthType() : 0);
            video.setStatus(1);

            transactionTemplate.executeWithoutResult(status -> {
                videoMapper.insert(video);
                Long videoId = video.getVideoId();
                if (CollUtil.isNotEmpty(bo.getCategoryIds())) {
                    for (Long categoryId : bo.getCategoryIds()) {
                        MediaVideoTag vt = new MediaVideoTag();
                        vt.setVideoId(videoId);
                        vt.setTagId(categoryId);
                        videoTagMapper.insert(vt);
                    }
                }
                if (CollUtil.isNotEmpty(bo.getDirectionIds())) {
                    for (Long directionId : bo.getDirectionIds()) {
                        MediaVideoTag vt = new MediaVideoTag();
                        vt.setVideoId(videoId);
                        vt.setTagId(directionId);
                        videoTagMapper.insert(vt);
                    }
                }
            });
            return video.getVideoId();
        } catch (IOException e) {
            log.error("视频上传失败", e);
            throw new ServiceException("视频上传失败：" + e.getMessage());
        }
    }

    // ==================== 分页查询 ====================

    /**
     * 分页查询视频列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public TableDataInfo<MediaVideoVo> selectVideoPage(MediaVideoQuery query) {
        Long userId = LoginHelper.getUserId();
        Long deptId = LoginHelper.getDeptId();
        List<Long> authorizedIds = authService.getAuthorizedVideoIds(userId, deptId);

        LambdaQueryWrapper<MediaVideo> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.and(w -> w.like(MediaVideo::getTitle, query.getKeyword()));
        }
        if ("hottest".equals(query.getSort())) {
            wrapper.orderByDesc(MediaVideo::getLikeCount);
        } else {
            wrapper.orderByDesc(MediaVideo::getCreateTime);
        }
        wrapper.and(w -> {
            w.eq(MediaVideo::getAuthType, 0);
            if (CollUtil.isNotEmpty(authorizedIds)) {
                w.or().in(MediaVideo::getVideoId, authorizedIds);
            }
        });

        Page<MediaVideoVo> page = videoMapper.selectVoPage(query.build(), wrapper);
        return TableDataInfo.build(page);
    }

    // ==================== 详情 ====================

    /**
     * 查询视频详情
     *
     * @param videoId 视频ID
     * @return 视频详情
     */
    @Override
    public MediaVideoDetailVo selectVideoDetail(Long videoId) {
        MediaVideo video = videoMapper.selectById(videoId);
        if (video == null) {
            return null;
        }
        MediaVideoDetailVo detail = new MediaVideoDetailVo();
        detail.setVideoId(video.getVideoId());
        detail.setTitle(video.getTitle());
        detail.setDescription(video.getDescription());
        detail.setAuthorId(video.getAuthorId());
        detail.setFileType(video.getFileType());
        detail.setFileSize(video.getFileSize());
        detail.setDuration(video.getDuration());
        detail.setViewCount(video.getViewCount());
        detail.setLikeCount(video.getLikeCount());
        detail.setAuthType(video.getAuthType());
        detail.setCreateTime(video.getCreateTime());
        detail.setUpdateTime(video.getUpdateTime());
        detail.setIsAuthor(Objects.equals(video.getAuthorId(), LoginHelper.getUserId()));

        // 构建缩略图URL
        try {
            detail.setThumbnailUrl(OssFactory.instance().getDomain() + "/" + video.getThumbnail());
        } catch (Exception e) {
            detail.setThumbnailUrl(video.getThumbnail());
        }

        // 查询当前用户是否已点赞
        Long currentUserId = LoginHelper.getUserId();
        Long likeCount = actionMapper.selectCount(new LambdaQueryWrapper<MediaVideoAction>()
            .eq(MediaVideoAction::getVideoId, videoId)
            .eq(MediaVideoAction::getUserId, currentUserId)
            .eq(MediaVideoAction::getActionType, ACTION_TYPE_LIKE));
        detail.setHasLiked(likeCount > 0);

        // 查询附件列表
        List<MediaVideoAttachment> attachments = attachmentMapper.selectList(
            new LambdaQueryWrapper<MediaVideoAttachment>().eq(MediaVideoAttachment::getVideoId, videoId));
        detail.setAttachments(MapstructUtils.convert(attachments, MediaVideoAttachmentVo.class));
        return detail;
    }

    // ==================== 删除 ====================

    /**
     * 删除视频
     *
     * @param videoId 视频ID
     * @return 影响行数
     */
    @Override
    public int deleteVideo(Long videoId) {
        return transactionTemplate.execute(status -> {
            // 删除附件
            attachmentMapper.selectList(new LambdaQueryWrapper<MediaVideoAttachment>()
                .eq(MediaVideoAttachment::getVideoId, videoId))
                .forEach(a -> attachmentMapper.deleteById(a.getAttachmentId()));
            // 删除标签关联
            videoTagMapper.delete(new LambdaQueryWrapper<MediaVideoTag>()
                .eq(MediaVideoTag::getVideoId, videoId));
            // 删除互动记录
            actionMapper.delete(new LambdaQueryWrapper<MediaVideoAction>()
                .eq(MediaVideoAction::getVideoId, videoId));
            // 删除视频
            return videoMapper.deleteById(videoId);
        });
    }

    // ==================== Range 播放 ====================

    /**
     * Range 流式播放视频
     *
     * @param videoId 视频ID
     * @param request  HTTP请求
     * @param response HTTP响应
     */
    @Override
    public void playVideo(Long videoId, HttpServletRequest request, HttpServletResponse response) {
        Long userId = LoginHelper.getUserId();
        Long deptId = LoginHelper.getDeptId();

        MediaVideo video = videoMapper.selectById(videoId);
        if (video == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!isVideoVisible(video, userId, deptId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            OssClient ossClient = OssFactory.instance();
            InputStream inputStream = ossClient.getObjectContent(video.getVideoPath());
            byte[] allBytes = inputStream.readAllBytes();
            long fileLength = allBytes.length;

            long startRange = 0;
            long endRange = fileLength - 1;
            String rangeHeader = request.getHeader("Range");

            if (StrUtil.isNotBlank(rangeHeader)) {
                try {
                    String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                    startRange = Long.parseLong(ranges[0]);
                    if (ranges.length > 1 && StrUtil.isNotBlank(ranges[1])) {
                        endRange = Long.parseLong(ranges[1]);
                    }
                } catch (NumberFormatException e) {
                    startRange = 0;
                    endRange = fileLength - 1;
                }
            }

            if (startRange < 0 || startRange >= fileLength || endRange >= fileLength) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }

            long contentLength = endRange - startRange + 1;
            response.reset();
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setContentType("video/" + getFileExtension(video.getVideoPath()));
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Length", String.valueOf(contentLength));
            response.setHeader("Content-Range", String.format("bytes %d-%d/%d", startRange, endRange, fileLength));
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

            try (OutputStream os = response.getOutputStream()) {
                os.write(allBytes, (int) startRange, (int) contentLength);
                os.flush();
            }
            recordWatch(videoId, userId);
        } catch (Exception e) {
            log.error("视频播放失败: videoId={}", videoId, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * 判断视频对用户是否可见
     *
     * @param video  视频实体
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 是否可见
     */
    private boolean isVideoVisible(MediaVideo video, Long userId, Long deptId) {
        if (video.getAuthType() == null || video.getAuthType() == 0) {
            return true;
        }
        List<Long> authorizedIds = authService.getAuthorizedVideoIds(userId, deptId);
        return authorizedIds.contains(video.getVideoId());
    }

    /**
     * 记录观看行为（同一用户对同一视频只记录一次）
     *
     * @param videoId 视频ID
     * @param userId  用户ID
     */
    private void recordWatch(Long videoId, Long userId) {
        synchronized (USER_LOCK_MAP.computeIfAbsent(userId, k -> new Object())) {
            transactionTemplate.executeWithoutResult(status -> {
                Long count = actionMapper.selectCount(new LambdaQueryWrapper<MediaVideoAction>()
                    .eq(MediaVideoAction::getVideoId, videoId)
                    .eq(MediaVideoAction::getUserId, userId)
                    .eq(MediaVideoAction::getActionType, ACTION_TYPE_PLAY));
                if (count > 0) {
                    return;
                }
                videoMapper.update(null, new LambdaQueryWrapper<MediaVideo>()
                    .eq(MediaVideo::getVideoId, videoId)
                    .setSql("view_count = view_count + 1"));
                MediaVideoAction action = new MediaVideoAction();
                action.setVideoId(videoId);
                action.setUserId(userId);
                action.setActionType(ACTION_TYPE_PLAY);
                action.setCreateTime(new Date());
                actionMapper.insert(action);
            });
        }
    }

    // ==================== 点赞/取消点赞 ====================

    /**
     * 点赞视频
     *
     * @param videoId 视频ID
     * @return 是否成功
     */
    @Override
    public boolean likeVideo(Long videoId) {
        Long userId = LoginHelper.getUserId();
        synchronized (USER_LOCK_MAP.computeIfAbsent(userId, k -> new Object())) {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                Long count = actionMapper.selectCount(new LambdaQueryWrapper<MediaVideoAction>()
                    .eq(MediaVideoAction::getVideoId, videoId)
                    .eq(MediaVideoAction::getUserId, userId)
                    .eq(MediaVideoAction::getActionType, ACTION_TYPE_LIKE));
                if (count > 0) {
                    throw new ServiceException("已点赞");
                }
                videoMapper.update(null, new LambdaQueryWrapper<MediaVideo>()
                    .eq(MediaVideo::getVideoId, videoId)
                    .setSql("like_count = like_count + 1"));
                MediaVideoAction action = new MediaVideoAction();
                action.setVideoId(videoId);
                action.setUserId(userId);
                action.setActionType(ACTION_TYPE_LIKE);
                action.setCreateTime(new Date());
                actionMapper.insert(action);
                return true;
            }));
        }
    }

    /**
     * 取消点赞
     *
     * @param videoId 视频ID
     * @return 是否成功
     */
    @Override
    public boolean unlikeVideo(Long videoId) {
        Long userId = LoginHelper.getUserId();
        synchronized (USER_LOCK_MAP.computeIfAbsent(userId, k -> new Object())) {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                MediaVideoAction action = actionMapper.selectOne(new LambdaQueryWrapper<MediaVideoAction>()
                    .eq(MediaVideoAction::getVideoId, videoId)
                    .eq(MediaVideoAction::getUserId, userId)
                    .eq(MediaVideoAction::getActionType, ACTION_TYPE_LIKE));
                if (action == null) {
                    throw new ServiceException("未点赞");
                }
                videoMapper.update(null, new LambdaQueryWrapper<MediaVideo>()
                    .eq(MediaVideo::getVideoId, videoId)
                    .setSql("like_count = like_count - 1"));
                actionMapper.deleteById(action.getId());
                return true;
            }));
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名（小写）
     */
    private String getFileExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        int idx = fileName.lastIndexOf(".");
        if (idx == -1 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase();
    }
}
