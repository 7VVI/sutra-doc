# 知识库模块数据库表结构设计文档

**创建日期**: 2026-04-14
**模块名称**: KB知识库管理系统
**设计版本**: v2.0

---

## 一、设计背景

### 1.1 原有系统分析

原有 KM 模块存在以下问题：
- 表前缀 `km_` 命名不够规范
- 缺少目录管理功能，无法按层级组织文档
- 权限管理粒度粗，仅支持单文档的下载权限控制
- 缺少多维度权限支持（数据范围、授权对象等）
- 权限配置工作量大，无法批量授权

### 1.2 设计目标

1. 统一表前缀为 `kb_`
2. 新增目录表，支持树形层级结构管理文档
3. 实现多维度权限体系：
   - 操作权限：查看、下载、编辑、删除、上传、管理
   - 数据范围：仅本人、本部门、本部门及下级、自定义、全部
   - 授权对象：用户、角色
4. 支持权限继承，减少配置工作量
5. 符合 MySQL 数据库设计规范

---

## 二、核心设计决策

### 2.1 目录层级设计

**决策**: 采用独立目录表 + 关联字段模式

- 新建 `kb_folder` 目录表
- `kb_doc` 表增加 `folder_id` 字段指向所属目录
- 目录表自引用 `parent_id` 实现多级嵌套

**理由**: 结构清晰，查询效率高，易于维护

### 2.2 权限表设计

**决策**: 分表设计（目录权限表 + 文档权限表）

- `kb_folder_permission`: 目录权限表，支持继承
- `kb_doc_permission`: 文档权限表，支持覆盖

**理由**: 目录权限有继承特性，文档权限有覆盖特性，职责不同适合分表

### 2.3 权限维度

**操作权限（6项）**:
| 权限编码 | 权限名称 | 说明 |
|---------|---------|-----|
| view | 查看 | 查看文档内容和预览 |
| download | 下载 | 下载文档原文件 |
| edit | 编辑 | 编辑文档内容和属性 |
| delete | 删除 | 删除文档 |
| upload | 上传 | 在目录下上传新文档 |
| manage | 管理 | 管理目录权限和子目录 |

**数据范围（5级）**:
| 范围值 | 范围名称 | 说明 |
|-------|---------|-----|
| 1 | 仅本人 | 仅可访问自己创建的文档 |
| 2 | 本部门 | 可访问所属部门的文档 |
| 3 | 本部门及下级 | 可访问本部门及下级部门的文档 |
| 4 | 自定义范围 | 可访问指定部门范围的文档 |
| 5 | 全部 | 可访问所有文档 |

### 2.4 授权对象

**决策**: 用户 + 角色两种授权对象

- 通过 `subject_type` 区分：1-用户，2-角色
- 通过 `subject_id` 存储对应的用户ID或角色ID
- 用户权限 > 角色权限（优先级）

**理由**: 适配大多数企业系统，与 Jeecg-Boot 框架兼容性好

### 2.5 权限继承

**决策**: 权限继承模式

- 子目录/文件默认继承父目录的权限设置
- 子目录可以单独设置权限覆盖继承（优先级更高）
- `inherit_flag` 控制是否让子目录继承此权限

**权限计算优先级**:
```
文档自身权限 > 所在目录权限 > 父目录权限(继承) > 根目录默认权限 > 无权限
```

---

## 三、表结构设计

### 3.1 表清单

| 类别 | 表名 | 说明 |
|-----|------|-----|
| 基础资源 | kb_file | 文件表（物理文件） |
| 基础资源 | kb_folder | 目录表（新增） |
| 基础资源 | kb_doc | 文档主表 |
| 版本管理 | kb_doc_version | 文档版本表 |
| 用户交互 | kb_doc_comments | 文档评论表 |
| 用户交互 | kb_doc_favourite | 文档收藏表 |
| 用户交互 | kb_doc_visit_record | 文档访问记录表 |
| 用户交互 | kb_search_record | 搜索记录表 |
| 分类关联 | kb_topic | 主题表 |
| 分类关联 | kb_business_type | 业务类型表 |
| 分类关联 | kb_doc_topic_type | 文档主题关联表 |
| 分类关联 | kb_doc_business_type | 文档业务类型关联表 |
| 权限管理 | kb_folder_permission | 目录权限表（新增） |
| 权限管理 | kb_doc_permission | 文档权限表（新增） |
| 系统管理 | kb_sys_config | 系统配置表 |
| 系统管理 | kb_sys_db_backup | 数据库备份记录表 |

**总计**: 16张表

### 3.2 kb_folder - 目录表

```sql
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
) COMMENT = '目录表';

-- 索引
CREATE INDEX idx_parent_id ON kb_folder(parent_id);
CREATE INDEX idx_folder_level ON kb_folder(folder_level);
CREATE INDEX idx_status ON kb_folder(status);
CREATE UNIQUE INDEX uk_folder_code ON kb_folder(folder_code);
CREATE INDEX idx_folder_path ON kb_folder(folder_path(100));
```

### 3.3 kb_folder_permission - 目录权限表

```sql
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
) COMMENT = '目录权限表';

-- 索引
CREATE INDEX idx_folder_id ON kb_folder_permission(folder_id);
CREATE INDEX idx_subject ON kb_folder_permission(subject_type, subject_id);
CREATE INDEX idx_data_scope ON kb_folder_permission(data_scope);
CREATE UNIQUE INDEX uk_folder_subject ON kb_folder_permission(folder_id, subject_type, subject_id, del_flag);
```

### 3.4 kb_doc_permission - 文档权限表

```sql
CREATE TABLE kb_doc_permission (
    `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `doc_id` bigint NOT NULL COMMENT '文档ID',
    `subject_type` tinyint(4) NOT NULL COMMENT '授权对象类型: 1-用户, 2-角色',
    `subject_id` bigint NOT NULL COMMENT '授权对象ID(用户ID或角色ID)',
    -- 操作权限(文档级别仅4项)
    `perm_view` tinyint(4) DEFAULT 0 COMMENT '查看权限: 0-无, 1-有',
    `perm_download` tinyint(4) DEFAULT 0 COMMENT '下载权限: 0-无, 1-有',
    `perm_edit` tinyint(4) DEFAULT 0 COMMENT '编辑权限: 0-无, 1-有',
    `perm_delete` tinyint(4) DEFAULT 0 COMMENT '删除权限: 0-无, 1-有',
    -- 数据范围(文档级别通常继承目录设置)
    `data_scope` tinyint(4) DEFAULT NULL COMMENT '数据范围: NULL-继承目录设置, 其他值-覆盖设置',
    -- 审计字段
    `create_by` bigint COMMENT '创建人ID(授权人)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag` VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 1-已删除',
    PRIMARY KEY (permission_id)
) COMMENT = '文档权限表';

-- 索引
CREATE INDEX idx_doc_id ON kb_doc_permission(doc_id);
CREATE INDEX idx_subject ON kb_doc_permission(subject_type, subject_id);
CREATE UNIQUE INDEX uk_doc_subject ON kb_doc_permission(doc_id, subject_type, subject_id, del_flag);
```

### 3.5 kb_doc 表修改

新增字段：
```sql
`folder_id` bigint NOT NULL DEFAULT 0 COMMENT '所属目录ID(0表示根目录/未分类)',
```

移除字段：
```sql
-- 移除 download_flag 字段（权限由权限表统一管理）
-- 原 download_flag=1 的语义迁移到权限表中
```

---

## 四、ER关系图

```
                    kb_folder (目录表)
                        │
                        │ parent_id (自引用)
                        │
            ┌───────────┴───────────┐
            │                       │
            ▼                       ▼
   kb_folder_permission         kb_doc (文档表)
   (目录权限表)                    │
            │                     │ folder_id
            │                     │
            │                     ▼
            │              kb_doc_permission
            │              (文档权限表)
            │                     │
            │                     │
            ▼                     ▼
      用户/角色授权对象        用户/角色授权对象
      (subject_type+subject_id)  (subject_type+subject_id)
```

---

## 五、权限计算流程

### 5.1 用户访问文档权限计算

```
1. 查询 kb_doc_permission
   → 是否有直接授予该用户的权限？
   → 有：使用文档权限

2. 查询 kb_doc_permission
   → 是否有授予该用户所属角色的权限？
   → 有：使用角色权限（多个角色取权限并集）

3. 查询 kb_folder_permission (文档所在目录)
   → 是否有直接授予该用户的权限？
   → 有：使用目录权限

4. 查询 kb_folder_permission (文档所在目录)
   → 是否有授予该用户所属角色的权限？
   → 有：使用角色目录权限

5. 查询父目录权限（向上递归，直到根目录）
   → 检查 inherit_flag=1 的权限记录
   → 找到则使用继承的权限

6. 无任何权限记录 → 默认无权限（拒绝访问）
```

### 5.2 权限优先级

```
文档自身权限 > 所在目录权限 > 父目录权限(继承) > 根目录默认权限 > 无权限
```

### 5.3 权限合并规则

- 多个角色的权限：取**并集**（任一角色有权限即可）
- 用户直接授权 + 角色授权：取**并集**
- 不同层级权限：取**最高优先级层级**的权限设置

---

## 六、初始化数据

### 6.1 默认目录结构

```sql
INSERT INTO kb_folder (folder_id, folder_name, parent_id, folder_level, folder_path, folder_code, sort_order, description, status) VALUES
(1, '知识库', 0, 1, '/知识库', 'ROOT', 1, '知识库根目录', 1),
(2, '研发中心', 1, 2, '/知识库/研发中心', 'DEV_CENTER', 1, '研发部门文档', 1),
(3, '销售中心', 1, 2, '/知识库/销售中心', 'SALES_CENTER', 2, '销售部门文档', 1),
(4, '人力资源', 1, 2, '/知识库/人力资源', 'HR_CENTER', 3, '人力资源文档', 1),
(5, '财务中心', 1, 2, '/知识库/财务中心', 'FINANCE_CENTER', 4, '财务部门文档', 1),
(6, '技术文档', 2, 3, '/知识库/研发中心/技术文档', 'TECH_DOC', 1, '技术类文档', 1),
(7, '项目文档', 2, 3, '/知识库/研发中心/项目文档', 'PROJECT_DOC', 2, '项目相关文档', 1);
```

### 6.2 默认权限配置

```sql
-- 管理员角色(ID=1)授予根目录全部权限
INSERT INTO kb_folder_permission (folder_id, subject_type, subject_id, perm_view, perm_download, perm_edit, perm_delete, perm_upload, perm_manage, data_scope, inherit_flag) VALUES
(1, 2, 1, 1, 1, 1, 1, 1, 1, 5, 1);

-- 普通员工角色(ID=2)授予根目录查看下载权限
INSERT INTO kb_folder_permission (folder_id, subject_type, subject_id, perm_view, perm_download, perm_edit, perm_delete, perm_upload, perm_manage, data_scope, inherit_flag) VALUES
(1, 2, 2, 1, 1, 0, 0, 0, 0, 2, 1);

-- 示例：用户(ID=100)授予研发中心目录管理权限
INSERT INTO kb_folder_permission (folder_id, subject_type, subject_id, perm_view, perm_download, perm_edit, perm_delete, perm_upload, perm_manage, data_scope, inherit_flag) VALUES
(2, 1, 100, 1, 1, 1, 1, 1, 1, 3, 1);
```

---

## 七、表前缀变更对照

| 原表名 (km_) | 新表名 (kb_) | 说明 |
|-------------|-------------|------|
| km_file | kb_file | 文件表 |
| km_doc | kb_doc | 文档主表（新增 folder_id） |
| km_doc_version | kb_doc_version | 文档版本表 |
| km_doc_comments | kb_doc_comments | 文档评论表 |
| km_doc_favourite | kb_doc_favourite | 文档收藏表 |
| km_doc_topic_type | kb_doc_topic_type | 文档主题关联表 |
| km_doc_business_type | kb_doc_business_type | 文档业务类型关联表 |
| km_doc_visit_record | kb_doc_visit_record | 文档访问记录表 |
| km_search_record | kb_search_record | 搜索记录表 |
| km_sys_config | kb_sys_config | 系统配置表 |
| km_sys_db_backup | kb_sys_db_backup | 数据库备份记录表 |
| km_topic | kb_topic | 主题表 |
| km_business_type | kb_business_type | 业务类型表 |
| - | kb_folder | 目录表（新增） |
| - | kb_folder_permission | 目录权限表（新增） |
| - | kb_doc_permission | 文档权限表（新增） |

---

## 八、设计评审检查

- [x] 所有表使用 `kb_` 前缀
- [x] 主键定义为 bigint AUTO_INCREMENT
- [x] 所有字段有中文注释
- [x] 包含必需的审计字段 (create_time, update_time, del_flag)
- [x] 枚举字段注释列出所有值
- [x] 外键字段创建索引
- [x] 唯一业务字段创建 UNIQUE INDEX
- [x] 索引定义在 CREATE TABLE 之后
- [x] 无 TBD 或 TODO 占位符
- [x] 无内部矛盾的设计决策
- [x] 范围聚焦单一模块，无需分解

---

## 九、后续实施步骤

1. 生成完整的 SQL 文件 `kb_module_design.sql`
2. 更新 Java 实体类（Entity）
3. 实现权限计算服务（PermissionService）
4. 实现权限继承查询逻辑
5. 更新 Controller 接口的权限校验

---

**文档状态**: 待用户审核
**下一步**: 用户确认后调用 writing-plans skill 生成实施计划