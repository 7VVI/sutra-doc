package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbBusinessType;

import java.io.Serial;

/**
 * KB业务类型业务对象 kb_business_type
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbBusinessType.class, reverseConvertGenerate = false)
public class KbBusinessTypeBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型ID
     */
    private Long businessTypeId;

    /**
     * 业务类型编码
     */
    @NotBlank(message = "业务类型编码不能为空")
    @Size(max = 50, message = "业务类型编码长度不能超过{max}个字符")
    private String businessTypeCode;

    /**
     * 业务类型名称
     */
    @NotBlank(message = "业务类型名称不能为空")
    @Size(max = 100, message = "业务类型名称长度不能超过{max}个字符")
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
    @NotNull(message = "排序号不能为空")
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

}