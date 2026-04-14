package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbFile;

import java.io.Serial;

/**
 * KB文件业务对象 kb_file
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbFile.class, reverseConvertGenerate = false)
public class KbFileBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 文件SHA256哈希值(用于去重)
     */
    private String sha256;

    /**
     * 物理存储路径
     */
    @NotBlank(message = "物理存储路径不能为空")
    private String physicalPath;

    /**
     * 原始文件名
     */
    @NotBlank(message = "原始文件名不能为空")
    @Size(max = 255, message = "原始文件名长度不能超过{max}个字符")
    private String originalName;

    /**
     * 文件大小,单位:字节
     */
    private Long fileSize;

    /**
     * 文件类型(扩展名)
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 状态: 1-正常, 0-删除
     */
    private Integer status;

}