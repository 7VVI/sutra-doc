package com.hmoob.media.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.oss.core.OssClient;
import com.hmoob.common.oss.entity.UploadResult;
import com.hmoob.common.oss.factory.OssFactory;
import com.hmoob.media.domain.MediaVideoAttachment;
import com.hmoob.media.domain.vo.MediaVideoAttachmentVo;
import com.hmoob.media.mapper.MediaVideoAttachmentMapper;
import com.hmoob.media.service.IMediaVideoAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 视频附件服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class MediaVideoAttachmentServiceImpl implements IMediaVideoAttachmentService {

    private final MediaVideoAttachmentMapper baseMapper;

    /**
     * 上传附件
     *
     * @param videoId 视频ID
     * @param file    附件文件
     * @return 附件ID
     */
    @Override
    public Long uploadAttachment(Long videoId, MultipartFile file) {
        try {
            OssClient ossClient = OssFactory.instance();
            UploadResult result = ossClient.uploadSuffix(file.getBytes(), file.getOriginalFilename(), file.getContentType());
            MediaVideoAttachment attachment = new MediaVideoAttachment();
            attachment.setVideoId(videoId);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setFilePath(result.getUrl());
            attachment.setFileSize(file.getSize());
            attachment.setFileFormat(getFileExtension(file.getOriginalFilename()));
            baseMapper.insert(attachment);
            return attachment.getAttachmentId();
        } catch (IOException e) {
            throw new ServiceException("附件上传失败：" + e.getMessage());
        }
    }

    /**
     * 查询视频的附件列表
     *
     * @param videoId 视频ID
     * @return 附件列表
     */
    @Override
    public List<MediaVideoAttachmentVo> selectAttachments(Long videoId) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<MediaVideoAttachment>()
            .eq(MediaVideoAttachment::getVideoId, videoId));
    }

    /**
     * 下载附件
     *
     * @param attachmentId 附件ID
     * @return 附件输入流
     */
    @Override
    public InputStream downloadAttachment(Long attachmentId) {
        MediaVideoAttachment attachment = baseMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new ServiceException("附件不存在");
        }
        try {
            return OssFactory.instance().getObjectContent(attachment.getFilePath());
        } catch (IOException e) {
            throw new ServiceException("附件下载失败");
        }
    }

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
