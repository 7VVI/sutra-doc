package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbDocPermission;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档权限视图对象 kb_doc_permission
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbDocPermission.class)
public class KbDocPermissionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    @ExcelProperty(value = "权限ID")
    private Long permissionId;

    /**
     * 文档ID
     */
    @ExcelProperty(value = "文档ID")
    private Long docId;

    /**
     * 授权对象类型: 1-用户, 2-角色
     */
    @ExcelProperty(value = "授权对象类型")
    private Integer subjectType;

    /**
     * 授权对象ID(用户ID或角色ID)
     */
    @ExcelProperty(value = "授权对象ID")
    private Long subjectId;

    /**
     * 查看权限: 0-无, 1-有
     */
    @ExcelProperty(value = "查看权限")
    private Integer permView;

    /**
     * 下载权限: 0-无, 1-有
     */
    @ExcelProperty(value = "下载权限")
    private Integer permDownload;

    /**
     * 编辑权限: 0-无, 1-有
     */
    @ExcelProperty(value = "编辑权限")
    private Integer permEdit;

    /**
     * 删除权限: 0-无, 1-有
     */
    @ExcelProperty(value = "删除权限")
    private Integer permDelete;

    /**
     * 数据范围: NULL-继承目录设置
     */
    private Integer dataScope;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}