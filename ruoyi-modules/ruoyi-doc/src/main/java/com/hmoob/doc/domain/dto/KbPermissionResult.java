package com.hmoob.doc.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * KB权限计算结果DTO
 * 用于存储权限计算后的最终结果
 *
 * @author hmoob
 */
@Data
public class KbPermissionResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否有查看权限
     */
    private boolean canView;

    /**
     * 是否有下载权限
     */
    private boolean canDownload;

    /**
     * 是否有编辑权限
     */
    private boolean canEdit;

    /**
     * 是否有删除权限
     */
    private boolean canDelete;

    /**
     * 是否有上传权限(仅目录权限)
     */
    private boolean canUpload;

    /**
     * 是否有管理权限(仅目录权限)
     */
    private boolean canManage;

    /**
     * 数据范围: 1-仅本人, 2-本部门, 3-本部门及下级, 4-自定义范围, 5-全部
     */
    private Integer dataScope;

    /**
     * 来源权限记录ID
     */
    private Long sourceId;

    /**
     * 权限来源类型: "DOC"-文档直接权限, "FOLDER"-目录权限, "INHERITED"-继承权限
     */
    private String sourceType;

    /**
     * 判断是否有任何权限
     *
     * @return 是否有权限
     */
    public boolean hasAnyPermission() {
        return canView || canDownload || canEdit || canDelete || canUpload || canManage;
    }

    /**
     * 创建无权限的结果
     *
     * @return 无权限结果
     */
    public static KbPermissionResult noPermission() {
        KbPermissionResult result = new KbPermissionResult();
        result.setCanView(false);
        result.setCanDownload(false);
        result.setCanEdit(false);
        result.setCanDelete(false);
        result.setCanUpload(false);
        result.setCanManage(false);
        result.setSourceType("NONE");
        return result;
    }

}