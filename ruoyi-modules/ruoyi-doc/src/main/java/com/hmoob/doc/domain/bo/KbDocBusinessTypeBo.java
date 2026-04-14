package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbDocBusinessType;

import java.io.Serial;

/**
 * KB文档业务类型关联业务对象 kb_doc_business_type
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbDocBusinessType.class, reverseConvertGenerate = false)
public class KbDocBusinessTypeBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    private Long businessTypeId;

    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long docId;

    /**
     * 业务类型编码
     */
    private String businessType;

    /**
     * 排序号
     */
    private Integer sortOrder;

}