package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文件变化记录实体类
 *
 * @author hmoob
 */
@Data
@TableName("kb_file_change_log")
public class KbFileChangeLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(value = "log_id")
    private Long logId;

    /**
     * 关联监控配置ID
     */
    private Long configId;

    /**
     * 文件/目录完整路径
     */
    private String filePath;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 是否目录: 0-文件, 1-目录
     */
    private Integer isDirectory;

    /**
     * 变化类型: CREATED/MODIFIED/DELETED
     */
    private String changeType;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件内容SHA256哈希(变化后)
     */
    private String contentHash;

    /**
     * 变化前的SHA256哈希
     */
    private String oldHash;

    /**
     * 关联文档ID(如有)
     */
    private Long docId;

    /**
     * 关联目录ID(如有)
     */
    private Long folderId;

    /**
     * 处理状态: 0-待处理, 1-处理成功, 2-处理失败, 3-忽略
     */
    private Integer processStatus;

    /**
     * 处理消息(错误信息等)
     */
    private String processMsg;

    /**
     * 处理完成时间
     */
    private Date processTime;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 创建时间(事件发生时间)
     */
    private Date createTime;
}
