## ADDED Requirements

### Requirement: 点赞视频
系统 SHALL 提供视频点赞功能。每个用户对同一视频只能点赞一次。点赞 MUST 使用 Redis 缓存加速判断，并原子更新视频 like_count。

#### Scenario: 首次点赞成功
- **WHEN** 用户 POST /media/video/like，指定 videoId
- **THEN** 在 media_video_action 表创建 action_type=1 记录，Redis 缓存标记已点赞，视频 like_count +1

#### Scenario: 重复点赞拒绝
- **WHEN** 用户对已点赞的视频再次请求点赞
- **THEN** 返回业务异常提示"已点赞"

### Requirement: 取消点赞
系统 SHALL 提供取消点赞功能。取消点赞 MUST 从 Redis 缓存移除标记，逻辑删除点赞记录，原子减少 like_count。

#### Scenario: 取消点赞成功
- **WHEN** 用户 POST /media/video/unlike，指定 videoId，且该用户已点赞
- **THEN** Redis 缓存移除标记，视频 like_count -1

#### Scenario: 未点赞时取消
- **WHEN** 用户请求取消点赞但实际未点赞
- **THEN** 返回业务异常提示"未点赞"

### Requirement: 分享视频
系统 SHALL 提供视频分享功能，生成包含分享令牌的链接，支持设置过期时间和最大访问次数。

#### Scenario: 创建分享链接
- **WHEN** 用户 POST /media/video/share，指定 videoId
- **THEN** 系统生成唯一 share_token，创建 media_video_share 记录，返回分享链接

#### Scenario: 通过分享链接访问
- **WHEN** 任何人通过分享链接 GET /media/video/share/{shareToken} 访问
- **THEN** 如链接未过期且未超过最大访问次数，返回视频信息并 access_count +1

#### Scenario: 分享链接已过期
- **WHEN** 分享链接的 expire_time 已过
- **THEN** 返回提示"链接已过期"

### Requirement: 观看记录查询
系统 SHALL 提供查询某视频所有观看用户的接口。

#### Scenario: 查询观看记录
- **WHEN** 用户 GET /media/video/{videoId}/watchers
- **THEN** 返回观看过该视频的用户列表（用户ID、姓名、头像）
