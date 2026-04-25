package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频分享视图对象
 *
 * @author hmoob
 */
@Data
public class MediaVideoShareVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分享ID
     */
    private Long id;

    /**
     * 分享令牌
     */
    private String shareToken;

    /**
     * 分享URL
     */
    private String shareUrl;

}
