package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbSysDbBackup;

import java.io.Serial;

/**
 * KB系统数据库备份业务对象 kb_sys_db_backup
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbSysDbBackup.class, reverseConvertGenerate = false)
public class KbSysDbBackupBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 备份ID
     */
    private Long backupId;

    /**
     * 备份文件名
     */
    @NotBlank(message = "备份文件名不能为空")
    @Size(max = 255, message = "备份文件名长度不能超过{max}个字符")
    private String fileName;

    /**
     * 备份文件路径
     */
    @NotBlank(message = "备份文件路径不能为空")
    private String filePath;

    /**
     * 文件大小,单位:字节
     */
    private Long fileSize;

    /**
     * 备份类型: 1-全量备份, 2-增量备份
     */
    @NotNull(message = "备份类型不能为空")
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

}