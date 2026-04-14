package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbDocVersion;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档版本视图对象 kb_doc_version
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbDocVersion.class)
public class KbDocVersionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 版本ID
     */
    @ExcelProperty(value = "版本ID")
    private Long versionId;

    /**
     * 文档ID
     */
    @ExcelProperty(value = "文档ID")
    private Long docId;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 版本号
     */
    @ExcelProperty(value = "版本号")
    private Integer versionNo;

    /**
     * 版本说明/变更描述
     */
    @ExcelProperty(value = "版本说明")
    private String versionComment;

    /**
     * 版本文件大小,单位:字节
     */
    @ExcelProperty(value = "文件大小")
    private Long fileSize;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}