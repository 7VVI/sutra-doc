package com.hmoob.media.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.media.domain.MediaVideoAttachment;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频附件视图对象 media_video_attachment
 *
 * @author hmoob
 */
@Data
@AutoMapper(target = MediaVideoAttachment.class)
public class MediaVideoAttachmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 附件ID
     */
    private Long attachmentId;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件格式
     */
    private String fileFormat;

    /**
     * 创建时间
     */
    private Date createTime;

}
