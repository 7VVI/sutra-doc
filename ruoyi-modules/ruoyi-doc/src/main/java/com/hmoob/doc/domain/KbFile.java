package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;
import com.hmoob.doc.enums.StorageTypeEnum;

import java.io.Serial;

/**
 * KB文件实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_file")
public class KbFile extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    @TableId(value = "file_id")
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
     * @see StorageTypeEnum
     */
    private String storageType;

    /**
     * 原始文件名
     */
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

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

}