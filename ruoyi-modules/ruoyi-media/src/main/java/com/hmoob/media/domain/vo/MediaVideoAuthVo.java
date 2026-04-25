package com.hmoob.media.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.media.domain.MediaVideoAuth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频授权视图对象 media_video_auth
 *
 * @author hmoob
 */
@Data
@AutoMapper(target = MediaVideoAuth.class)
public class MediaVideoAuthVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 授权ID
     */
    private Long id;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 授权对象类型: 1-用户, 2-角色
     */
    private Integer authTargetType;

    /**
     * 授权对象ID
     */
    private Long authTargetId;

    /**
     * 授权对象名称
     */
    private String authTargetName;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 创建时间
     */
    private Date createTime;

}
