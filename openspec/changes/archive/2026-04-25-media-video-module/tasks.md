## 1. 模块骨架搭建

- [ ] 1.1 创建 ruoyi-modules/ruoyi-media Maven 子模块，配置 pom.xml 依赖（ruoyi-common、ruoyi-common-oss、ruoyi-common-mybatis 等）
- [ ] 1.2 在父 pom.xml 中注册 ruoyi-media 模块
- [ ] 1.3 创建包结构：controller / domain / domain.bo / domain.vo / mapper / service / service.impl
- [ ] 1.4 执行 docs/media_module_design.sql 建表

## 2. 实体与 Mapper 层

- [ ] 2.1 创建 7 个 Entity 类：MediaVideo、MediaTag、MediaVideoTag、MediaVideoAuth、MediaVideoAction、MediaVideoAttachment、MediaVideoShare
- [ ] 2.2 创建对应的 7 个 Mapper 接口
- [ ] 2.3 如有复杂查询（视频分页+权限过滤），编写对应 Mapper XML

## 3. 分类标签管理（media-tag）

- [ ] 3.1 创建 MediaTagBo / MediaTagVo 转换对象
- [ ] 3.2 实现 MediaTagService / MediaTagServiceImpl：增删改查、树形结构
- [ ] 3.3 实现 MediaTagController：GET /media/tag、POST /media/tag、PUT /media/tag/{id}、DELETE /media/tag/{id}

## 4. 视频核心 CRUD（video-crud）

- [ ] 4.1 创建 MediaVideoBo / MediaVideoVo / MediaVideoQuery 等转换对象和查询参数类
- [ ] 4.2 创建 MediaVideoAttachmentBo / MediaVideoAttachmentVo
- [ ] 4.3 实现 MediaVideoService / MediaVideoServiceImpl：上传、分页查询、详情、删除
- [ ] 4.4 实现 MediaVideoAttachmentService：附件上传、下载、列表查询
- [ ] 4.5 实现 MediaVideoController：POST /media/video/upload、GET /media/video/page、GET /media/video/{id}、DELETE /media/video/{id}、附件相关接口
- [ ] 4.6 视频上传事务：保存视频主记录 + 建立 video_tag 关联（使用 TransactionTemplate）

## 5. 视频流式播放（video-streaming）

- [ ] 5.1 实现视频流播放接口 GET /media/video/play/{videoId}，支持 Range 请求头解析
- [ ] 5.2 实现 206 Partial Content 响应，从对象存储获取指定范围字节流
- [ ] 5.3 播放前权限校验逻辑集成（公开/组织/用户三级）
- [ ] 5.4 播放成功后自动记录观看（view_count +1，创建或更新 action_type=2 记录）

## 6. 视频权限控制（video-auth）

- [ ] 6.1 创建 MediaVideoAuthBo / MediaVideoAuthVo
- [ ] 6.2 实现 MediaVideoAuthService：新增授权（组织/用户）、删除授权、查询授权列表
- [ ] 6.3 实现 MediaVideoAuthController：POST /media/auth、DELETE /media/auth/{id}、GET /media/auth/{videoId}
- [ ] 6.4 实现权限过滤逻辑：视频分页查询中根据当前用户组织/授权过滤可见视频
- [ ] 6.5 实现过期授权清理能力

## 7. 视频互动（video-interaction）

- [ ] 7.1 创建 MediaVideoActionBo（点赞/取消点赞请求）
- [ ] 7.2 实现点赞功能：POST /media/video/like（Redis 缓存 + like_count 原子更新 + 防重复）
- [ ] 7.3 实现取消点赞：POST /media/video/unlike
- [ ] 7.4 实现分享功能：POST /media/video/share（生成 share_token）、GET /media/video/share/{token}
- [ ] 7.5 实现观看记录查询：GET /media/video/{videoId}/watchers

## 8. 学习进度跟踪（video-learning）

- [ ] 8.1 定义学习进度 JSON 结构：duration、progress、last_position、completed
- [ ] 8.2 实现学习进度自动记录/更新逻辑（与播放接口集成）
- [ ] 8.3 实现断点续播接口：GET /media/video/{videoId}/progress
- [ ] 8.4 实现学习记录查询：GET /media/video/learning/mine、GET /media/video/learning/statistics
