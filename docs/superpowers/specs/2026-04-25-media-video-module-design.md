# 视频媒体模块设计文档

## 概述

新增 `ruoyi-modules/ruoyi-media` 模块，实现企业内部视频学习(慕课)平台。基于 `docs/media_module_design.sql` 的 7 张表设计，支持视频上传、Range 流式播放、三级权限控制、互动功能和学习进度跟踪。

## 项目约定

| 项目 | 约定 |
|------|------|
| 基类 | `TenantEntity` (含 tenantId, createBy, createTime, updateBy, updateTime, createDept 自动填充) |
| Mapper | `BaseMapperPlus<T, V>` (内置 selectVoById, selectVoPage 等 VO 转换方法) |
| 注入方式 | `@RequiredArgsConstructor` + `private final`，禁止 @Autowired |
| 权限框架 | Sa-Token `@SaCheckPermission` |
| 分页 | `PageQuery` + `TableDataInfo<Vo>` |
| 事务 | `TransactionTemplate` 编程式事务，禁止 @Transactional |
| 对象转换 | `MapstructUtils.convert()` |
| 包名 | `com.hmoob.media` |
| 返回值 | `R<T>` 统一响应包装 |

## 模块结构

```
ruoyi-modules/ruoyi-media/
  pom.xml
  src/main/java/com/hmoob/media/
    controller/
      MediaVideoController        -- 视频 CRUD + 播放 + 互动
      MediaTagController          -- 分类标签管理
      MediaVideoAuthController    -- 授权管理
    domain/
      MediaVideo.java             -- 视频主表 (media_video)
      MediaTag.java               -- 标签表 (media_tag)
      MediaVideoTag.java          -- 视频-标签关联 (media_video_tag)
      MediaVideoAuth.java         -- 授权表 (media_video_auth)
      MediaVideoAction.java       -- 互动记录表 (media_video_action)
      MediaVideoAttachment.java   -- 附件表 (media_video_attachment)
      MediaVideoShare.java        -- 分享链接表 (media_video_share)
      bo/                         -- 输入对象 (Bo)
      vo/                         -- 输出对象 (Vo)
    mapper/
      MediaVideoMapper.java
      MediaTagMapper.java
      MediaVideoTagMapper.java
      MediaVideoAuthMapper.java
      MediaVideoActionMapper.java
      MediaVideoAttachmentMapper.java
      MediaVideoShareMapper.java
    service/
      IMediaVideoService.java
      IMediaTagService.java
      IMediaVideoAuthService.java
      IMediaVideoActionService.java
      IMediaVideoAttachmentService.java
      IMediaVideoShareService.java
      impl/
        MediaVideoServiceImpl.java
        MediaTagServiceImpl.java
        MediaVideoAuthServiceImpl.java
        MediaVideoActionServiceImpl.java
        MediaVideoAttachmentServiceImpl.java
        MediaVideoShareServiceImpl.java
```

## 数据库表 (7 张)

1. **media_tag** — 分类标签表，tag_type 区分分类(1)和方向(2)，支持 parent_id 树形结构
2. **media_video** — 视频主表，auth_type 控制权限策略(0=公开/1=组织/2=用户)
3. **media_video_tag** — 视频-标签多对多关联，联合主键
4. **media_video_auth** — 统一授权表，auth_target_type 区分组织(1)和用户(2)
5. **media_video_action** — 互动记录表，action_type 区分点赞(1)/观看(2)/分享(3)，extra JSON 存学习进度
6. **media_video_attachment** — 视频附件表
7. **media_video_share** — 分享链接表，独立生命周期管理

完整建表 SQL 见 `docs/media_module_design.sql`。

## API 设计

### 视频管理 (MediaVideoController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/media/video/upload` | 上传视频+缩略图+关联标签 |
| GET | `/media/video/page` | 分页查询(关键词/分类/方向/排序)，权限过滤 |
| GET | `/media/video/{id}` | 视频详情 |
| DELETE | `/media/video/{id}` | 逻辑删除视频及附件 |
| GET | `/media/video/play/{id}` | Range 流式播放，返回 206 |
| POST | `/media/video/like` | 点赞 |
| POST | `/media/video/unlike` | 取消点赞 |
| POST | `/media/video/share` | 创建分享链接 |
| GET | `/media/video/share/{token}` | 通过分享令牌访问 |
| GET | `/media/video/{id}/watchers` | 观看记录 |
| GET | `/media/video/{id}/progress` | 断点续播位置 |
| GET | `/media/video/learning/mine` | 我的学习记录 |
| GET | `/media/video/learning/statistics` | 学习统计 |
| POST | `/media/video/attachment` | 上传附件 |
| GET | `/media/video/{id}/attachments` | 附件列表 |
| GET | `/media/video/attachment/{id}/download` | 下载附件 |

### 标签管理 (MediaTagController)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/media/tag` | 标签列表，type 参数筛选 |
| GET | `/media/tag/tree` | 树形标签 |
| POST | `/media/tag` | 新增标签 |
| PUT | `/media/tag/{id}` | 修改标签 |
| DELETE | `/media/tag/{id}` | 删除标签 |

### 授权管理 (MediaVideoAuthController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/media/auth` | 新增授权 |
| DELETE | `/media/auth/{id}` | 删除授权 |
| GET | `/media/auth/{videoId}` | 查询视频授权列表(分页) |

## 关键技术方案

### 1. Range 流式播放

```
客户端 → GET /media/video/play/{id}
         Range: bytes=0-1048575

服务端 → 206 Partial Content
         Content-Range: bytes 0-1048575/<fileSize>
         Content-Length: 1048576
         <byte stream from MinIO/OSS>
```

流程：
1. 校验登录用户 + 视频权限
2. 从 OSS 获取对象信息和输入流(指定 offset/length)
3. 设置 206 响应头 + Content-Range
4. 8KB 缓冲区流式输出
5. 播放成功后异步记录观看 + 更新学习进度

### 2. 权限过滤逻辑

视频列表查询时：
```
可见视频 = auth_type=0 (公开)
        ∪ auth_type=1 AND 用户组织在 media_video_auth(auth_target_type=1) 中
        ∪ auth_type=2 AND 用户ID在 media_video_auth(auth_target_type=2) 中且未过期
```

### 3. 互动记录

统一使用 `media_video_action` 表：
- `action_type=1` 点赞：唯一约束 `(video_id, user_id, action_type)`，Redis 缓存加速判断
- `action_type=2` 观看：extra JSON 存 `{duration, progress, last_position, completed}`
- 点赞/取消点赞使用 `synchronized` 用户锁 + `TransactionTemplate` 保证一致性

### 4. 学习进度 extra JSON 结构

```json
{
  "duration": 1200,
  "progress": 75.5,
  "last_position": 906,
  "completed": false
}
```

### 5. 文件存储

复用 `ruoyi-common-oss` 模块，`video_path`、`thumbnail`、`file_path` 字段均存储 OSS 对象 key。获取预览/下载 URL 时通过 OssClient 动态生成。

## 依赖关系

```xml
ruoyi-common-core        -- 通用工具, R, BaseEntity
ruoyi-common-mybatis     -- BaseMapperPlus, PageQuery, TableDataInfo
ruoyi-common-security    -- Sa-Token, LoginHelper
ruoyi-common-tenant      -- TenantEntity
ruoyi-common-web         -- BaseController
ruoyi-common-redis       -- RedisTemplate
ruoyi-common-oss         -- OssClient 文件上传下载
ruoyi-common-log         -- 操作日志注解
ruoyi-common-idempotent  -- 防重复提交
```

## 非目标

- 视频转码/压缩（上传前处理）
- 弹幕功能
- 视频评论系统
- 前端页面
- 视频推荐算法
