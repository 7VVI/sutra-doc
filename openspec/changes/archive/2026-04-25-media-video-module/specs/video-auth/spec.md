## ADDED Requirements

### Requirement: 视频权限类型设置
系统 SHALL 支持在创建或编辑视频时设置权限类型：0=公开、1=组织可见、2=指定用户。权限类型 MUST 存储在 media_video 表的 auth_type 字段中。

#### Scenario: 设置公开视频
- **WHEN** 管理员上传视频时选择 auth_type=0
- **THEN** 所有人可直接查看该视频，无需查授权表

#### Scenario: 设置组织可见视频
- **WHEN** 管理员上传视频时选择 auth_type=1，并指定可见的组织列表
- **THEN** 仅指定组织内的用户可见该视频

#### Scenario: 设置指定用户视频
- **WHEN** 管理员上传视频时选择 auth_type=2，并指定可见的用户列表
- **THEN** 仅被指定的用户可见该视频

### Requirement: 授权记录管理
系统 SHALL 提供授权记录的增删查接口。授权记录 MUST 存储在 media_video_auth 表中，支持组织授权(auth_target_type=1)和用户授权(auth_target_type=2)。

#### Scenario: 新增组织授权
- **WHEN** 管理员 POST /media/auth 创建组织授权，指定 videoId、orgId
- **THEN** 在 media_video_auth 表创建 auth_target_type=1 的记录，同时更新视频 auth_type=1

#### Scenario: 新增用户授权
- **WHEN** 管理员 POST /media/auth 创建用户授权，指定 videoId、userId、expireTime
- **THEN** 在 media_video_auth 表创建 auth_target_type=2 的记录，同时更新视频 auth_type=2

#### Scenario: 删除授权
- **WHEN** 管理员 DELETE /media/auth/{id}
- **THEN** 逻辑删除该授权记录（del_flag='2'）

#### Scenario: 查询视频授权列表
- **WHEN** 管理员 GET /media/auth/{videoId}
- **THEN** 返回该视频的所有授权记录（分页），包含组织名称/用户名称、过期时间

### Requirement: 授权过期清理
系统 SHALL 支持清理已过期的用户授权记录。过期授权 MUST 被逻辑删除。

#### Scenario: 过期授权不可见
- **WHEN** 用户查询可观看视频列表时
- **THEN** 已过期的授权记录不参与权限判断

### Requirement: 视频列表权限过滤
视频分页查询 MUST 根据当前登录用户的组织和用户授权进行过滤，仅返回用户有权观看的视频。

#### Scenario: 用户查看视频列表
- **WHEN** 登录用户请求视频分页列表
- **THEN** 返回: 公开视频 + 用户所属组织被授权的视频 + 用户被授权的视频，三者合并去重
