package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbDocVersion;

import java.io.Serial;

/**
 * KB文档版本业务对象 kb_doc_version
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbDocVersion.class, reverseConvertGenerate = false)
public class KbDocVersionBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 版本ID
     */
    private Long versionId;

    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long docId;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 版本号
     */
    @NotNull(message = "版本号不能为空")
    private Integer versionNo;

    /**
     * 版本说明/变更描述
     */
    private String versionComment;

    /**
     * 版本文件大小,单位:字节
     */
    private Long fileSize;

}