package com.hmoob.media.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 视频上传参数对象
 *
 * @author hmoob
 */
@Data
public class MediaVideoUploadBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视频标题
     */
    @NotBlank(message = "视频标题不能为空")
    private String title;

    /**
     * 视频描述
     */
    private String description;

    /**
     * 作者ID
     */
    @NotNull(message = "作者ID不能为空")
    private Long authorId;

    /**
     * 授权类型: 0-公开, 1-指定用户/角色
     */
    private Integer authType;

    /**
     * 分类标签ID列表
     */
    private List<Long> categoryIds;

    /**
     * 方向标签ID列表
     */
    private List<Long> directionIds;

}
