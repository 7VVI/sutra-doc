package com.hmoob.media.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.media.domain.MediaTag;

import java.io.Serial;

/**
 * 标签业务对象 media_tag
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MediaTag.class, reverseConvertGenerate = false)
public class MediaTagBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    private String tagName;

    /**
     * 标签类型
     */
    @NotNull(message = "标签类型不能为空")
    private Integer tagType;

    /**
     * 父级标签ID
     */
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态: 0-正常, 1-禁用
     */
    private Integer status;

}
