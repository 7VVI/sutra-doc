## ADDED Requirements

### Requirement: 视频上传
系统 SHALL 提供视频上传接口，接收视频文件、缩略图、标题、作者信息、分类ID和方向ID。视频文件和缩略图 MUST 上传到对象存储(MinIO/OSS)，返回存储 key 后写入数据库。

#### Scenario: 上传视频成功
- **WHEN** 用户通过 POST /media/video/upload 提交视频文件、缩略图、标题、作者、分类和方向
- **THEN** 系统将视频文件和缩略图上传到对象存储，创建 media_video 记录，并在 media_video_tag 中建立关联，返回视频ID

#### Scenario: 上传视频缺少必填字段
- **WHEN** 用户提交上传请求但缺少视频文件或标题
- **THEN** 系统返回参数校验错误提示

### Requirement: 视频分页查询
系统 SHALL 提供视频分页查询接口，支持按关键词(标题/作者名)、分类、方向、排序方式(最新/最热)进行筛选。查询结果 MUST 包含作者姓名、缩略图预览URL、点赞用户头像列表、当前用户是否已点赞、附件数量、授权组织信息。

#### Scenario: 按最新排序查询
- **WHEN** 用户请求 GET /media/video/page，sort=newest
- **THEN** 返回按创建时间倒序的视频分页列表

#### Scenario: 按分类和方向联合筛选
- **WHEN** 用户请求 GET /media/video/page，category=1,2&direction=3
- **THEN** 返回同时满足分类和方向条件的视频列表

#### Scenario: 按关键词搜索
- **WHEN** 用户请求 GET /media/video/page，keyword=SpringBoot
- **THEN** 返回标题包含关键词或作者名包含关键词的视频列表

### Requirement: 视频详情查询
系统 SHALL 提供视频详情查询接口，返回视频完整信息，包含作者名称、缩略图预览URL、点赞用户头像列表。

#### Scenario: 查询存在的视频
- **WHEN** 用户请求 GET /media/video/{videoId}
- **THEN** 返回该视频的完整信息

#### Scenario: 查询不存在的视频
- **WHEN** 用户请求 GET /media/video/{videoId}，videoId 不存在
- **THEN** 返回 404 错误

### Requirement: 视频删除
系统 SHALL 提供视频逻辑删除功能。删除视频 MUST 同时逻辑删除关联的附件记录。

#### Scenario: 删除视频
- **WHEN** 管理员请求 DELETE /media/video/{videoId}
- **THEN** 视频 del_flag 设为 '2'，关联附件 del_flag 也设为 '2'

### Requirement: 视频附件上传
系统 SHALL 支持为已上传的视频添加附件文件。

#### Scenario: 上传附件成功
- **WHEN** 用户通过 POST /media/video/attachment 上传文件并指定 videoId
- **THEN** 文件上传到对象存储，在 media_video_attachment 表创建记录

### Requirement: 视频附件下载
系统 SHALL 提供视频附件的下载接口。

#### Scenario: 下载附件
- **WHEN** 用户请求 GET /media/video/attachment/{attachmentId}/download
- **THEN** 系统从对象存储获取文件并以流方式返回给客户端

### Requirement: 视频附件列表查询
系统 SHALL 提供根据视频ID查询其所有附件列表的接口。

#### Scenario: 查询视频附件
- **WHEN** 用户请求 GET /media/video/{videoId}/attachments
- **THEN** 返回该视频的所有未删除附件列表（文件名、大小、格式、上传时间）
