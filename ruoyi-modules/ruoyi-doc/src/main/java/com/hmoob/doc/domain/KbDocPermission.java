package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * KB文档权限实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_doc_permission")
public class KbDocPermission extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    @TableId(value = "permission_id")
    private Long permissionId;

    /**
     * 文档ID
     */
    private Long docId;

    /**
     * 授权对象类型: 1-用户, 2-角色
     */
    private Integer subjectType;

    /**
     * 授权对象ID(用户ID或角色ID)
     */
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

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

}