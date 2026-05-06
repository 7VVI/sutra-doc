-- =====================================================
-- KB文件变化监控模块表结构设计
--
-- 创建时间: 2026-05-06
-- 模块说明: 文件变化监控与自动索引跟踪
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. kb_file_watch_config - 文件监控配置表
-- 说明: 配置需要监控的目录路径及监控策略
-- =====================================================
DROP TABLE IF EXISTS kb_file_watch_config;
CREATE TABLE kb_file_watch_config (
    `config_id`       bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `watch_path`      VARCHAR(500) NOT NULL COMMENT '监控目录路径',
    `watch_name`      VARCHAR(100) NOT NULL COMMENT '监控名称(便于识别)',
    `folder_id`       bigint DEFAULT 0 COMMENT '关联KB目录ID(新增文件自动归入此目录)',
    `recursive`       tinyint(4) DEFAULT 1 COMMENT '是否递归监控子目录: 0-否, 1-是',
    `include_pattern` VARCHAR(500) COMMENT '包含的文件模式(正则,逗号分隔,如:*.pdf,*.docx)',
    `exclude_pattern` VARCHAR(500) COMMENT '排除的文件模式(正则,逗号分隔,如:*.tmp,~$*)',
    `auto_index`      tinyint(4) DEFAULT 1 COMMENT '自动索引到ES: 0-否, 1-是',
    `auto_add_doc`    tinyint(4) DEFAULT 1 COMMENT '新文件自动添加到文档库: 0-否, 1-是',
    `scan_interval`   int DEFAULT 300 COMMENT '全量扫描间隔(秒)',
    `debounce_ms`     int DEFAULT 500 COMMENT '事件防抖时间(毫秒)',
    `status`          tinyint(4) DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `tenant_id`       VARCHAR(20) DEFAULT '000000' COMMENT '租户编号',
    `create_by`       bigint COMMENT '创建人ID',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       bigint COMMENT '更新人ID',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文件监控配置表';

CREATE INDEX idx_status ON kb_file_watch_config(status);
CREATE INDEX idx_tenant_id ON kb_file_watch_config(tenant_id);
CREATE INDEX idx_del_flag ON kb_file_watch_config(del_flag);

-- =====================================================
-- 2. kb_file_change_log - 文件变化记录表
-- 说明: 记录所有监控到的文件变化事件，便于追溯问题
-- =====================================================
DROP TABLE IF EXISTS kb_file_change_log;
CREATE TABLE kb_file_change_log (
    `log_id`          bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `config_id`       bigint NOT NULL COMMENT '关联监控配置ID',
    `file_path`       VARCHAR(500) NOT NULL COMMENT '文件/目录完整路径',
    `file_name`       VARCHAR(200) COMMENT '文件名',
    `is_directory`    tinyint(4) DEFAULT 0 COMMENT '是否目录: 0-文件, 1-目录',
    `change_type`     VARCHAR(20) NOT NULL COMMENT '变化类型: CREATED/MODIFIED/DELETED',
    `file_size`       bigint COMMENT '文件大小(字节)',
    `content_hash`    VARCHAR(64) COMMENT '文件内容SHA256哈希(变化后)',
    `old_hash`        VARCHAR(64) COMMENT '变化前的SHA256哈希',
    `doc_id`          bigint COMMENT '关联文档ID(如有)',
    `folder_id`       bigint COMMENT '关联目录ID(如有)',
    `process_status`  tinyint(4) DEFAULT 0 COMMENT '处理状态: 0-待处理, 1-处理成功, 2-处理失败, 3-忽略',
    `process_msg`     VARCHAR(500) COMMENT '处理消息(错误信息等)',
    `process_time`    DATETIME COMMENT '处理完成时间',
    `tenant_id`       VARCHAR(20) DEFAULT '000000' COMMENT '租户编号',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(事件发生时间)',
    PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文件变化记录表';

CREATE INDEX idx_config_id ON kb_file_change_log(config_id);
CREATE INDEX idx_file_path ON kb_file_change_log(file_path(100));
CREATE INDEX idx_change_type ON kb_file_change_log(change_type);
CREATE INDEX idx_process_status ON kb_file_change_log(process_status);
CREATE INDEX idx_create_time ON kb_file_change_log(create_time);
CREATE INDEX idx_tenant_id ON kb_file_change_log(tenant_id);
CREATE INDEX idx_doc_id ON kb_file_change_log(doc_id);

SET FOREIGN_KEY_CHECKS = 1;
