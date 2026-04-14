package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbFolderPermission;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB目录权限视图对象 kb_folder_permission
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbFolderPermission.class)
public class KbFolderPermissionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    @ExcelProperty(value = "权限ID")
    private Long permissionId;

    /**
     * 目录ID
     */
    @ExcelProperty(value = "目录ID")
    private Long folderId;

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
     * 上传权限: 0-无, 1-有
     */
    @ExcelProperty(value = "上传权限")
    private Integer permUpload;

    /**
     * 管理权限: 0-无, 1-有(包含子目录管理)
     */
    @ExcelProperty(value = "管理权限")
    private Integer permManage;

    /**
     * 数据范围: 1-仅本人, 2-本部门, 3-本部门及下级, 4-自定义范围, 5-全部
     */
    @ExcelProperty(value = "数据范围")
    private Integer dataScope;

    /**
     * 自定义范围(部门ID列表,逗号分隔)
     */
    private String customScope;

    /**
     * 继承标志: 1-子目录继承此权限, 0-不继承
     */
    @ExcelProperty(value = "继承标志")
    private Integer inheritFlag;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}