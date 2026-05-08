package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbFolder;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * KB目录视图对象 kb_folder
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbFolder.class)
public class KbFolderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目录ID
     */
    @ExcelProperty(value = "目录ID")
    private Long folderId;

    /**
     * 部门ID（所属组织）
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 目录名称
     */
    @ExcelProperty(value = "目录名称")
    private String folderName;

    /**
     * 父目录ID(0表示根目录)
     */
    private Long parentId;

    /**
     * 目录层级(1-一级,2-二级...)
     */
    private Integer folderLevel;

    /**
     * 目录完整路径(如:/研发部/技术文档)
     */
    private String folderPath;

    /**
     * 目录编码(业务标识)
     */
    @ExcelProperty(value = "目录编码")
    private String folderCode;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
    private Integer sortOrder;

    /**
     * 目录描述
     */
    private String description;

    /**
     * 目录图标
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
     * 是否有子节点
     */
    private Boolean hasChildren;

    /**
     * 子目录列表
     */
    private List<KbFolderVo> children = new ArrayList<>();

}