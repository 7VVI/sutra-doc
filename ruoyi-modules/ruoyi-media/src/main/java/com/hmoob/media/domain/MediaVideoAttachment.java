package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.hmoob.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 视频附件实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_video_attachment")
public class MediaVideoAttachment extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 附件ID
     */
    @TableId(value = "attachment_id")
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
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件格式
     */
    private String fileFormat;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

    /**
     * 租户编号
     */
    @TableField(exist = false)
    private String tenantId;

    /**
     * 创建部门
     */
    @TableField(fill = FieldFill.INSERT,exist = false)
    private Long createDept;
}
