## ADDED Requirements

### Requirement: 学习进度记录
系统 SHALL 在用户观看视频时自动记录和更新学习进度。学习进度 MUST 包含累计观看时长(秒)、进度百分比(0-100)、最后观看位置(秒)、是否完成。进度数据存储在 media_video_action 表的 extra JSON 字段中。

#### Scenario: 首次观看创建学习记录
- **WHEN** 用户首次播放某视频
- **THEN** 创建 action_type=2 的互动记录，extra 字段初始化进度数据

#### Scenario: 更新学习进度
- **WHEN** 用户继续观看视频（已有学习记录）
- **THEN** 更新 extra 字段中的 duration（累加）、progress（重算）、last_position（更新）、completed（判断）

#### Scenario: 视频观看完成
- **WHEN** 用户的观看进度达到 100%（或 duration >= 视频总时长）
- **THEN** extra.completed 设为 true

### Requirement: 断点续播
系统 SHALL 支持断点续播功能，返回用户上次观看的最后位置。

#### Scenario: 获取续播位置
- **WHEN** 用户请求 GET /media/video/{videoId}/progress
- **THEN** 返回 last_position（上次观看位置的秒数），如无记录返回 0

### Requirement: 学习记录查询
系统 SHALL 支持查询用户的视频学习记录列表。

#### Scenario: 查询我的学习记录
- **WHEN** 用户请求 GET /media/video/learning/mine
- **THEN** 返回该用户的所有学习记录（视频标题、观看时长、进度、完成状态、最后学习时间）

#### Scenario: 按完成状态筛选
- **WHEN** 管理员请求 GET /media/video/learning/statistics
- **THEN** 返回学习统计（总视频数、已完成数、学习中数、未开始数）
