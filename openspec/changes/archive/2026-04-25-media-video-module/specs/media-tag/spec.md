## ADDED Requirements

### Requirement: 标签CRUD
系统 SHALL 提供分类标签的增删改查接口。标签通过 tag_type 字段区分类型：1=分类、2=方向。

#### Scenario: 创建标签
- **WHEN** 管理员 POST /media/tag 创建标签，指定 tagName 和 tagType
- **THEN** 在 media_tag 表创建记录

#### Scenario: 查询所有分类
- **WHEN** 用户请求 GET /media/tag?type=1
- **THEN** 返回所有 tag_type=1 且 status=1 的标签列表

#### Scenario: 查询所有方向
- **WHEN** 用户请求 GET /media/tag?type=2
- **THEN** 返回所有 tag_type=2 且 status=1 的标签列表

#### Scenario: 更新标签
- **WHEN** 管理员 PUT /media/tag/{tagId} 更新标签名称或排序
- **THEN** 更新 media_tag 记录

#### Scenario: 删除标签
- **WHEN** 管理员 DELETE /media/tag/{tagId}
- **THEN** 逻辑删除标签（del_flag='2'），不影响已关联的视频数据

### Requirement: 视频-标签关联
系统 SHALL 在视频上传时自动建立视频与分类、方向的关联关系，存储在 media_video_tag 表中。

#### Scenario: 视频上传时关联标签
- **WHEN** 视频上传时指定 category=1 和 direction=2
- **THEN** 在 media_video_tag 表创建 (video_id, 1) 和 (video_id, 2) 两条记录

#### Scenario: 视频支持多个分类和方向
- **WHEN** 视频上传时指定多个分类和方向
- **THEN** 为每个分类和方向分别创建关联记录

### Requirement: 树形标签支持
系统 SHALL 支持标签的树形结构（通过 parent_id 字段），用于创建层级分类。

#### Scenario: 创建子标签
- **WHEN** 管理员创建标签时指定 parent_id=5
- **THEN** 新标签作为 ID=5 标签的子标签创建

#### Scenario: 查询标签树
- **WHEN** 用户请求 GET /media/tag/tree?type=1
- **THEN** 返回按树形结构组织的分类列表
