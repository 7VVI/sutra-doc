package com.hmoob.media.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频点赞参数对象
 *
 * @author hmoob
 */
@Data
public class MediaVideoLikeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

}
