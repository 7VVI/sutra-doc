package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbFolderPermission;

import java.io.Serial;

/**
 * KB目录权限业务对象 kb_folder_permission
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbFolderPermission.class, reverseConvertGenerate = false)
public class KbFolderPermissionBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    private Long permissionId;

    /**
     * 目录ID
     */
    @NotNull(message = "目录ID不能为空")
    private Long folderId;

    /**
     * 授权对象类型: 1-用户, 2-角色
     */
    @NotNull(message = "授权对象类型不能为空")
    private Integer subjectType;

    /**
     * 授权对象ID(用户ID或角色ID)
     */
    @NotNull(message = "授权对象ID不能为空")
    private Long subjectId;

    /**
     * 查看权限: 0-无, 1-有
     */
    private Integer permView;

    /**
     * 下载权限: 0-无, 1-有
     */
    private Integer permDownload;

    /**
     * 编辑权限: 0-无, 1-有
     */
    private Integer permEdit;

    /**
     * 删除权限: 0-无, 1-有
     */
    private Integer permDelete;

    /**
     * 上传权限: 0-无, 1-有
     */
    private Integer permUpload;

    /**
     * 管理权限: 0-无, 1-有(包含子目录管理)
     */
    private Integer permManage;

    /**
     * 数据范围: 1-仅本人, 2-本部门, 3-本部门及下级, 4-自定义范围, 5-全部
     */
    private Integer dataScope;

    /**
     * 自定义范围(部门ID列表,逗号分隔)
     */
    private String customScope;

    /**
     * 继承标志: 1-子目录继承此权限, 0-不继承
     */
    private Integer inheritFlag;

}