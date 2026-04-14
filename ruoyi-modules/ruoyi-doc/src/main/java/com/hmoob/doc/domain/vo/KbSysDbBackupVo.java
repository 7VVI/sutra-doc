package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbSysDbBackup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB系统数据库备份视图对象 kb_sys_db_backup
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbSysDbBackup.class)
public class KbSysDbBackupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 备份ID
     */
    @ExcelProperty(value = "备份ID")
    private Long backupId;

    /**
     * 备份文件名
     */
    @ExcelProperty(value = "备份文件名")
    private String fileName;

    /**
     * 备份文件路径
     */
    private String filePath;

    /**
     * 文件大小,单位:字节
     */
    @ExcelProperty(value = "文件大小")
    private Long fileSize;

    /**
     * 备份类型: 1-全量备份, 2-增量备份
     */
    @ExcelProperty(value = "备份类型")
    private Integer backupType;

    /**
     * 备份状态: 0-进行中, 1-成功, 2-失败
     */
    @ExcelProperty(value = "备份状态")
    private Integer backupStatus;

    /**
     * 备份耗时,单位:毫秒
     */
    @ExcelProperty(value = "备份耗时(ms)")
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
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}