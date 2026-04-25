package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hmoob.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 标签实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_tag")
public class MediaTag extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签ID
     */
    @TableId(value = "tag_id")
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
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

}
