package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbBusinessType;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * KB业务类型视图对象 kb_business_type
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbBusinessType.class)
public class KbBusinessTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型ID
     */
    @ExcelProperty(value = "业务类型ID")
    private Long businessTypeId;

    /**
     * 业务类型编码
     */
    @ExcelProperty(value = "业务类型编码")
    private String businessTypeCode;

    /**
     * 业务类型名称
     */
    @ExcelProperty(value = "业务类型名称")
    private String businessTypeName;

    /**
     * 父业务类型ID(0表示根节点)
     */
    private Long parentId;

    /**
     * 类型层级
     */
    @ExcelProperty(value = "类型层级")
    private Integer typeLevel;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
    private Integer sortOrder;

    /**
     * 类型描述
     */
    private String description;

    /**
     * 类型图标
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
     * 子业务类型列表
     */
    private List<KbBusinessTypeVo> children = new ArrayList<>();

}