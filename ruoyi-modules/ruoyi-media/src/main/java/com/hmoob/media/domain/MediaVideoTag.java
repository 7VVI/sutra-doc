package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频-标签关联实体类
 *
 * @author hmoob
 */
@Data
@TableName("media_video_tag")
public class MediaVideoTag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 标签ID
     */
    private Long tagId;

}
