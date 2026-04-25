package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 观看者视图对象
 *
 * @author hmoob
 */
@Data
public class MediaWatcherVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String avatar;

}
