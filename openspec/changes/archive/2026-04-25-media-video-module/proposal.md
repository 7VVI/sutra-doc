## Why

当前系统已有知识库(KB)模块，但缺少专门的视频学习(慕课)能力。企业内部有大量教学视频需要统一管理、分类、授权和播放。需要一个独立的视频媒体模块，支持视频上传、分类检索、权限控制、流式播放(Range协议)、互动(点赞/观看/分享)和学习进度跟踪。

## What Changes

- 新增 `ruoyi-modules/ruoyi-media` Maven 子模块，作为独立的视频媒体模块
- 基于 `docs/media_module_design.sql` 的 7 张表设计，创建对应的 Entity/Mapper/Service/Controller
- 视频文件存储复用现有 MinIO/OSS 基础设施，支持降级到本地存储
- 视频播放采用 HTTP Range 协议实现流式渐进播放，支持拖动进度条
- 视频权限控制：公开 / 组织可见 / 指定用户，三种模式
- 互动功能：点赞/取消点赞、观看记录、分享链接
- 学习记录：观看时长、进度百分比、断点续播位置
- 分类标签统一管理：通过 tag_type 区分"分类"和"方向"
- 视频附件管理：支持上传和下载关联附件

## Capabilities

### New Capabilities

- `video-crud`: 视频的增删改查，包括上传视频文件、缩略图、附件管理
- `video-streaming`: 基于 HTTP Range 协议的视频流式播放，支持断点续播
- `video-auth`: 视频权限控制（公开/组织/用户三级授权），含过期时间管理
- `video-interaction`: 视频互动功能（点赞/观看记录/分享链接）
- `video-learning`: 学习进度跟踪（时长/进度/完成状态/断点位置）
- `media-tag`: 分类标签管理（分类/方向统一模型，支持树形结构）

### Modified Capabilities

（无已有 capability 需要修改）

## Impact

- **新增 Maven 模块**: `ruoyi-modules/ruoyi-media`，需在父 pom 中注册
- **数据库**: 执行 `docs/media_module_design.sql` 新建 7 张表
- **文件存储**: 复用现有 MinIO/OSS 配置和上传能力
- **API 路径**: 所有接口以 `/media/` 开头
- **依赖关系**: 依赖 `ruoyi-common` (通用工具/基础类) 和系统模块的用户/部门服务
