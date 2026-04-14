package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbSysConfig;

import java.io.Serial;

/**
 * KB系统配置业务对象 kb_sys_config
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbSysConfig.class, reverseConvertGenerate = false)
public class KbSysConfigBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 配置项编码
     */
    @NotBlank(message = "配置项编码不能为空")
    @Size(max = 100, message = "配置项编码长度不能超过{max}个字符")
    private String itemCode;

    /**
     * 配置项名称
     */
    @NotBlank(message = "配置项名称不能为空")
    @Size(max = 100, message = "配置项名称长度不能超过{max}个字符")
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
    @NotNull(message = "排序号不能为空")
    private Integer sortOrder;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 状态: 1-启用, 0-禁用
     */
    private Integer status;

}