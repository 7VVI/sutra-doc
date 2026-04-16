-- =====================================================
-- KB知识库模块表结构设计
-- 重构优化版本 - 符合MySQL数据库设计规范
--
-- 创建时间: 2026-04-14
-- 模块说明: 知识库管理系统核心表结构
-- 版本: v2.0
-- =====================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. kb_file - 文件表
-- 说明: 存储物理文件的基础信息，通过SHA256实现文件去重
-- =====================================================
DROP TABLE IF EXISTS kb_file;
CREATE TABLE kb_file (
    `file_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `sha256` VARCHAR(64) NOT NULL COMMENT '文件SHA256哈希值(用于去重)',
    `physical_path` VARCHAR(255) NOT NULL COMMENT '物理存储路径',
    `original_name` VARCHAR(200) COMMENT '原始文件名',
    `file_size` bigint DEFAULT 0 COMMENT '文件大小,单位:字节',
    `file_type` VARCHAR(20) COMMENT '文件类型(扩展名)',
    `mime_type` VARCHAR(255) COMMENT 'MIME类型',
    `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-正常, 0-删除',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文件表';

-- 文件表索引
CREATE UNIQUE INDEX uk_sha256 ON kb_file(sha256);
CREATE INDEX idx_status ON kb_file(status);
CREATE INDEX idx_file_type ON kb_file(file_type);
CREATE INDEX idx_del_flag ON kb_file(del_flag);


-- =====================================================
-- 2. kb_folder - 目录表 (新增)
-- 说明: 存储文档目录的层级结构，支持树形组织
-- =====================================================
DROP TABLE IF EXISTS kb_folder;
CREATE TABLE kb_folder (
    `folder_id` bigint NOT NULL AUTO_INCREMENT COMMENT '目录ID',
    `folder_name` VARCHAR(100) NOT NULL COMMENT '目录名称',
    `parent_id` bigint DEFAULT 0 COMMENT '父目录ID(0表示根目录)',
    `folder_level` INT DEFAULT 1 COMMENT '目录层级(1-一级,2-二级...)',
    `folder_path` VARCHAR(500) COMMENT '目录完整路径(如:/研发部/技术文档)',
    `folder_code` VARCHAR(32) COMMENT '目录编码(业务标识)',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `description` VARCHAR(200) COMMENT '目录描述',
    `icon` VARCHAR(50) COMMENT '目录图标',
    `doc_count` bigint DEFAULT 0 COMMENT '文档数量(含子目录)',
    `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (folder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '目录表';

-- 目录表索引
CREATE INDEX idx_parent_id ON kb_folder(parent_id);
CREATE INDEX idx_folder_level ON kb_folder(folder_level);
CREATE INDEX idx_status ON kb_folder(status);
CREATE INDEX idx_del_flag ON kb_folder(del_flag);
CREATE UNIQUE INDEX uk_folder_code ON kb_folder(folder_code);
CREATE INDEX idx_folder_path ON kb_folder(folder_path(100));


-- =====================================================
-- 3. kb_folder_permission - 目录权限表 (新增)
-- 说明: 存储目录级别的权限设置，支持权限继承
-- =====================================================
DROP TABLE IF EXISTS kb_folder_permission;
CREATE TABLE kb_folder_permission (
    `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `folder_id` bigint NOT NULL COMMENT '目录ID',
    `subject_type` tinyint(4) NOT NULL COMMENT '授权对象类型: 1-用户, 2-角色',
    `subject_id` bigint NOT NULL COMMENT '授权对象ID(用户ID或角色ID)',
    -- 操作权限
    `perm_view` tinyint(4) DEFAULT 0 COMMENT '查看权限: 0-无, 1-有',
    `perm_download` tinyint(4) DEFAULT 0 COMMENT '下载权限: 0-无, 1-有',
    `perm_edit` tinyint(4) DEFAULT 0 COMMENT '编辑权限: 0-无, 1-有',
    `perm_delete` tinyint(4) DEFAULT 0 COMMENT '删除权限: 0-无, 1-有',
    `perm_upload` tinyint(4) DEFAULT 0 COMMENT '上传权限: 0-无, 1-有',
    `perm_manage` tinyint(4) DEFAULT 0 COMMENT '管理权限: 0-无, 1-有(包含子目录管理)',
    -- 数据范围
    `data_scope` tinyint(4) DEFAULT 1 COMMENT '数据范围: 1-仅本人, 2-本部门, 3-本部门及下级, 4-自定义范围, 5-全部',
    `custom_scope` VARCHAR(500) COMMENT '自定义范围(部门ID列表,逗号分隔)',
    -- 权限继承设置
    `inherit_flag` tinyint(4) DEFAULT 1 COMMENT '继承标志: 1-子目录继承此权限, 0-不继承',
    -- 审计字段
    `create_by` bigint COMMENT '创建人ID(授权人)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '目录权限表';

-- 目录权限表索引
CREATE INDEX idx_folder_id ON kb_folder_permission(folder_id);
CREATE INDEX idx_subject ON kb_folder_permission(subject_type, subject_id);
CREATE INDEX idx_data_scope ON kb_folder_permission(data_scope);
CREATE INDEX idx_inherit_flag ON kb_folder_permission(inherit_flag);
CREATE INDEX idx_del_flag ON kb_folder_permission(del_flag);
-- 联合唯一索引：同一目录同一对象只能有一条权限记录
CREATE UNIQUE INDEX uk_folder_subject ON kb_folder_permission(folder_id, subject_type, subject_id, del_flag);


-- =====================================================
-- 4. kb_doc - 文档主表
-- 说明: 知识文档的核心业务表，存储文档基本信息及统计数据
-- =====================================================
DROP TABLE IF EXISTS kb_doc;
CREATE TABLE kb_doc (
    `doc_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `folder_id` bigint NOT NULL DEFAULT 0 COMMENT '所属目录ID(0表示根目录/未分类)',
    `file_id` bigint NOT NULL COMMENT '关联文件ID',
    `preview_file_id` bigint COMMENT '预览文件ID(PDF转换后)',
    `original_preview_file_id` bigint COMMENT '原始预览文件ID',
    `index_id` VARCHAR(64) COMMENT '全文检索索引ID',
    `org_code` VARCHAR(64) COMMENT '组织编码',
    `dep_id` bigint COMMENT '部门ID',
    `doc_name` VARCHAR(200) NOT NULL COMMENT '文档名称',
    `doc_title` VARCHAR(200) COMMENT '文档标题',
    `serial_number` VARCHAR(64) COMMENT '文档编号(业务唯一标识)',
    `file_type` VARCHAR(20) COMMENT '文件类型',
    `file_size` bigint DEFAULT 0 COMMENT '文件大小,单位:字节',
    `category` VARCHAR(32) COMMENT '文档分类(字典:kb_dict_category)',
    `keywords` VARCHAR(500) COMMENT '关键词(逗号分隔)',
    `status` tinyint(4) DEFAULT 0 COMMENT '状态: 0-待处理, 1-已发布, 2-已撤回, 3-已归档, 4-处理失败',
    `fti_flag` tinyint(4) DEFAULT 0 COMMENT '全文检索标志: 0-否, 1-是(字典:dict_fti_flag)',
    `convert_flag` tinyint(4) DEFAULT 0 COMMENT '转换标志: 0-未转换, 1-转换成功, 2-转换失败, 3-转换中',
    `release_flag` tinyint(4) DEFAULT 0 COMMENT '发布标志: 0-未发布, 1-已发布',
    `public_remark` tinyint(4) DEFAULT 0 COMMENT '公开备注标志: 0-不公开, 1-公开(字典:dict_public_remark)',
    `current_version` INT DEFAULT 1 COMMENT '当前版本号',
    `download_count` bigint DEFAULT 0 COMMENT '下载次数',
    `comment_count` bigint DEFAULT 0 COMMENT '评论次数',
    `view_count` bigint DEFAULT 0 COMMENT '浏览次数',
    `favourite_count` bigint DEFAULT 0 COMMENT '收藏次数',
    `process_msg` VARCHAR(500) COMMENT '处理消息(错误信息等)',
    `remark` TEXT COMMENT '备注',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文档表';

-- 文档表索引
CREATE INDEX idx_folder_id ON kb_doc(folder_id);
CREATE INDEX idx_file_id ON kb_doc(file_id);
CREATE INDEX idx_preview_file_id ON kb_doc(preview_file_id);
CREATE INDEX idx_org_code ON kb_doc(org_code);
CREATE INDEX idx_dep_id ON kb_doc(dep_id);
CREATE INDEX idx_status ON kb_doc(status);
CREATE INDEX idx_category ON kb_doc(category);
CREATE INDEX idx_release_flag ON kb_doc(release_flag);
CREATE INDEX idx_fti_flag ON kb_doc(fti_flag);
CREATE INDEX idx_convert_flag ON kb_doc(convert_flag);
CREATE INDEX idx_create_time ON kb_doc(create_time);
CREATE INDEX idx_del_flag ON kb_doc(del_flag);
CREATE INDEX idx_del_create_time ON kb_doc(del_flag, create_time);
CREATE INDEX idx_folder_status ON kb_doc(folder_id, status);
CREATE UNIQUE INDEX uk_serial_number ON kb_doc(serial_number);


-- =====================================================
-- 5. kb_doc_permission - 文档权限表 (新增)
-- 说明: 存储单个文档的权限覆盖设置
-- =====================================================
DROP TABLE IF EXISTS kb_doc_permission;
CREATE TABLE kb_doc_permission (
    `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `doc_id` bigint NOT NULL COMMENT '文档ID',
    `subject_type` tinyint(4) NOT NULL COMMENT '授权对象类型: 1-用户, 2-角色',
    `subject_id` bigint NOT NULL COMMENT '授权对象ID(用户ID或角色ID)',
    -- 操作权限(文档级别仅4项，上传和管理属于目录级权限)
    `perm_view` tinyint(4) DEFAULT 0 COMMENT '查看权限: 0-无, 1-有',
    `perm_download` tinyint(4) DEFAULT 0 COMMENT '下载权限: 0-无, 1-有',
    `perm_edit` tinyint(4) DEFAULT 0 COMMENT '编辑权限: 0-无, 1-有',
    `perm_delete` tinyint(4) DEFAULT 0 COMMENT '删除权限: 0-无, 1-有',
    -- 数据范围(文档级别通常继承目录设置)
    `data_scope` tinyint(4) DEFAULT NULL COMMENT '数据范围: NULL-继承目录设置, 1-仅本人, 2-本部门, 3-本部门及下级, 4-自定义范围, 5-全部',
    -- 审计字段
    `create_by` bigint COMMENT '创建人ID(授权人)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文档权限表';

-- 文档权限表索引
CREATE INDEX idx_doc_id ON kb_doc_permission(doc_id);
CREATE INDEX idx_subject ON kb_doc_permission(subject_type, subject_id);
CREATE INDEX idx_data_scope ON kb_doc_permission(data_scope);
CREATE INDEX idx_del_flag ON kb_doc_permission(del_flag);
-- 联合唯一索引：同一文档同一对象只能有一条权限记录
CREATE UNIQUE INDEX uk_doc_subject ON kb_doc_permission(doc_id, subject_type, subject_id, del_flag);


-- =====================================================
-- 6. kb_doc_version - 文档版本表
-- 说明: 存储文档的历史版本，支持版本回溯
-- =====================================================
DROP TABLE IF EXISTS kb_doc_version;
CREATE TABLE kb_doc_version (
    `version_id` bigint NOT NULL AUTO_INCREMENT COMMENT '版本ID',
    `doc_id` bigint NOT NULL COMMENT '文档ID',
    `file_id` bigint NOT NULL COMMENT '文件ID',
    `version_no` INT NOT NULL COMMENT '版本号',
    `version_comment` TEXT COMMENT '版本说明/变更描述',
    `file_size` bigint DEFAULT 0 COMMENT '版本文件大小,单位:字节',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文档版本表';

-- 文档版本表索引
CREATE INDEX idx_doc_id ON kb_doc_version(doc_id);
CREATE INDEX idx_file_id ON kb_doc_version(file_id);
CREATE INDEX idx_version_no ON kb_doc_version(version_no);
CREATE INDEX idx_create_time ON kb_doc_version(create_time);
CREATE INDEX idx_del_flag ON kb_doc_version(del_flag);
CREATE UNIQUE INDEX uk_doc_version ON kb_doc_version(doc_id, version_no);


-- =====================================================
-- 7. kb_doc_comments - 文档评论表
-- 说明: 存储用户对文档的评论，支持评论回复
-- =====================================================
DROP TABLE IF EXISTS kb_doc_comments;
CREATE TABLE kb_doc_comments (
    `comment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `doc_id` bigint NOT NULL COMMENT '文档ID',
    `parent_id` bigint DEFAULT 0 COMMENT '父评论ID(用于回复,0表示一级评论)',
    `reply_to_user_id` bigint COMMENT '回复目标用户ID',
    `comment_content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` bigint DEFAULT 0 COMMENT '点赞数',
    `is_anonymous` tinyint(4) DEFAULT 0 COMMENT '是否匿名: 0-否, 1-是',
    `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-正常, 0-已删除, 2-待审核',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文档评论表';

-- 文档评论表索引
CREATE INDEX idx_doc_id ON kb_doc_comments(doc_id);
CREATE INDEX idx_parent_id ON kb_doc_comments(parent_id);
CREATE INDEX idx_create_by ON kb_doc_comments(create_by);
CREATE INDEX idx_status ON kb_doc_comments(status);
CREATE INDEX idx_create_time ON kb_doc_comments(create_time);
CREATE INDEX idx_del_flag ON kb_doc_comments(del_flag);
CREATE INDEX idx_del_create_time ON kb_doc_comments(del_flag, create_time);


-- =====================================================
-- 8. kb_doc_favourite - 文档收藏表
-- 说明: 存储用户收藏的文档记录
-- =====================================================
DROP TABLE IF EXISTS kb_doc_favourite;
CREATE TABLE kb_doc_favourite (
    `favourite_id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `doc_id` bigint NOT NULL COMMENT '文档ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `folder_name` VARCHAR(50) COMMENT '收藏文件夹名称',
    `remark` VARCHAR(200) COMMENT '收藏备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (favourite_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文档收藏表';

-- 文档收藏表索引
CREATE INDEX idx_doc_id ON kb_doc_favourite(doc_id);
CREATE INDEX idx_user_id ON kb_doc_favourite(user_id);
CREATE INDEX idx_folder_name ON kb_doc_favourite(folder_name);
CREATE INDEX idx_create_time ON kb_doc_favourite(create_time);
CREATE INDEX idx_del_flag ON kb_doc_favourite(del_flag);
-- 联合唯一索引：防止同一用户重复收藏同一文档(考虑软删除)
CREATE UNIQUE INDEX uk_user_doc ON kb_doc_favourite(user_id, doc_id, del_flag);


-- =====================================================
-- 9. kb_doc_topic_type - 文档主题类型关联表
-- 说明: 文档与主题类型的多对多关联关系
-- =====================================================
DROP TABLE IF EXISTS kb_doc_topic_type;
CREATE TABLE kb_doc_topic_type (
    `topic_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `doc_id` bigint NOT NULL COMMENT '文档ID',
    `topic_id` bigint NOT NULL COMMENT '主题ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (topic_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文档主题类型关联表';

-- 文档主题类型关联表索引
CREATE INDEX idx_doc_id ON kb_doc_topic_type(doc_id);
CREATE INDEX idx_topic_id ON kb_doc_topic_type(topic_id);
CREATE INDEX idx_del_flag ON kb_doc_topic_type(del_flag);
-- 联合唯一索引：防止同一文档重复关联同一主题
CREATE UNIQUE INDEX uk_doc_topic ON kb_doc_topic_type(doc_id, topic_id);


-- =====================================================
-- 10. kb_doc_business_type - 文档业务类型关联表
-- 说明: 文档与业务类型的多对多关联关系
-- =====================================================
DROP TABLE IF EXISTS kb_doc_business_type;
CREATE TABLE kb_doc_business_type (
    `business_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `doc_id` bigint NOT NULL COMMENT '文档ID',
    `business_type` VARCHAR(32) NOT NULL COMMENT '业务类型编码',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (business_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文档业务类型关联表';

-- 文档业务类型关联表索引
CREATE INDEX idx_doc_id ON kb_doc_business_type(doc_id);
CREATE INDEX idx_business_type ON kb_doc_business_type(business_type);
CREATE INDEX idx_del_flag ON kb_doc_business_type(del_flag);
-- 联合唯一索引：防止同一文档重复关联同一业务类型
CREATE UNIQUE INDEX uk_doc_business ON kb_doc_business_type(doc_id, business_type);


-- =====================================================
-- 11. kb_doc_visit_record - 文档访问记录表
-- 说明: 记录用户对文档的各种操作行为，用于统计分析
-- =====================================================
DROP TABLE IF EXISTS kb_doc_visit_record;
CREATE TABLE kb_doc_visit_record (
    `visit_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问记录ID',
    `doc_id` bigint COMMENT '文档ID(搜索记录可为空)',
    `folder_id` bigint COMMENT '目录ID(浏览目录时记录)',
    `visit_type` tinyint(4) NOT NULL COMMENT '访问类型: 1-上传, 2-预览, 3-下载, 4-删除, 5-编辑, 6-分享',
    `keywords` VARCHAR(200) COMMENT '搜索关键词(搜索时记录)',
    `keywords_max` VARCHAR(200) COMMENT '最大匹配关键词',
    `source_ip` VARCHAR(45) COMMENT '来源IP地址',
    `user_id` bigint COMMENT '用户ID',
    `device_type` VARCHAR(20) COMMENT '设备类型: pc, mobile, tablet',
    `browser` VARCHAR(50) COMMENT '浏览器类型',
    `referer` VARCHAR(255) COMMENT '来源页面',
    `duration_seconds` INT DEFAULT 0 COMMENT '停留时长,单位:秒',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (visit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '文档访问记录表';

-- 文档访问记录表索引
CREATE INDEX idx_doc_id ON kb_doc_visit_record(doc_id);
CREATE INDEX idx_folder_id ON kb_doc_visit_record(folder_id);
CREATE INDEX idx_visit_type ON kb_doc_visit_record(visit_type);
CREATE INDEX idx_user_id ON kb_doc_visit_record(user_id);
CREATE INDEX idx_source_ip ON kb_doc_visit_record(source_ip);
CREATE INDEX idx_create_time ON kb_doc_visit_record(create_time);
CREATE INDEX idx_del_flag ON kb_doc_visit_record(del_flag);
-- 复合索引：支持按类型和时间查询
CREATE INDEX idx_type_create_time ON kb_doc_visit_record(visit_type, create_time);
-- 复合索引：支持按文档和类型查询
CREATE INDEX idx_doc_visit_type ON kb_doc_visit_record(doc_id, visit_type);


-- =====================================================
-- 12. kb_search_record - 搜索记录表
-- 说明: 记录用户的搜索行为，用于搜索分析和优化
-- =====================================================
DROP TABLE IF EXISTS kb_search_record;
CREATE TABLE kb_search_record (
    `search_id` bigint NOT NULL AUTO_INCREMENT COMMENT '搜索记录ID',
    `keywords` VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    `keywords_max` VARCHAR(200) COMMENT '最大匹配关键词',
    `search_title` VARCHAR(200) COMMENT '搜索结果标题',
    `search_content` TEXT COMMENT '搜索内容片段',
    `topic_codes` VARCHAR(200) COMMENT '主题编码(逗号分隔)',
    `business_types` VARCHAR(200) COMMENT '业务类型(逗号分隔)',
    `folder_ids` VARCHAR(200) COMMENT '目录ID列表(逗号分隔)',
    `source_ip` VARCHAR(45) COMMENT '来源IP地址',
    `result_count` INT DEFAULT 0 COMMENT '搜索结果数量',
    `search_duration_ms` INT DEFAULT 0 COMMENT '搜索耗时,单位:毫秒',
    `search_type` tinyint(4) DEFAULT 1 COMMENT '搜索类型: 1-关键词搜索, 2-高级搜索, 3-全文检索',
    `user_id` bigint COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (search_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '搜索记录表';

-- 搜索记录表索引
CREATE INDEX idx_keywords ON kb_search_record(keywords);
CREATE INDEX idx_user_id ON kb_search_record(user_id);
CREATE INDEX idx_source_ip ON kb_search_record(source_ip);
CREATE INDEX idx_search_type ON kb_search_record(search_type);
CREATE INDEX idx_create_time ON kb_search_record(create_time);
CREATE INDEX idx_del_flag ON kb_search_record(del_flag);
-- 复合索引：支持关键词统计
CREATE INDEX idx_keywords_create_time ON kb_search_record(keywords, create_time);


-- =====================================================
-- 13. kb_topic - 主题表
-- 说明: 存储文档主题分类信息
-- =====================================================
DROP TABLE IF EXISTS kb_topic;
CREATE TABLE kb_topic (
    `topic_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主题ID',
    `topic_code` VARCHAR(32) NOT NULL COMMENT '主题编码',
    `topic_name` VARCHAR(100) NOT NULL COMMENT '主题名称',
    `parent_id` bigint DEFAULT 0 COMMENT '父主题ID(0表示根节点)',
    `topic_level` INT DEFAULT 1 COMMENT '主题层级',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `description` VARCHAR(200) COMMENT '主题描述',
    `icon` VARCHAR(50) COMMENT '主题图标',
    `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (topic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '主题表';

-- 主题表索引
CREATE UNIQUE INDEX uk_topic_code ON kb_topic(topic_code);
CREATE INDEX idx_parent_id ON kb_topic(parent_id);
CREATE INDEX idx_status ON kb_topic(status);
CREATE INDEX idx_del_flag ON kb_topic(del_flag);


-- =====================================================
-- 14. kb_business_type - 业务类型表
-- 说明: 存储业务类型分类信息
-- =====================================================
DROP TABLE IF EXISTS kb_business_type;
CREATE TABLE kb_business_type (
    `business_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '业务类型ID',
    `business_type_code` VARCHAR(32) NOT NULL COMMENT '业务类型编码',
    `business_type_name` VARCHAR(100) NOT NULL COMMENT '业务类型名称',
    `parent_id` bigint DEFAULT 0 COMMENT '父业务类型ID(0表示根节点)',
    `type_level` INT DEFAULT 1 COMMENT '类型层级',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `description` VARCHAR(200) COMMENT '类型描述',
    `icon` VARCHAR(50) COMMENT '类型图标',
    `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (business_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '业务类型表';

-- 业务类型表索引
CREATE UNIQUE INDEX uk_business_type_code ON kb_business_type(business_type_code);
CREATE INDEX idx_parent_id ON kb_business_type(parent_id);
CREATE INDEX idx_status ON kb_business_type(status);
CREATE INDEX idx_del_flag ON kb_business_type(del_flag);


-- =====================================================
-- 15. kb_sys_config - 系统配置表
-- 说明: 存储知识库系统的各项配置参数
-- =====================================================
DROP TABLE IF EXISTS kb_sys_config;
CREATE TABLE kb_sys_config (
    `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `item_code` VARCHAR(64) NOT NULL COMMENT '配置项编码',
    `item_name` VARCHAR(100) NOT NULL COMMENT '配置项名称',
    `item_value` TEXT COMMENT '配置项值',
    `value_type` VARCHAR(20) DEFAULT 'string' COMMENT '值类型: string-字符串, number-数字, boolean-布尔, json-JSON对象, array-数组',
    `value_options` TEXT COMMENT '可选值列表(JSON格式)',
    `item_group` VARCHAR(50) COMMENT '配置分组',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `remark` VARCHAR(200) COMMENT '备注说明',
    `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '系统配置表';

-- 系统配置表索引
CREATE UNIQUE INDEX uk_item_code ON kb_sys_config(item_code);
CREATE INDEX idx_item_group ON kb_sys_config(item_group);
CREATE INDEX idx_status ON kb_sys_config(status);
CREATE INDEX idx_del_flag ON kb_sys_config(del_flag);


-- =====================================================
-- 16. kb_sys_db_backup - 数据库备份记录表
-- 说明: 记录数据库备份操作的历史记录
-- =====================================================
DROP TABLE IF EXISTS kb_sys_db_backup;
CREATE TABLE kb_sys_db_backup (
    `backup_id` bigint NOT NULL AUTO_INCREMENT COMMENT '备份ID',
    `file_name` VARCHAR(200) NOT NULL COMMENT '备份文件名',
    `file_path` VARCHAR(255) NOT NULL COMMENT '备份文件路径',
    `file_size` bigint DEFAULT 0 COMMENT '文件大小,单位:字节',
    `backup_type` tinyint(4) DEFAULT 1 COMMENT '备份类型: 1-全量备份, 2-增量备份',
    `backup_status` tinyint(4) DEFAULT 0 COMMENT '备份状态: 0-进行中, 1-成功, 2-失败',
    `backup_duration_ms` INT DEFAULT 0 COMMENT '备份耗时,单位:毫秒',
    `backup_range` VARCHAR(100) COMMENT '备份范围(表名列表)',
    `error_msg` VARCHAR(500) COMMENT '错误信息',
    `remark` VARCHAR(200) COMMENT '备注',
    `create_by` bigint COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (backup_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '数据库备份记录表';

-- 数据库备份记录表索引
CREATE INDEX idx_backup_type ON kb_sys_db_backup(backup_type);
CREATE INDEX idx_backup_status ON kb_sys_db_backup(backup_status);
CREATE INDEX idx_create_time ON kb_sys_db_backup(create_time);
CREATE INDEX idx_del_flag ON kb_sys_db_backup(del_flag);


-- =====================================================
-- 初始化数据 - 默认目录结构
-- =====================================================
INSERT INTO kb_folder (folder_id, folder_name, parent_id, folder_level, folder_path, folder_code, sort_order, description, status) VALUES
(1, '知识库', 0, 1, '/知识库', 'ROOT', 1, '知识库根目录', 1),
(2, '研发中心', 1, 2, '/知识库/研发中心', 'DEV_CENTER', 1, '研发部门文档', 1),
(3, '销售中心', 1, 2, '/知识库/销售中心', 'SALES_CENTER', 2, '销售部门文档', 1),
(4, '人力资源', 1, 2, '/知识库/人力资源', 'HR_CENTER', 3, '人力资源文档', 1),
(5, '财务中心', 1, 2, '/知识库/财务中心', 'FINANCE_CENTER', 4, '财务部门文档', 1),
(6, '技术文档', 2, 3, '/知识库/研发中心/技术文档', 'TECH_DOC', 1, '技术类文档', 1),
(7, '项目文档', 2, 3, '/知识库/研发中心/项目文档', 'PROJECT_DOC', 2, '项目相关文档', 1);


-- =====================================================
-- 初始化数据 - 默认权限配置
-- =====================================================
-- 管理员角色(ID=1)授予根目录全部权限
INSERT INTO kb_folder_permission (folder_id, subject_type, subject_id, perm_view, perm_download, perm_edit, perm_delete, perm_upload, perm_manage, data_scope, inherit_flag) VALUES
(1, 2, 1, 1, 1, 1, 1, 1, 1, 5, 1);

-- 普通员工角色(ID=2)授予根目录查看下载权限(本部门数据范围)
INSERT INTO kb_folder_permission (folder_id, subject_type, subject_id, perm_view, perm_download, perm_edit, perm_delete, perm_upload, perm_manage, data_scope, inherit_flag) VALUES
(1, 2, 2, 1, 1, 0, 0, 0, 0, 2, 1);


-- =====================================================
-- 初始化数据 - 系统配置默认值
-- =====================================================
INSERT INTO kb_sys_config (item_code, item_name, item_value, value_type, item_group, sort_order, remark, status) VALUES
('kb.max_file_size', '最大文件大小', '100', 'number', 'file', 1, '单位:MB', 1),
('kb.allowed_file_types', '允许的文件类型', 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip,rar', 'array', 'file', 2, '允许上传的文件扩展名', 1),
('kb.auto_preview', '自动生成预览', 'true', 'boolean', 'file', 3, '上传后自动生成PDF预览', 1),
('kb.fti_enabled', '全文检索开关', 'true', 'boolean', 'search', 1, '是否启用全文检索功能', 1),
('kb.search_result_limit', '搜索结果数量限制', '100', 'number', 'search', 2, '搜索返回的最大结果数', 1),
('kb.comment_audit', '评论审核开关', 'false', 'boolean', 'comment', 1, '评论发布是否需要审核', 1),
('kb.backup_schedule', '备份周期', 'daily', 'string', 'backup', 1, '备份执行周期: daily, weekly, monthly', 1),
('kb.backup_keep_days', '备份保留天数', '30', 'number', 'backup', 2, '备份文件保留天数', 1),
('kb.permission_inherit_default', '权限继承默认值', 'true', 'boolean', 'permission', 1, '新建目录默认继承父目录权限', 1),
('kb.default_data_scope', '默认数据范围', '2', 'number', 'permission', 2, '新建权限默认数据范围: 1-仅本人, 2-本部门, 3-本部门及下级, 4-自定义, 5-全部', 1);


-- =====================================================
-- 初始化数据 - 默认主题
-- =====================================================
INSERT INTO kb_topic (topic_code, topic_name, parent_id, topic_level, sort_order, description, status) VALUES
('TECH', '技术文档', 0, 1, 1, '技术类文档', 1),
('MANUAL', '操作手册', 0, 1, 2, '操作说明类文档', 1),
('POLICY', '政策制度', 0, 1, 3, '公司政策和制度文档', 1),
('TRAINING', '培训资料', 0, 1, 4, '培训相关文档', 1);


-- =====================================================
-- 初始化数据 - 默认业务类型
-- =====================================================
INSERT INTO kb_business_type (business_type_code, business_type_name, parent_id, type_level, sort_order, description, status) VALUES
('DEV', '研发', 0, 1, 1, '研发部门业务', 1),
('SALES', '销售', 0, 1, 2, '销售部门业务', 1),
('HR', '人力资源', 0, 1, 3, '人力资源部门业务', 1),
('FINANCE', '财务', 0, 1, 4, '财务部门业务', 1),
('OPS', '运营', 0, 1, 5, '运营部门业务', 1);


SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 表关系说明 (ER关系图)
-- =====================================================
--
--   kb_folder (目录表)
--       │
--       │ parent_id (自引用，树形结构)
--       │
--       ├─────────────────────┐
--       │                     │
--       ▼                     ▼
--  kb_folder_permission   kb_doc (文档表)
--  (目录权限表)               │
--       │                    │ folder_id (关联目录)
--       │                    │
--       │                    ▼
--       │             kb_doc_permission
--       │             (文档权限表)
--       │                    │
--       │                    │
--       ▼                    ▼
--  用户/角色授权对象      用户/角色授权对象
--  (subject_type+        (subject_type+
--   subject_id)           subject_id)
--
--
--   kb_file (文件表)
--       │
--       │ file_id (关联)
--       ▼
--   kb_doc ──────────────────────────────────┐
--       │                                    │
--       │ doc_id                             │ doc_id
--       ▼                                    ▼
--   kb_doc_version                    kb_doc_comments
--   (版本表)                          (评论表)
--
--
--   kb_doc ──────────────────────────────────┐
--       │                                    │
--       │ doc_id                             │ doc_id
--       ▼                                    ▼
--   kb_doc_favourite                  kb_doc_visit_record
--   (收藏表)                          (访问记录表)
--
--
--   kb_topic (主题表)              kb_business_type (业务类型表)
--       │                                    │
--       │ topic_id                           │ business_type_code
--       ▼                                    ▼
--   kb_doc_topic_type               kb_doc_business_type
--   (主题关联表)                    (业务类型关联表)
--
--
--   kb_search_record                kb_sys_config
--   (搜索记录表)                    (系统配置表)
--
--
--   kb_sys_db_backup
--   (数据库备份记录表)
--
-- =====================================================
-- 权限计算优先级说明
-- =====================================================
--
-- 用户访问某文档时的权限计算流程:
--
-- 1. 查询 kb_doc_permission
--    → 是否有直接授予该用户的权限？
--    → 有：使用文档自身权限（最高优先级）
--
-- 2. 查询 kb_doc_permission
--    → 是否有授予该用户所属角色的权限？
--    → 有：使用角色权限（多个角色取权限并集）
--
-- 3. 查询 kb_folder_permission (文档所在目录)
--    → 是否有直接授予该用户的权限？
--    → 有：使用目录权限
--
-- 4. 查询 kb_folder_permission (文档所在目录)
--    → 是否有授予该用户所属角色的权限？
--    → 有：使用角色目录权限
--
-- 5. 查询父目录权限（向上递归，直到根目录）
--    → 检查 inherit_flag=1 的权限记录
--    → 找到则使用继承的权限
--
-- 6. 无任何权限记录 → 默认无权限（拒绝访问）
--
-- 权限优先级总结:
-- 文档自身权限 > 所在目录权限 > 父目录权限(继承) > 根目录默认权限 > 无权限
--
-- =====================================================
-- 表清单汇总
-- =====================================================
--
-- | 类别       | 表名                    | 数量 |
-- |-----------|-------------------------|-----|
-- | 基础资源   | kb_file, kb_folder, kb_doc | 3   |
-- | 版本管理   | kb_doc_version          | 1   |
-- | 用户交互   | kb_doc_comments, kb_doc_favourite, kb_doc_visit_record, kb_search_record | 4 |
-- | 分类关联   | kb_topic, kb_business_type, kb_doc_topic_type, kb_doc_business_type | 4 |
-- | 权限管理   | kb_folder_permission, kb_doc_permission | 2 |
-- | 系统管理   | kb_sys_config, kb_sys_db_backup | 2 |
-- | 合计       |                         | 16  |
--
-- =====================================================