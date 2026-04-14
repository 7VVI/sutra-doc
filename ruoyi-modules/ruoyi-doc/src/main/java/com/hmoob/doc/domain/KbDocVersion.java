package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档版本实体类
 *
 * @author hmoob
 */
@Data
@TableName("kb_doc_version")
public class KbDocVersion implements Serializable {

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
     * 创建人ID
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

}