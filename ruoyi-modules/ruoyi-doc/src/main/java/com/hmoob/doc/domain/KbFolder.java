package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * KB目录实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_folder")
public class KbFolder extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目录ID
     */
    @TableId(value = "folder_id")
    private Long folderId;

    /**
     * 部门ID（所属组织）
     */
    private Long deptId;

    /**
     * 目录名称
     */
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
    private String folderCode;

    /**
     * 排序号
     */
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
    private Integer status;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
//    @TableLogic
    private String delFlag;

    /**
     * 子目录列表
     */
    @TableField(exist = false)
    private List<KbFolder> children = new ArrayList<>();

}
