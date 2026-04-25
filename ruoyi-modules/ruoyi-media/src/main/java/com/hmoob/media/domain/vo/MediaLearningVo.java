package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 学习记录视图对象
 *
 * @author hmoob
 */
@Data
public class MediaLearningVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 视频标题
     */
    private String videoTitle;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 观看时长(秒)
     */
    private Integer watchDuration;

    /**
     * 学习进度(0-100)
     */
    private BigDecimal progress;

    /**
     * 最后播放位置(秒)
     */
    private Integer lastPosition;

    /**
     * 是否已完成
     */
    private Boolean completed;

    /**
     * 更新时间
     */
    private Date updateTime;

}
