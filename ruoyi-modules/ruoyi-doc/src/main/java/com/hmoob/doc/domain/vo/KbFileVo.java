package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文件视图对象 kb_file
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbFile.class)
public class KbFileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    @ExcelProperty(value = "文件ID")
    private Long fileId;

    /**
     * 文件SHA256哈希值(用于去重)
     */
    private String sha256;

    /**
     * 物理存储路径
     */
    private String physicalPath;

    /**
     * 存储类型: oss-对象存储, local-本地存储
     */
    private String storageType;

    /**
     * 原始文件名
     */
    @ExcelProperty(value = "原始文件名")
    private String originalName;

    /**
     * 文件大小,单位:字节
     */
    @ExcelProperty(value = "文件大小")
    private Long fileSize;

    /**
     * 文件类型(扩展名)
     */
    @ExcelProperty(value = "文件类型")
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 状态: 1-正常, 0-删除
     */
    @ExcelProperty(value = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}