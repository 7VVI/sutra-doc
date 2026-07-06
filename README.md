## Sutra-Doc 企业知识库与视频学习平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.12-blue.svg)]()
[![JDK-17](https://img.shields.io/badge/JDK-17-green.svg)]()
[![RuoYi-Vue-Plus](https://img.shields.io/badge/RuoYi_Vue_Plus-5.6.0-success.svg)](https://gitee.com/dromara/RuoYi-Vue-Plus)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-yellow.svg)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg)]()

---

> 基于 RuoYi-Vue-Plus 构建的企业级知识库管理与视频学习平台，集文档管理、全文检索、AI 智能问答、视频课程于一体。

## 平台简介

Sutra-Doc 是一套面向企业的知识管理与在线学习系统，包含两大核心业务模块：

- **藏经阁（知识库）** — 企业文档的集中管理、全文检索与 AI 智能问答
- **讲武堂（视频平台）** — 视频课程的上传、分类、播放与权限管控

前端采用 [ruoyi-plus-vben5](https://github.com/imdap/ruoyi-plus-vben5)（Vue3 + Vben5 + Ant Design Vue），同时提供面向管理后台与面向普通用户的门户页面。

## 技术栈

| 类别 | 技术选型 |
|---|---|
| 基础框架 | RuoYi-Vue-Plus 5.6.0 (Spring Boot 3.5.12) |
| 前端框架 | Vue3 + TypeScript + Vben5 + Ant Design Vue |
| ORM | MyBatis-Plus |
| 权限认证 | Sa-Token + JWT |
| 全文检索 | Elasticsearch 8.x + IK 中文分词 |
| AI 集成 | LangChain4j + 智谱 AI（SSE 流式对话） |
| 文档解析 | Apache Tika（PDF/Office/TXT 等） |
| 文件存储 | MinIO (OSS) + 本地存储双模式，SHA256 去重 |
| 缓存 | Redis + Redisson |
| 数据库 | MySQL |

## 项目结构

```
RuoYi-Vue-Plus/
├── ruoyi-admin/               # 管理后台启动模块
├── ruoyi-modules/
│   ├── ruoyi-doc/             # 知识库模块（藏经阁）
│   ├── ruoyi-media/           # 视频媒体模块（讲武堂）
│   ├── ruoyi-system/          # 系统管理
│   └── ...
└── ruoyi-common/              # 公共模块
```

---

## 藏经阁 — 知识库模块 (`ruoyi-doc`)

知识库模块提供企业文档全生命周期管理，涵盖文件上传、在线预览、全文检索、AI 问答、权限管控、版本管理等完整能力。

### 功能清单

| 功能 | 说明 |
|---|---|
| 文档管理 | 上传、编辑、发布/撤回、归档，状态流转完整（草稿→已发布→已撤回→已归档） |
| 全文检索 | Elasticsearch + IK 分词，支持关键词高亮、高级多条件搜索、热门搜索词、搜索建议、相似文档推荐 |
| AI 智能问答 | 接入智谱 AI，基于 LangChain4j 实现 SSE 流式对话，可对文档内容进行智能问答 |
| 目录管理 | 多层级文件夹树形结构，支持懒加载、部门文档树、目录级统计 |
| 在线预览 | PDF、图片、文本文件直接预览；Office 文档（doc/xls/ppt）通过转换服务预览 |
| 文件监控 | 基于快照对比的文件系统监控，支持自动发现新增/修改/删除/重命名文件并同步入库 |
| 权限体系 | 文件夹与文档级别的细粒度权限（查看/下载/编辑/删除/上传/管理），支持权限继承 |
| 版本控制 | 文档多版本管理，支持版本回滚 |
| 文档评论 | 树形评论/回复，支持点赞 |
| 文档收藏 | 用户收藏与取消收藏 |
| 访问日志 | 记录上传/预览/下载/编辑等操作日志，支持 Excel 导出 |
| 下载限流 | 基于 Redis 的每日下载限额（默认 50 次/天），超限返回 429 |
| 统计面板 | 文件总量、周增长、活跃部门、搜索次数，支持环比计算 |
| 文件去重 | SHA256 哈希校验，重复文件不重复存储 |
| 双存储模式 | OSS（MinIO）优先，失败自动降级到本地存储 |
| 多租户 | 全链路租户隔离，ES 搜索、下载限流均按租户独立统计 |

### 文档解析流水线

```
文件上传 → 异步解析（Apache Tika）→ 提取文本内容 → 构建 ES 文档 → 索引到 Elasticsearch
                                                                          ↓
                                                                    全文检索可用
```

- 上传后异步处理，不阻塞响应
- 支持格式：PDF、DOC/DOCX、XLS/XLSX、PPT/PPTX、TXT、RTF、HTML、XML、ODT/ODS/ODP
- 全文索引状态实时追踪（未索引 → 处理中 → 已完成 / 失败）

---

## 讲武堂 — 视频媒体模块 (`ruoyi-media`)

视频模块提供企业内部的视频课程管理与学习平台，支持视频上传、分类管理、流式播放、权限控制等。

### 功能清单

| 功能 | 说明 |
|---|---|
| 视频管理 | 上传、编辑（标题/描述/作者/封面/标签）、删除、状态管理（待审核/已发布/已下架） |
| 流式播放 | HTTP Range 字节流式传输，支持拖拽进度条 seek，自动记录播放次数 |
| 分类标签 | 层级标签树，支持「分类」和「方向」两种标签类型 |
| 访问控制 | 公开视频 / 限制访问视频，可按用户或部门授权，支持过期时间 |
| 点赞互动 | 用户点赞/取消点赞，防重复，原子计数 |
| 分享链接 | 生成带 Token 的限时分享链接，可设置过期时间和最大访问次数 |
| 附件管理 | 视频可关联附件文件（PDF、文档等），支持上传和下载 |
| 视频检索 | 支持关键词、作者、分类、方向等多条件筛选与排序 |

### 视频播放架构

```
客户端请求 → 鉴权检查（公开/授权）→ Range 解析 → OSS 读取字节流 → 206 Partial Content 响应
                                                                    ↓
                                                          播放器拖拽进度条 seek
```

---

## 前端门户页面

除管理后台外，系统提供面向普通用户的门户页面：

| 页面 | 说明 |
|---|---|
| 主页 | 门户首页，集成藏经阁、讲武堂、论道台三大入口 |
| 藏经阁 | 部门文档统计、文档检索与 AI 智能问答双模式切换 |
| 讲武堂 | 视频课程列表，支持分类/方向/年份/讲师筛选，内嵌 Video.js 播放器 |
| 论道台 | 主题讨论区，展示话题卡片与关联文档 |
| AI 助手 | 基于 SSE 流式响应的 AI 对话页面，支持多轮会话、Markdown 渲染、快捷提问 |

---

## 演示图例

|  |  |
|---|---|
| ![主页](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobmain_page.png "主页") | ![论道台](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobtopic_page.png "论道台") |
| ![视频页面](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobvideo_page.png "视频页面") | ![AI智能助手](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobai_agent.png "AI智能助手") |
| ![藏经阁](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobfolder.png "藏经阁") | ![文档中心](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobdoc_center.png "文档中心") |
| ![文档日志](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobdoc_log.png "文档日志") | ![文档关注](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobdoc_watch.png "文档关注") |
| ![视频管理](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobvideo.png "视频管理") | ![视频播放](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/hmoobpaly_video.png "视频播放") |

---

## 基于 RuoYi-Vue-Plus

本项目基于 [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus) 5.6.0 构建，继承其基础能力：

- 用户/角色/部门/岗位/菜单/字典/参数/通知公告等系统管理
- Sa-Token 权限认证（登录/角色/权限/二级认证校验）
- 多租户支持
- MyBatis-Plus ORM + 分页 + 数据权限
- Redisson 分布式锁 + 缓存
- MinIO / S3 文件存储
- SpringDoc 接口文档
- FastExcel 导入导出
- Docker 部署编排

参考文档：[https://plus-doc.dromara.org](https://plus-doc.dromara.org)
