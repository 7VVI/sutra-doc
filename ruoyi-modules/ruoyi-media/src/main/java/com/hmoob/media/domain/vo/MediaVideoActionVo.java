package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频互动记录视图对象
 *
 * @author hmoob
 */
@Data
public class MediaVideoActionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 互动类型: 1-点赞, 2-收藏, 3-播放
     */
    private Integer actionType;

    /**
     * 扩展信息(JSON格式)
     */
    private String extra;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
