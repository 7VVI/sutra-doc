package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbDocTopicType;

import java.io.Serial;

/**
 * KB文档主题关联业务对象 kb_doc_topic_type
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbDocTopicType.class, reverseConvertGenerate = false)
public class KbDocTopicTypeBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    private Long topicTypeId;

    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long docId;

    /**
     * 主题ID
     */
    @NotNull(message = "主题ID不能为空")
    private Long topicId;

    /**
     * 排序号
     */
    private Integer sortOrder;

}