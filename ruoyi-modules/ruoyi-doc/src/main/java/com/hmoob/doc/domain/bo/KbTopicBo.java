package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbTopic;

import java.io.Serial;

/**
 * KB主题业务对象 kb_topic
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbTopic.class, reverseConvertGenerate = false)
public class KbTopicBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主题ID
     */
    private Long topicId;

    /**
     * 主题编码
     */
    @NotBlank(message = "主题编码不能为空")
    @Size(max = 50, message = "主题编码长度不能超过{max}个字符")
    private String topicCode;

    /**
     * 主题名称
     */
    @NotBlank(message = "主题名称不能为空")
    @Size(max = 100, message = "主题名称长度不能超过{max}个字符")
    private String topicName;

    /**
     * 父主题ID(0表示根节点)
     */
    private Long parentId;

    /**
     * 主题层级
     */
    private Integer topicLevel;

    /**
     * 排序号
     */
    @NotNull(message = "排序号不能为空")
    private Integer sortOrder;

    /**
     * 主题描述
     */
    private String description;

    /**
     * 主题图标
     */
    private String icon;

    /**
     * 状态: 1-启用, 0-禁用
     */
    private Integer status;

}