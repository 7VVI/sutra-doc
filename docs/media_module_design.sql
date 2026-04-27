-- =====================================================
-- 视频媒体模块 - 数据库设计
-- 基于 jeecg-boot media 模块逆向分析，重新设计
-- =====================================================
-- 设计思路:
--   1. 统一分类维度: 分类和方向合并为 media_tag，通过 tag_type 区分
--   2. 统一授权模型: 组织授权和用户授权合并为 media_video_auth
--   3. 统一互动记录: 点赞/观看/分享合并为 media_video_action
--   4. 视频权限通过 auth_type 字段控制整体策略:
--      0=公开, 1=组织可见, 2=指定用户
-- =====================================================


-- ---------------------------------------------------
-- 1. 分类标签表 (统一管理"分类"和"方向")
--    tag_type: 1=分类, 2=方向
--    支持树形结构 (parent_id)
-- ---------------------------------------------------
DROP TABLE IF EXISTS media_tag;
CREATE TABLE media_tag (
    `tag_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `tag_name`    VARCHAR(100) NOT NULL COMMENT '标签名称',
    `tag_type`    TINYINT      NOT NULL COMMENT '标签类型: 1=分类, 2=方向',
    `parent_id`   BIGINT       DEFAULT 0 COMMENT '父标签ID, 0=顶级',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序序号(升序)',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 1-启用, 0-停用',
    `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   BIGINT       DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    VARCHAR(1)   DEFAULT '0' COMMENT '删除标志: 0-正常, 2-删除',
    PRIMARY KEY (tag_id)
) COMMENT = '分类标签表(分类/方向)';

CREATE INDEX idx_media_tag_type ON media_tag(tag_type, status, del_flag);
CREATE INDEX idx_media_tag_parent ON media_tag(parent_id);


-- ---------------------------------------------------
-- 2. 视频主表
-- ---------------------------------------------------
DROP TABLE IF EXISTS media_video;
CREATE TABLE media_video (
    `video_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '视频ID',
    `title`        VARCHAR(255) NOT NULL COMMENT '视频标题',
    `description`  TEXT         DEFAULT NULL COMMENT '视频描述',
    `author_id`    BIGINT       NOT NULL COMMENT '作者/上传人ID',
    `file_type`    VARCHAR(20)  DEFAULT NULL COMMENT '文件格式: mp4, avi, mov等',
    `file_size`    BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
    `duration`     INT          DEFAULT 0 COMMENT '视频时长(秒)',
    `video_path`   VARCHAR(500) NOT NULL COMMENT '视频文件存储路径',
    `thumbnail`    VARCHAR(500) DEFAULT NULL COMMENT '封面缩略图存储路径',
    `view_count`   BIGINT       DEFAULT 0 COMMENT '观看次数',
    `like_count`   BIGINT       DEFAULT 0 COMMENT '点赞数',
    `auth_type`    TINYINT      DEFAULT 0 COMMENT '权限类型: 0-公开, 1-组织可见, 2-指定用户',
    `status`       TINYINT      DEFAULT 1 COMMENT '状态: 1-正常, 0-停用',
    `create_by`    BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    BIGINT       DEFAULT NULL COMMENT '更新人',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`     VARCHAR(1)   DEFAULT '0' COMMENT '删除标志: 0-正常, 2-删除',
    PRIMARY KEY (video_id)
) COMMENT = '视频主表';

CREATE INDEX idx_media_video_author ON media_video(author_id, del_flag);
CREATE INDEX idx_media_video_auth_type ON media_video(auth_type, del_flag);
CREATE INDEX idx_media_video_time ON media_video(del_flag, create_time DESC);
CREATE INDEX idx_media_video_like ON media_video(del_flag, like_count DESC);


-- ---------------------------------------------------
-- 3. 视频-标签关联表 (多对多)
--    一个视频可属于多个分类和多个方向
-- ---------------------------------------------------
DROP TABLE IF EXISTS media_video_tag;
CREATE TABLE media_video_tag (
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `tag_id`   BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (video_id, tag_id)
) COMMENT = '视频-标签关联表';

CREATE INDEX idx_media_video_tag_tag ON media_video_tag(tag_id);


-- ---------------------------------------------------
-- 4. 视频授权表 (统一组织授权和用户授权)
--    auth_target_type: 1=组织, 2=用户
-- ---------------------------------------------------
DROP TABLE IF EXISTS media_video_auth;
CREATE TABLE media_video_auth (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `video_id`         BIGINT       NOT NULL COMMENT '视频ID',
    `auth_target_type` TINYINT      NOT NULL COMMENT '授权目标类型: 1-组织, 2-用户',
    `auth_target_id`   BIGINT       NOT NULL COMMENT '授权目标ID(组织ID或用户ID)',
    `auth_target_name` VARCHAR(200) DEFAULT NULL COMMENT '目标名称快照',
    `expire_time`      DATETIME     DEFAULT NULL COMMENT '过期时间, NULL=永久有效',
    `create_by`        BIGINT       DEFAULT NULL COMMENT '授权人',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `del_flag`         VARCHAR(1)   DEFAULT '0' COMMENT '删除标志: 0-正常, 2-删除',
    PRIMARY KEY (id)
) COMMENT = '视频授权表';

CREATE UNIQUE INDEX uk_media_video_auth ON media_video_auth(video_id, auth_target_type, auth_target_id, del_flag);
CREATE INDEX idx_media_video_auth_target ON media_video_auth(auth_target_type, auth_target_id, del_flag);
CREATE INDEX idx_media_video_auth_expire ON media_video_auth(expire_time);


-- ---------------------------------------------------
-- 5. 视频互动记录表 (点赞/观看/分享，统一记录)
--    action_type: 1=点赞, 2=观看, 3=分享
--    对于观看: extra 存 JSON {"duration":120,"progress":75,"position":45,"completed":true}
-- ---------------------------------------------------
DROP TABLE IF EXISTS media_video_action;
CREATE TABLE media_video_action (
    `id`          BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `video_id`    BIGINT     NOT NULL COMMENT '视频ID',
    `user_id`     BIGINT     NOT NULL COMMENT '操作用户ID',
    `action_type` TINYINT    NOT NULL COMMENT '操作类型: 1-点赞, 2-观看, 3-分享',
    `extra`       JSON       DEFAULT NULL COMMENT '扩展信息(JSON, 观看时存进度/时长等)',
    `create_time` DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '首次操作时间',
    `update_time` DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `del_flag`    VARCHAR(1) DEFAULT '0' COMMENT '删除标志: 0-正常, 2-删除',
    PRIMARY KEY (id)
) COMMENT = '视频互动记录表';

CREATE UNIQUE INDEX uk_media_video_action ON media_video_action(video_id, user_id, action_type, del_flag);
CREATE INDEX idx_media_video_action_user ON media_video_action(user_id, action_type);
CREATE INDEX idx_media_video_action_type ON media_video_action(action_type, create_time DESC);


-- ---------------------------------------------------
-- 6. 视频附件表
-- ---------------------------------------------------
DROP TABLE IF EXISTS media_video_attachment;
CREATE TABLE media_video_attachment (
    `attachment_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '附件ID',
    `video_id`      BIGINT       NOT NULL COMMENT '关联视频ID',
    `file_name`     VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path`     VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size`     BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
    `file_format`   VARCHAR(20)  DEFAULT NULL COMMENT '文件格式: pdf, srt, doc等',
    `create_by`     BIGINT       DEFAULT NULL COMMENT '上传人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_by`     BIGINT       DEFAULT NULL COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`      VARCHAR(1)   DEFAULT '0' COMMENT '删除标志: 0-正常, 2-删除',
    PRIMARY KEY (attachment_id)
) COMMENT = '视频附件表';

CREATE INDEX idx_media_video_attachment_video ON media_video_attachment(video_id, del_flag);


-- ---------------------------------------------------
-- 7. 视频分享链接表
--    独立于互动记录，因为分享链接有独立的生命周期管理
-- ---------------------------------------------------
DROP TABLE IF EXISTS media_video_share;
CREATE TABLE media_video_share (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `video_id`     BIGINT      NOT NULL COMMENT '视频ID',
    `share_token`  VARCHAR(64) NOT NULL COMMENT '分享令牌(唯一)',
    `expire_time`  DATETIME    DEFAULT NULL COMMENT '过期时间, NULL=永久有效',
    `access_count` INT         DEFAULT 0 COMMENT '已访问次数',
    `max_access`   INT         DEFAULT 0 COMMENT '最大访问次数, 0=不限',
    `create_by`    BIGINT      DEFAULT NULL COMMENT '分享人',
    `create_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `del_flag`     VARCHAR(1)  DEFAULT '0' COMMENT '删除标志: 0-正常, 2-删除',
    PRIMARY KEY (id)
) COMMENT = '视频分享链接表';

CREATE UNIQUE INDEX uk_media_video_share_token ON media_video_share(share_token);
CREATE INDEX idx_media_video_share_video ON media_video_share(video_id);
CREATE INDEX idx_media_video_share_create_by ON media_video_share(create_by);
