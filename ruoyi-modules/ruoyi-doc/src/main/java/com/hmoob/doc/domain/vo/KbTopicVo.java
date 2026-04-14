package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbTopic;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * KB主题视图对象 kb_topic
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbTopic.class)
public class KbTopicVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主题ID
     */
    @ExcelProperty(value = "主题ID")
    private Long topicId;

    /**
     * 主题编码
     */
    @ExcelProperty(value = "主题编码")
    private String topicCode;

    /**
     * 主题名称
     */
    @ExcelProperty(value = "主题名称")
    private String topicName;

    /**
     * 父主题ID(0表示根节点)
     */
    private Long parentId;

    /**
     * 主题层级
     */
    @ExcelProperty(value = "主题层级")
    private Integer topicLevel;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
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
    @ExcelProperty(value = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 子主题列表
     */
    private List<KbTopicVo> children = new ArrayList<>();

}