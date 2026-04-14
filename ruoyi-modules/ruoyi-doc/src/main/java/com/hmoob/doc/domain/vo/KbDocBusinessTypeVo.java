package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbDocBusinessType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档业务类型关联视图对象 kb_doc_business_type
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbDocBusinessType.class)
public class KbDocBusinessTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @ExcelProperty(value = "关联ID")
    private Long businessTypeId;

    /**
     * 文档ID
     */
    @ExcelProperty(value = "文档ID")
    private Long docId;

    /**
     * 业务类型编码
     */
    @ExcelProperty(value = "业务类型编码")
    private String businessType;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}