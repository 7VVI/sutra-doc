package com.hmoob.media.service;

import com.hmoob.media.domain.vo.MediaVideoAttachmentVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 视频附件服务接口
 *
 * @author hmoob
 */
public interface IMediaVideoAttachmentService {

    /**
     * 上传附件
     *
     * @param videoId 视频ID
     * @param file    附件文件
     * @return 附件ID
     */
    Long uploadAttachment(Long videoId, MultipartFile file);

    /**
     * 查询视频的附件列表
     *
     * @param videoId 视频ID
     * @return 附件列表
     */
    List<MediaVideoAttachmentVo> selectAttachments(Long videoId);

    /**
     * 下载附件
     *
     * @param attachmentId 附件ID
     * @return 附件输入流
     */
    InputStream downloadAttachment(Long attachmentId);
}
