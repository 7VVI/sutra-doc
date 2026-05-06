package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.hmoob.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 视频信息实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_video")
public class MediaVideo extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    @TableId(value = "video_id")
    private Long videoId;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频描述
     */
    private String description;

    /**
     * 作者名称
     */
    private String authorName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 视频时长(秒)
     */
    private Integer duration;

    /**
     * 视频存储路径
     */
    private String videoPath;

    /**
     * 缩略图路径
     */
    private String thumbnail;

    /**
     * 播放次数
     */
    private Long viewCount;

    /**
     * 点赞次数
     */
    private Long likeCount;

    /**
     * 授权类型: 0-公开, 1-指定用户/角色
     */
    private Integer authType;

    /**
     * 状态: 0-待审核, 1-已发布, 2-已下架
     */
    private Integer status;

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
