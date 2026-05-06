## ADDED Requirements

### Requirement: HTTP Range 流式播放
系统 SHALL 支持 HTTP Range 协议的视频流式播放。播放接口 MUST 解析 Range 请求头，从对象存储获取指定范围的字节流并返回 206 Partial Content 响应。

#### Scenario: 首次请求（无 Range 头）
- **WHEN** 客户端请求 GET /media/video/play/{videoId}，不携带 Range 头
- **THEN** 系统返回完整视频文件，响应状态码 200

#### Scenario: Range 请求播放
- **WHEN** 客户端请求携带 Range: bytes=0-1048575
- **THEN** 系统返回 206 状态码，Content-Range 头指明返回范围，Content-Length 头指明实际长度

#### Scenario: 拖动进度条（随机位置播放）
- **WHEN** 客户端请求携带 Range: bytes=5242880-
- **THEN** 系统从指定位置开始返回剩余数据，响应状态码 206

#### Scenario: 无效的 Range 范围
- **WHEN** 客户端请求的 Range 超出文件大小范围
- **THEN** 系统返回 416 Range Not Satisfiable，Content-Range 头指明文件总大小

### Requirement: 播放权限校验
视频播放接口 MUST 在返回视频流之前校验用户观看权限。公开视频(auth_type=0)所有人可看；组织可见视频(auth_type=1)需用户所属组织在授权列表中；指定用户视频(auth_type=2)需用户在授权列表中。

#### Scenario: 播放公开视频
- **WHEN** 登录用户请求播放 auth_type=0 的视频
- **THEN** 直接返回视频流

#### Scenario: 播放组织授权视频（有权限）
- **WHEN** 登录用户所属组织在视频的授权组织列表中
- **THEN** 返回视频流

#### Scenario: 播放组织授权视频（无权限）
- **WHEN** 登录用户所属组织不在视频的授权组织列表中
- **THEN** 返回 403 Forbidden

#### Scenario: 播放指定用户授权视频（已过期）
- **WHEN** 用户的授权已超过 expire_time
- **THEN** 返回 403 Forbidden

### Requirement: 播放自动记录观看
视频播放成功后，系统 MUST 自动记录观看行为。如该用户已看过该视频，SHALL NOT 重复记录，但 SHALL 更新学习进度。

#### Scenario: 首次观看记录
- **WHEN** 用户首次播放某视频成功
- **THEN** 在 media_video_action 表创建 action_type=2 的记录，视频 view_count +1

#### Scenario: 重复观看不重复计数
- **WHEN** 用户再次播放已看过的视频
- **THEN** 不新增观看记录，view_count 不变，仅更新学习进度
