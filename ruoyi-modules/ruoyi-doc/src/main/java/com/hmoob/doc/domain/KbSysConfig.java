package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * KB系统配置实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_sys_config")
public class KbSysConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @TableId(value = "config_id")
    private Long configId;

    /**
     * 配置项编码
     */
    private String itemCode;

    /**
     * 配置项名称
     */
    private String itemName;

    /**
     * 配置项值
     */
    private String itemValue;

    /**
     * 值类型: string-字符串, number-数字, boolean-布尔, json-JSON对象, array-数组
     */
    private String valueType;

    /**
     * 可选值列表(JSON格式)
     */
    private String valueOptions;

    /**
     * 配置分组
     */
    private String itemGroup;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 状态: 1-启用, 0-禁用
     */
    private Integer status;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

}