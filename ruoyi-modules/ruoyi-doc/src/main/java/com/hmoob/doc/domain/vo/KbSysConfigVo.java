package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbSysConfig;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB系统配置视图对象 kb_sys_config
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbSysConfig.class)
public class KbSysConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @ExcelProperty(value = "配置ID")
    private Long configId;

    /**
     * 配置项编码
     */
    @ExcelProperty(value = "配置项编码")
    private String itemCode;

    /**
     * 配置项名称
     */
    @ExcelProperty(value = "配置项名称")
    private String itemName;

    /**
     * 配置项值
     */
    @ExcelProperty(value = "配置项值")
    private String itemValue;

    /**
     * 值类型: string-字符串, number-数字, boolean-布尔, json-JSON对象, array-数组
     */
    @ExcelProperty(value = "值类型")
    private String valueType;

    /**
     * 可选值列表(JSON格式)
     */
    private String valueOptions;

    /**
     * 配置分组
     */
    @ExcelProperty(value = "配置分组")
    private String itemGroup;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
    private Integer sortOrder;

    /**
     * 备注说明
     */
    private String remark;

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

}