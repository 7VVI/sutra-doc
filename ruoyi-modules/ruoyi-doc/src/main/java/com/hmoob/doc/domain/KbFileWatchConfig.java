package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 文件监控配置实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_file_watch_config")
public class KbFileWatchConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @TableId(value = "config_id")
    private Long configId;

    /**
     * 监控目录路径
     */
    private String watchPath;

    /**
     * 监控名称(便于识别)
     */
    private String watchName;

    /**
     * 关联KB目录ID(新增文件自动归入此目录)
     */
    private Long folderId;

    /**
     * 是否递归监控子目录: 0-否, 1-是
     */
    @TableField("`recursive`")
    private Integer recursive;

    /**
     * 包含的文件模式(逗号分隔,如:*.pdf,*.docx)
     */
    private String includePattern;

    /**
     * 排除的文件模式(逗号分隔,如:*.tmp,~$*)
     */
    private String excludePattern;

    /**
     * 自动索引到ES: 0-否, 1-是
     */
    private Integer autoIndex;

    /**
     * 新文件自动添加到文档库: 0-否, 1-是
     */
    private Integer autoAddDoc;

    /**
     * 全量扫描间隔(秒)
     */
    private Integer scanInterval;

    /**
     * 事件防抖时间(毫秒)
     */
    private Integer debounceMs;

    /**
     * 状态: 1-启用, 0-禁用
     */
    private Integer status;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

    /**
     * 创建部门
     */
    @TableField(fill = FieldFill.INSERT,exist = false)
    private Long createDept;

}
