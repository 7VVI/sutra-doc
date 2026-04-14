package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB系统数据库备份实体类
 *
 * @author hmoob
 */
@Data
@TableName("kb_sys_db_backup")
public class KbSysDbBackup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 备份ID
     */
    @TableId(value = "backup_id")
    private Long backupId;

    /**
     * 备份文件名
     */
    private String fileName;

    /**
     * 备份文件路径
     */
    private String filePath;

    /**
     * 文件大小,单位:字节
     */
    private Long fileSize;

    /**
     * 备份类型: 1-全量备份, 2-增量备份
     */
    private Integer backupType;

    /**
     * 备份状态: 0-进行中, 1-成功, 2-失败
     */
    private Integer backupStatus;

    /**
     * 备份耗时,单位:毫秒
     */
    private Integer backupDurationMs;

    /**
     * 备份范围(表名列表)
     */
    private String backupRange;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 备注
     */
    private String remark;

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