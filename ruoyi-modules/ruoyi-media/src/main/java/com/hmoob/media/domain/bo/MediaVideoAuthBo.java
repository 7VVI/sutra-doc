package com.hmoob.media.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频授权参数对象
 *
 * @author hmoob
 */
@Data
public class MediaVideoAuthBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    /**
     * 授权对象类型: 1-用户, 2-角色
     */
    @NotNull(message = "授权对象类型不能为空")
    private Integer authTargetType;

    /**
     * 授权对象ID
     */
    @NotNull(message = "授权对象ID不能为空")
    private Long authTargetId;

    /**
     * 授权对象名称
     */
    private String authTargetName;

    /**
     * 过期时间
     */
    private Date expireTime;

}
