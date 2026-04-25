package com.hmoob.media.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频分享参数对象
 *
 * @author hmoob
 */
@Data
public class MediaVideoShareBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 最大访问次数
     */
    private Integer maxAccess;

}
