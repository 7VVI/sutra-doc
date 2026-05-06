## Context

RuoYi-Vue-Plus 项目目前有 `ruoyi-modules/ruoyi-doc` 知识库模块，但缺少视频学习(慕课)模块。本次新增 `ruoyi-modules/ruoyi-media` 模块，基于 `docs/media_module_design.sql` 的 7 张表设计，实现完整的视频学习平台。

现有项目约定：
- 包名 `com.hmoob`，非 `org.dromara`
- 使用 `@RequiredArgsConstructor` + `private final` 构造器注入
- 使用 `TransactionTemplate` 编程式事务，禁止 `@Transactional`
- 使用 `MapstructUtils.convert()` 进行对象转换
- 分层：Controller → Service → Mapper，Controller 禁止注入 Mapper
- 文件存储：支持 MinIO/OSS，通过配置降级到本地存储

## Goals / Non-Goals

**Goals:**
- 创建独立的 `ruoyi-media` Maven 子模块，遵循项目现有分层架构
- 实现视频上传、流式播放(Range协议)、分类检索、权限控制
- 实现点赞/观看/分享互动功能和视频附件管理
- 实现学习进度跟踪（时长/进度/断点续播）
- 视频权限支持三级：公开、组织可见、指定用户
- 分类标签统一管理（分类/方向通过 tag_type 区分）

**Non-Goals:**
- 不做视频转码/压缩（由上传前处理）
- 不做弹幕功能
- 不做视频评论系统
- 不做前端页面开发（仅后端 API）
- 不做视频推荐算法

## Decisions

### 1. 模块结构

**决策**: 新建 `ruoyi-modules/ruoyi-media`，包名 `com.hmoob.media`

**理由**: 与现有 `ruoyi-doc` 模块平级，职责清晰。内部按 domain/controller/service/mapper 分层，domain 下分 bo/vo 子包。

**目录结构**:
```
ruoyi-modules/ruoyi-media/
  src/main/java/com/hmoob/media/
    controller/     -- MediaVideoController, MediaTagController, MediaVideoAuthController
    domain/
      MediaVideo.java, MediaTag.java, MediaVideoTag.java,
      MediaVideoAuth.java, MediaVideoAction.java,
      MediaVideoAttachment.java, MediaVideoShare.java
      bo/   -- Bo 类(接收前端参数)
      vo/   -- Vo 类(返回前端数据)
    mapper/
    service/
      impl/
    config/         -- MediaStorageConfig (文件存储配置)
  src/main/resources/
    mapper/         -- MyBatis XML(如需要复杂查询)
```

### 2. 视频流式播放方案

**决策**: 使用 HTTP Range 协议，后端从 MinIO/OSS/本地存储读取指定范围字节流返回

**理由**: 原系统已验证此方案可行性（参考 `MediaVideosServiceImpl.openVideo`）。浏览器原生支持 Range 请求，无需额外播放器组件。

**实现要点**:
- 解析 `Range: bytes=start-end` 请求头
- 返回 `206 Partial Content` + `Content-Range` 响应头
- 支持视频拖动（随机位置播放）
- 播放前校验用户权限（公开/组织/用户级）
- 播放成功后异步记录观看和学习进度

### 3. 文件存储复用

**决策**: 复用现有 OSS/MinIO 上传能力，通过 `OssClient` 统一上传，`video_path` 和 `thumbnail` 字段存储对象 key

**理由**: 项目已有成熟的文件上传基础设施（`ruoyi-common-oss`），不需要重新造轮子。

### 4. 互动记录表设计

**决策**: 点赞/观看/分享统一到 `media_video_action` 一张表，通过 `action_type` 区分，观看进度用 JSON `extra` 字段

**替代方案**: 每种互动独立一张表（原系统方案）
**理由**: 减少表数量，查询模式统一（按 video_id + user_id），扩展新的互动类型只需加 action_type 值。观看进度的 JSON 字段灵活且不影响索引。

### 5. 权限模型设计

**决策**: 视频表 `auth_type` 字段控制策略（0=公开/1=组织/2=用户），授权细节在 `media_video_auth` 表

**理由**: 公开视频不需要查授权表，直接通过 auth_type=0 过滤。只有非公开视频才需要关联查授权表，查询效率高。

### 6. 分类标签统一

**决策**: 分类和方向合并为 `media_tag` 表，通过 `tag_type` 区分

**替代方案**: 分类和方向各一张表（原系统方案）
**理由**: 数据结构完全一致（name + description + sort），合并后减少表数量和重复代码，支持树形结构扩展。

## Risks / Trade-offs

- **[大文件上传]** 视频文件可能很大 → 限制单文件大小上限(如 500MB)，后续可扩展分片上传
- **[并发点赞]** 同一用户并发点赞可能导致计数不一致 → 使用 Redis 缓存 + 数据库 `like_count = like_count + 1` 原子更新
- **[Range 请求性能]** 高并发视频播放对存储系统压力大 → 利用浏览器缓存头 + MinIO 自带的流式传输能力
- **[JSON extra 字段]** 观看进度的 JSON 字段不方便 SQL 查询统计 → 如需统计报表，后续可抽独立字段或物化视图
