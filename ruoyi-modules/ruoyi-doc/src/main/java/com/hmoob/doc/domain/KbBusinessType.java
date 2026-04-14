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
 * KB业务类型实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_business_type")
public class KbBusinessType extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型ID
     */
    @TableId(value = "business_type_id")
    private Long businessTypeId;

    /**
     * 业务类型编码
     */
    private String businessTypeCode;

    /**
     * 业务类型名称
     */
    private String businessTypeName;

    /**
     * 父业务类型ID(0表示根节点)
     */
    private Long parentId;

    /**
     * 类型层级
     */
    private Integer typeLevel;

    /**
     * 排序号
     */
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
    private Integer status;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

    /**
     * 子业务类型列表
     */
    @TableField(exist = false)
    private List<KbBusinessType> children = new ArrayList<>();

}