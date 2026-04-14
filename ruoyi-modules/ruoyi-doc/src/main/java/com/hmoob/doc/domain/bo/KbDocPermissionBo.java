package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbDocPermission;

import java.io.Serial;

/**
 * KB文档权限业务对象 kb_doc_permission
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbDocPermission.class, reverseConvertGenerate = false)
public class KbDocPermissionBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    private Long permissionId;

    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long docId;

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
     * 数据范围: NULL-继承目录设置
     */
    private Integer dataScope;

}