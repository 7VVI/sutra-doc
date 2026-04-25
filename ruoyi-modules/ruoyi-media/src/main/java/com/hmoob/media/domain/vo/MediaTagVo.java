package com.hmoob.media.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.media.domain.MediaTag;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 标签视图对象 media_tag
 *
 * @author hmoob
 */
@Data
@AutoMapper(target = MediaTag.class)
public class MediaTagVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 标签类型
     */
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 子标签列表
     */
    private List<MediaTagVo> children;

}
