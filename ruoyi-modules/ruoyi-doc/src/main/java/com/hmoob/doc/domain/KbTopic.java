package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * KB主题实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_topic")
public class KbTopic extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主题ID
     */
    @TableId(value = "topic_id")
    private Long topicId;

    /**
     * 主题编码
     */
    private String topicCode;

    /**
     * 主题名称
     */
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

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

    /**
     * 子主题列表
     */
    @TableField(exist = false)
    private List<KbTopic> children = new ArrayList<>();

}