package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频列表视图对象
 *
 * @author hmoob
 */
@Data
public class MediaVideoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
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
     * 作者ID
     */
    private Long authorId;

    /**
     * 作者名称
     */
    private String authorName;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

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
     * 授权部门名称(逗号分隔)
     */
    private String authDeptNames;

    /**
     * 当前用户是否已点赞
     */
    private Boolean hasLiked;

    /**
     * 附件数量
     */
    private Long attachmentCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
