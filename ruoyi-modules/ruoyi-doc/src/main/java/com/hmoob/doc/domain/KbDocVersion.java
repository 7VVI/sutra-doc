package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * KB文档版本实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_doc_version")
public class KbDocVersion extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 版本ID
     */
    @TableId(value = "version_id")
    private Long versionId;

    /**
     * 文档ID
     */
    private Long docId;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 版本说明/变更描述
     */
    private String versionComment;

    /**
     * 版本文件大小,单位:字节
     */
    private Long fileSize;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

}