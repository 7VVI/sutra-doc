# 视频媒体模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 ruoyi-media Maven 模块，实现视频上传、Range 流式播放、三级权限控制、互动功能和学习进度跟踪的完整慕课平台。

**Architecture:** 标准 RuoYi-Vue-Plus 分层架构 Controller → Service → Mapper。Entity 继承 TenantEntity，Mapper 继承 BaseMapperPlus<T,V>，使用 TransactionTemplate 编程式事务。视频流式播放基于 HTTP Range 协议，文件存储复用 OssFactory。

**Tech Stack:** Spring Boot 3.5 / MyBatis-Plus / Sa-Token / OssClient(S3) / Redis / MapstructUtils

---

## File Structure

### Create (新建文件)

```
ruoyi-modules/ruoyi-media/
  pom.xml
  src/main/java/com/hmoob/media/
    domain/
      MediaVideo.java
      MediaTag.java
      MediaVideoTag.java
      MediaVideoAuth.java
      MediaVideoAction.java
      MediaVideoAttachment.java
      MediaVideoShare.java
      bo/
        MediaVideoBo.java
        MediaVideoUploadBo.java
        MediaTagBo.java
        MediaVideoAuthBo.java
        MediaVideoLikeBo.java
        MediaVideoShareBo.java
        MediaVideoAttachmentBo.java
        MediaVideoQuery.java
      vo/
        MediaVideoVo.java
        MediaVideoDetailVo.java
        MediaTagVo.java
        MediaVideoAuthVo.java
        MediaVideoActionVo.java
        MediaVideoAttachmentVo.java
        MediaVideoShareVo.java
        MediaLearningVo.java
        MediaLearningStatisticsVo.java
        MediaWatcherVo.java
    mapper/
      MediaVideoMapper.java
      MediaTagMapper.java
      MediaVideoTagMapper.java
      MediaVideoAuthMapper.java
      MediaVideoActionMapper.java
      MediaVideoAttachmentMapper.java
      MediaVideoShareMapper.java
    service/
      IMediaVideoService.java
      IMediaTagService.java
      IMediaVideoAuthService.java
      IMediaVideoActionService.java
      IMediaVideoAttachmentService.java
      IMediaVideoShareService.java
      impl/
        MediaVideoServiceImpl.java
        MediaTagServiceImpl.java
        MediaVideoAuthServiceImpl.java
        MediaVideoActionServiceImpl.java
        MediaVideoAttachmentServiceImpl.java
        MediaVideoShareServiceImpl.java
    controller/
      MediaVideoController.java
      MediaTagController.java
      MediaVideoAuthController.java
```

### Modify (修改文件)

```
ruoyi-modules/pom.xml  -- 添加 ruoyi-media module
```

---

### Task 1: Maven 模块骨架

**Files:**
- Create: `ruoyi-modules/ruoyi-media/pom.xml`
- Modify: `ruoyi-modules/pom.xml`

- [ ] **Step 1: 创建 ruoyi-media/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.hmoob</groupId>
        <artifactId>ruoyi-modules</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>ruoyi-media</artifactId>
    <description>视频媒体模块</description>

    <dependencies>
        <dependency>
            <groupId>com.hmoob</groupId>
            <artifactId>ruoyi-common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.hmoob</groupId>
            <artifactId>ruoyi-common-mybatis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.hmoob</groupId>
            <artifactId>ruoyi-common-tenant</artifactId>
        </dependency>
        <dependency>
            <groupId>com.hmoob</groupId>
            <artifactId>ruoyi-common-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.hmoob</groupId>
            <artifactId>ruoyi-common-log</artifactId>
        </dependency>
        <dependency>
            <groupId>com.hmoob</groupId>
            <artifactId>ruoyi-common-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.hmoob</groupId>
            <artifactId>ruoyi-common-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.hmoob</groupId>
            <artifactId>ruoyi-common-oss</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 在 ruoyi-modules/pom.xml 的 modules 中添加 ruoyi-media**

在 `<modules>` 块内末尾添加:
```xml
<module>ruoyi-media</module>
```

- [ ] **Step 3: 创建包目录结构**

创建以下空目录:
```
ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/controller/
ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/bo/
ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/vo/
ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/mapper/
ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/service/impl/
```

- [ ] **Step 4: Commit**

```bash
git add ruoyi-modules/ruoyi-media/ ruoyi-modules/pom.xml
git commit -m "feat(media): 初始化 ruoyi-media 模块骨架"
```

---

### Task 2: Entity 层 — 7 个实体类

**Files:**
- Create: `ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/MediaVideo.java`
- Create: `ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/MediaTag.java`
- Create: `ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/MediaVideoTag.java`
- Create: `ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/MediaVideoAuth.java`
- Create: `ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/MediaVideoAction.java`
- Create: `ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/MediaVideoAttachment.java`
- Create: `ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/MediaVideoShare.java`

- [ ] **Step 1: 创建 MediaTag.java**

```java
package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 分类标签对象 media_tag
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_tag")
public class MediaTag extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标签ID */
    @TableId(value = "tag_id")
    private Long tagId;

    /** 标签名称 */
    private String tagName;

    /** 标签类型: 1=分类, 2=方向 */
    private Integer tagType;

    /** 父标签ID, 0=顶级 */
    private Long parentId;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态: 1-启用, 0-停用 */
    private Integer status;

    /** 删除标志: 0-正常, 2-删除 */
    private String delFlag;
}
```

- [ ] **Step 2: 创建 MediaVideo.java**

```java
package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 视频主表对象 media_video
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_video")
public class MediaVideo extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 视频ID */
    @TableId(value = "video_id")
    private Long videoId;

    /** 视频标题 */
    private String title;

    /** 视频描述 */
    private String description;

    /** 作者/上传人ID */
    private Long authorId;

    /** 文件格式: mp4, avi, mov等 */
    private String fileType;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 视频时长(秒) */
    private Integer duration;

    /** 视频文件存储路径 */
    private String videoPath;

    /** 封面缩略图存储路径 */
    private String thumbnail;

    /** 观看次数 */
    private Long viewCount;

    /** 点赞数 */
    private Long likeCount;

    /** 权限类型: 0-公开, 1-组织可见, 2-指定用户 */
    private Integer authType;

    /** 状态: 1-正常, 0-停用 */
    private Integer status;

    /** 删除标志: 0-正常, 2-删除 */
    @TableLogic
    private String delFlag;
}
```

- [ ] **Step 3: 创建 MediaVideoTag.java**

```java
package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频-标签关联对象 media_video_tag
 */
@Data
@TableName("media_video_tag")
public class MediaVideoTag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 视频ID */
    private Long videoId;

    /** 标签ID */
    private Long tagId;
}
```

- [ ] **Step 4: 创建 MediaVideoAuth.java**

```java
package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 视频授权对象 media_video_auth
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_video_auth")
public class MediaVideoAuth extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 视频ID */
    private Long videoId;

    /** 授权目标类型: 1-组织, 2-用户 */
    private Integer authTargetType;

    /** 授权目标ID(组织ID或用户ID) */
    private Long authTargetId;

    /** 目标名称快照 */
    private String authTargetName;

    /** 过期时间 */
    private Date expireTime;

    /** 删除标志 */
    private String delFlag;
}
```

- [ ] **Step 5: 创建 MediaVideoAction.java**

```java
package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频互动记录对象 media_video_action
 */
@Data
@TableName("media_video_action")
public class MediaVideoAction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 视频ID */
    private Long videoId;

    /** 操作用户ID */
    private Long userId;

    /** 操作类型: 1-点赞, 2-观看, 3-分享 */
    private Integer actionType;

    /** 扩展信息JSON */
    private String extra;

    /** 首次操作时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date updateTime;

    /** 删除标志 */
    private String delFlag;
}
```

- [ ] **Step 6: 创建 MediaVideoAttachment.java**

```java
package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 视频附件对象 media_video_attachment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_video_attachment")
public class MediaVideoAttachment extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 附件ID */
    @TableId(value = "attachment_id")
    private Long attachmentId;

    /** 关联视频ID */
    private Long videoId;

    /** 原始文件名 */
    private String fileName;

    /** 文件存储路径 */
    private String filePath;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 文件格式 */
    private String fileFormat;

    /** 删除标志 */
    @TableLogic
    private String delFlag;
}
```

- [ ] **Step 7: 创建 MediaVideoShare.java**

```java
package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 视频分享链接对象 media_video_share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_video_share")
public class MediaVideoShare extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 视频ID */
    private Long videoId;

    /** 分享令牌 */
    private String shareToken;

    /** 过期时间 */
    private Date expireTime;

    /** 已访问次数 */
    private Integer accessCount;

    /** 最大访问次数, 0=不限 */
    private Integer maxAccess;

    /** 删除标志 */
    private String delFlag;
}
```

- [ ] **Step 8: Commit**

```bash
git add ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/
git commit -m "feat(media): 添加 7 个 Entity 实体类"
```

---

### Task 3: Mapper 层

**Files:**
- Create: 7 个 Mapper 接口

- [ ] **Step 1: 创建 7 个 Mapper**

所有 Mapper 遵循同一模式 `extends BaseMapperPlus<Entity, EntityVo>`:

**MediaVideoMapper.java:**
```java
package com.hmoob.media.mapper;

import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.media.domain.MediaVideo;
import com.hmoob.media.domain.vo.MediaVideoVo;

/**
 * 视频主表 Mapper接口
 */
public interface MediaVideoMapper extends BaseMapperPlus<MediaVideo, MediaVideoVo> {
}
```

**MediaTagMapper.java:**
```java
package com.hmoob.media.mapper;

import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.media.domain.MediaTag;
import com.hmoob.media.domain.vo.MediaTagVo;

/**
 * 分类标签 Mapper接口
 */
public interface MediaTagMapper extends BaseMapperPlus<MediaTag, MediaTagVo> {
}
```

**MediaVideoTagMapper.java:**
```java
package com.hmoob.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmoob.media.domain.MediaVideoTag;

/**
 * 视频-标签关联 Mapper接口
 */
public interface MediaVideoTagMapper extends BaseMapper<MediaVideoTag> {
}
```

**MediaVideoAuthMapper.java:**
```java
package com.hmoob.media.mapper;

import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.media.domain.MediaVideoAuth;
import com.hmoob.media.domain.vo.MediaVideoAuthVo;

/**
 * 视频授权 Mapper接口
 */
public interface MediaVideoAuthMapper extends BaseMapperPlus<MediaVideoAuth, MediaVideoAuthVo> {
}
```

**MediaVideoActionMapper.java:**
```java
package com.hmoob.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmoob.media.domain.MediaVideoAction;

/**
 * 视频互动记录 Mapper接口
 */
public interface MediaVideoActionMapper extends BaseMapper<MediaVideoAction> {
}
```

**MediaVideoAttachmentMapper.java:**
```java
package com.hmoob.media.mapper;

import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.media.domain.MediaVideoAttachment;
import com.hmoob.media.domain.vo.MediaVideoAttachmentVo;

/**
 * 视频附件 Mapper接口
 */
public interface MediaVideoAttachmentMapper extends BaseMapperPlus<MediaVideoAttachment, MediaVideoAttachmentVo> {
}
```

**MediaVideoShareMapper.java:**
```java
package com.hmoob.media.mapper;

import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.media.domain.MediaVideoShare;
import com.hmoob.media.domain.vo.MediaVideoShareVo;

/**
 * 视频分享链接 Mapper接口
 */
public interface MediaVideoShareMapper extends BaseMapperPlus<MediaVideoShare, MediaVideoShareVo> {
}
```

- [ ] **Step 2: Commit**

```bash
git add ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/mapper/
git commit -m "feat(media): 添加 7 个 Mapper 接口"
```

---

### Task 4: Bo / Vo 对象层

**Files:**
- Create: `domain/bo/` 下 8 个 Bo 类
- Create: `domain/vo/` 下 10 个 Vo 类

- [ ] **Step 1: 创建 Bo 类**

**MediaTagBo.java:**
```java
package com.hmoob.media.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.media.domain.MediaTag;

import java.io.Serial;

/**
 * 分类标签业务对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MediaTag.class, reverseConvertGenerate = false)
public class MediaTagBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标签ID */
    private Long tagId;

    /** 标签名称 */
    @NotBlank(message = "标签名称不能为空")
    private String tagName;

    /** 标签类型: 1=分类, 2=方向 */
    @NotNull(message = "标签类型不能为空")
    private Integer tagType;

    /** 父标签ID */
    private Long parentId;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
```

**MediaVideoUploadBo.java:**
```java
package com.hmoob.media.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 视频上传业务对象
 */
@Data
public class MediaVideoUploadBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 视频标题 */
    @NotBlank(message = "视频标题不能为空")
    private String title;

    /** 视频描述 */
    private String description;

    /** 作者ID */
    @NotNull(message = "作者不能为空")
    private Long authorId;

    /** 权限类型: 0-公开, 1-组织可见, 2-指定用户 */
    private Integer authType;

    /** 分类ID列表 */
    private List<Long> categoryIds;

    /** 方向ID列表 */
    private List<Long> directionIds;
}
```

**MediaVideoQuery.java:**
```java
package com.hmoob.media.domain.bo;

import com.hmoob.common.mybatis.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 视频分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MediaVideoQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 搜索关键词 */
    private String keyword;

    /** 作者ID列表 */
    private List<Long> authorIds;

    /** 分类ID列表 */
    private List<Long> categoryIds;

    /** 方向ID列表 */
    private List<Long> directionIds;

    /** 排序方式: newest/hottest */
    private String sort;
}
```

**MediaVideoAuthBo.java:**
```java
package com.hmoob.media.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频授权业务对象
 */
@Data
public class MediaVideoAuthBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 视频ID */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    /** 授权目标类型: 1-组织, 2-用户 */
    @NotNull(message = "授权目标类型不能为空")
    private Integer authTargetType;

    /** 授权目标ID */
    @NotNull(message = "授权目标ID不能为空")
    private Long authTargetId;

    /** 目标名称快照 */
    private String authTargetName;

    /** 过期时间 */
    private Date expireTime;
}
```

**MediaVideoLikeBo.java:**
```java
package com.hmoob.media.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频点赞业务对象
 */
@Data
public class MediaVideoLikeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 视频ID */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;
}
```

**MediaVideoShareBo.java:**
```java
package com.hmoob.media.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频分享业务对象
 */
@Data
public class MediaVideoShareBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 视频ID */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    /** 过期时间 */
    private Date expireTime;

    /** 最大访问次数, 0=不限 */
    private Integer maxAccess;
}
```

**MediaVideoAttachmentBo.java:**
```java
package com.hmoob.media.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频附件上传业务对象
 */
@Data
public class MediaVideoAttachmentBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联视频ID */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;
}
```

**MediaVideoBo.java:**
```java
package com.hmoob.media.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.media.domain.MediaVideo;

import java.io.Serial;

/**
 * 视频编辑业务对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MediaVideo.class, reverseConvertGenerate = false)
public class MediaVideoBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 视频ID */
    private Long videoId;

    /** 视频标题 */
    private String title;

    /** 视频描述 */
    private String description;

    /** 权限类型 */
    private Integer authType;

    /** 状态 */
    private Integer status;
}
```

- [ ] **Step 2: 创建 Vo 类**

**MediaTagVo.java:**
```java
package com.hmoob.media.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.media.domain.MediaTag;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 分类标签视图对象
 */
@Data
@AutoMapper(target = MediaTag.class)
public class MediaTagVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long tagId;
    private String tagName;
    private Integer tagType;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private Date createTime;

    /** 子标签列表(树形) */
    private List<MediaTagVo> children;
}
```

**MediaVideoVo.java:**
```java
package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频列表视图对象(分页查询用)
 */
@Data
public class MediaVideoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long videoId;
    private String title;
    private String description;
    private Long authorId;
    private String authorName;
    private String thumbnailUrl;
    private String fileType;
    private Long fileSize;
    private Long viewCount;
    private Long likeCount;
    private Integer authType;
    private String authDeptNames;
    private Boolean hasLiked;
    private Long attachmentCount;
    private Date createTime;
    private Date updateTime;
}
```

**MediaVideoDetailVo.java:**
```java
package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 视频详情视图对象
 */
@Data
public class MediaVideoDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long videoId;
    private String title;
    private String description;
    private Long authorId;
    private String authorName;
    private String thumbnailUrl;
    private String fileType;
    private Long fileSize;
    private Integer duration;
    private Long viewCount;
    private Long likeCount;
    private Integer authType;
    private Boolean hasLiked;
    private Boolean isAuthor;
    private Date createTime;
    private Date updateTime;

    /** 点赞用户头像列表 */
    private List<MediaWatcherVo> likedUsers;

    /** 附件列表 */
    private List<MediaVideoAttachmentVo> attachments;
}
```

**MediaVideoAuthVo.java:**
```java
package com.hmoob.media.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.media.domain.MediaVideoAuth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频授权视图对象
 */
@Data
@AutoMapper(target = MediaVideoAuth.class)
public class MediaVideoAuthVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long videoId;
    private Integer authTargetType;
    private Long authTargetId;
    private String authTargetName;
    private Date expireTime;
    private Date createTime;
}
```

**MediaVideoAttachmentVo.java:**
```java
package com.hmoob.media.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.media.domain.MediaVideoAttachment;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频附件视图对象
 */
@Data
@AutoMapper(target = MediaVideoAttachment.class)
public class MediaVideoAttachmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long attachmentId;
    private Long videoId;
    private String fileName;
    private Long fileSize;
    private String fileFormat;
    private Date createTime;
}
```

**MediaVideoShareVo.java:**
```java
package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频分享视图对象
 */
@Data
public class MediaVideoShareVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String shareToken;
    private String shareUrl;
}
```

**MediaWatcherVo.java:**
```java
package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 观看/点赞用户视图对象
 */
@Data
public class MediaWatcherVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userName;
    private String avatar;
}
```

**MediaLearningVo.java:**
```java
package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 学习记录视图对象
 */
@Data
public class MediaLearningVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long videoId;
    private String videoTitle;
    private String thumbnailUrl;
    private Integer watchDuration;
    private BigDecimal progress;
    private Integer lastPosition;
    private Boolean completed;
    private Date updateTime;
}
```

**MediaLearningStatisticsVo.java:**
```java
package com.hmoob.media.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学习统计视图对象
 */
@Data
public class MediaLearningStatisticsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 总学习视频数 */
    private Long totalCount;
    /** 已完成数 */
    private Long completedCount;
    /** 学习中数 */
    private Long learningCount;
}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/bo/ ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/domain/vo/
git commit -m "feat(media): 添加 Bo/Vo 对象层"
```

---

### Task 5: 标签管理 — Service + Controller

**Files:**
- Create: `service/IMediaTagService.java`
- Create: `service/impl/MediaTagServiceImpl.java`
- Create: `controller/MediaTagController.java`

- [ ] **Step 1: 创建 IMediaTagService.java**

```java
package com.hmoob.media.service;

import com.hmoob.media.domain.bo.MediaTagBo;
import com.hmoob.media.domain.vo.MediaTagVo;

import java.util.List;

/**
 * 分类标签服务接口
 */
public interface IMediaTagService {

    /**
     * 查询标签列表
     *
     * @param tagType 标签类型: 1=分类, 2=方向, null=全部
     * @return 标签列表
     */
    List<MediaTagVo> selectTagList(Integer tagType);

    /**
     * 查询标签树
     *
     * @param tagType 标签类型
     * @return 树形标签列表
     */
    List<MediaTagVo> selectTagTree(Integer tagType);

    /**
     * 新增标签
     *
     * @param bo 标签信息
     * @return 结果
     */
    int insertTag(MediaTagBo bo);

    /**
     * 修改标签
     *
     * @param bo 标签信息
     * @return 结果
     */
    int updateTag(MediaTagBo bo);

    /**
     * 删除标签
     *
     * @param tagId 标签ID
     * @return 结果
     */
    int deleteTag(Long tagId);
}
```

- [ ] **Step 2: 创建 MediaTagServiceImpl.java**

```java
package com.hmoob.media.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.media.domain.MediaTag;
import com.hmoob.media.domain.bo.MediaTagBo;
import com.hmoob.media.domain.vo.MediaTagVo;
import com.hmoob.media.mapper.MediaTagMapper;
import com.hmoob.media.service.IMediaTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类标签服务实现
 */
@RequiredArgsConstructor
@Service
public class MediaTagServiceImpl implements IMediaTagService {

    private final MediaTagMapper baseMapper;

    @Override
    public List<MediaTagVo> selectTagList(Integer tagType) {
        LambdaQueryWrapper<MediaTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tagType != null, MediaTag::getTagType, tagType)
            .eq(MediaTag::getStatus, 1)
            .orderByAsc(MediaTag::getSortOrder);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public List<MediaTagVo> selectTagTree(Integer tagType) {
        List<MediaTagVo> allTags = selectTagList(tagType);
        return buildTree(allTags, 0L);
    }

    private List<MediaTagVo> buildTree(List<MediaTagVo> allTags, Long parentId) {
        return allTags.stream()
            .filter(t -> parentId.equals(t.getParentId()))
            .peek(t -> t.setChildren(buildTree(allTags, t.getTagId())))
            .collect(Collectors.toList());
    }

    @Override
    public int insertTag(MediaTagBo bo) {
        MediaTag tag = MapstructUtils.convert(bo, MediaTag.class);
        return baseMapper.insert(tag);
    }

    @Override
    public int updateTag(MediaTagBo bo) {
        MediaTag tag = MapstructUtils.convert(bo, MediaTag.class);
        return baseMapper.updateById(tag);
    }

    @Override
    public int deleteTag(Long tagId) {
        return baseMapper.deleteById(tagId);
    }
}
```

- [ ] **Step 3: 创建 MediaTagController.java**

```java
package com.hmoob.media.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.media.domain.bo.MediaTagBo;
import com.hmoob.media.domain.vo.MediaTagVo;
import com.hmoob.media.service.IMediaTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类标签管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/media/tag")
public class MediaTagController extends BaseController {

    private final IMediaTagService tagService;

    /**
     * 查询标签列表
     */
    @GetMapping("/list")
    public R<List<MediaTagVo>> list(@RequestParam(required = false) Integer type) {
        return R.ok(tagService.selectTagList(type));
    }

    /**
     * 查询标签树
     */
    @GetMapping("/tree")
    public R<List<MediaTagVo>> tree(@RequestParam(required = false) Integer type) {
        return R.ok(tagService.selectTagTree(type));
    }

    /**
     * 新增标签
     */
    @SaCheckPermission("media:tag:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody MediaTagBo bo) {
        return toAjax(tagService.insertTag(bo));
    }

    /**
     * 修改标签
     */
    @SaCheckPermission("media:tag:edit")
    @PutMapping("/{tagId}")
    public R<Void> edit(@Validated @RequestBody MediaTagBo bo) {
        return toAjax(tagService.updateTag(bo));
    }

    /**
     * 删除标签
     */
    @SaCheckPermission("media:tag:remove")
    @DeleteMapping("/{tagId}")
    public R<Void> remove(@PathVariable Long tagId) {
        return toAjax(tagService.deleteTag(tagId));
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/service/IMediaTagService.java ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/service/impl/MediaTagServiceImpl.java ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/controller/MediaTagController.java
git commit -m "feat(media): 实现分类标签管理 Service + Controller"
```

---

### Task 6: 视频授权 Service

**Files:**
- Create: `service/IMediaVideoAuthService.java`
- Create: `service/impl/MediaVideoAuthServiceImpl.java`

- [ ] **Step 1: 创建 IMediaVideoAuthService.java**

```java
package com.hmoob.media.service;

import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.media.domain.bo.MediaVideoAuthBo;
import com.hmoob.media.domain.vo.MediaVideoAuthVo;

import java.util.List;

/**
 * 视频授权服务接口
 */
public interface IMediaVideoAuthService {

    /**
     * 新增授权
     *
     * @param bo 授权信息
     * @return 结果
     */
    int insertAuth(MediaVideoAuthBo bo);

    /**
     * 删除授权
     *
     * @param id 授权记录ID
     * @return 结果
     */
    int deleteAuth(Long id);

    /**
     * 查询视频授权列表(分页)
     *
     * @param videoId   视频ID
     * @param pageQuery 分页参数
     * @return 授权列表
     */
    TableDataInfo<MediaVideoAuthVo> selectAuthPage(Long videoId, PageQuery pageQuery);

    /**
     * 获取用户有权限观看的视频ID列表
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 视频ID列表
     */
    List<Long> getAuthorizedVideoIds(Long userId, Long deptId);
}
```

- [ ] **Step 2: 创建 MediaVideoAuthServiceImpl.java**

```java
package com.hmoob.media.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.media.domain.MediaVideoAuth;
import com.hmoob.media.domain.bo.MediaVideoAuthBo;
import com.hmoob.media.domain.vo.MediaVideoAuthVo;
import com.hmoob.media.mapper.MediaVideoAuthMapper;
import com.hmoob.media.service.IMediaVideoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视频授权服务实现
 */
@RequiredArgsConstructor
@Service
public class MediaVideoAuthServiceImpl implements IMediaVideoAuthService {

    private final MediaVideoAuthMapper baseMapper;

    @Override
    public int insertAuth(MediaVideoAuthBo bo) {
        MediaVideoAuth auth = MapstructUtils.convert(bo, MediaVideoAuth.class);
        return baseMapper.insert(auth);
    }

    @Override
    public int deleteAuth(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public TableDataInfo<MediaVideoAuthVo> selectAuthPage(Long videoId, PageQuery pageQuery) {
        LambdaQueryWrapper<MediaVideoAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MediaVideoAuth::getVideoId, videoId)
            .orderByDesc(MediaVideoAuth::getCreateTime);
        Page<MediaVideoAuthVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<Long> getAuthorizedVideoIds(Long userId, Long deptId) {
        // 1. 获取所有 auth_type=0 (公开) 的视频ID — 由调用方直接处理
        // 2. 获取组织授权的视频ID
        List<Long> result = new ArrayList<>();
        if (deptId != null) {
            List<MediaVideoAuth> orgAuths = baseMapper.selectList(
                new LambdaQueryWrapper<MediaVideoAuth>()
                    .eq(MediaVideoAuth::getAuthTargetType, 1)
                    .eq(MediaVideoAuth::getAuthTargetId, deptId)
            );
            result.addAll(orgAuths.stream().map(MediaVideoAuth::getVideoId).collect(Collectors.toList()));
        }
        // 3. 获取用户授权的视频ID(含未过期的)
        List<MediaVideoAuth> userAuths = baseMapper.selectList(
            new LambdaQueryWrapper<MediaVideoAuth>()
                .eq(MediaVideoAuth::getAuthTargetType, 2)
                .eq(MediaVideoAuth::getAuthTargetId, userId)
                .and(w -> w.isNull(MediaVideoAuth::getExpireTime)
                    .or().gt(MediaVideoAuth::getExpireTime, new Date()))
        );
        result.addAll(userAuths.stream().map(MediaVideoAuth::getVideoId).collect(Collectors.toList()));
        return result.stream().distinct().collect(Collectors.toList());
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/service/IMediaVideoAuthService.java ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/service/impl/MediaVideoAuthServiceImpl.java
git commit -m "feat(media): 实现视频授权 Service"
```

---

### Task 7: 视频 Service — 核心业务(上传/查询/删除)

**Files:**
- Create: `service/IMediaVideoService.java`
- Create: `service/impl/MediaVideoServiceImpl.java` (核心部分)

这是最大的一个 Service。IMediaVideoService 分两步完成：Task 7 做上传/查询/删除，Task 8 做播放/互动。

- [ ] **Step 1: 创建 IMediaVideoService.java**

```java
package com.hmoob.media.service;

import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.media.domain.bo.MediaVideoQuery;
import com.hmoob.media.domain.bo.MediaVideoUploadBo;
import com.hmoob.media.domain.vo.MediaVideoDetailVo;
import com.hmoob.media.domain.vo.MediaVideoVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频管理服务接口
 */
public interface IMediaVideoService {

    /**
     * 上传视频
     *
     * @param file      视频文件
     * @param thumbnail 缩略图文件
     * @param bo        上传参数
     * @return 视频ID
     */
    Long uploadVideo(MultipartFile file, MultipartFile thumbnail, MediaVideoUploadBo bo);

    /**
     * 分页查询视频列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    TableDataInfo<MediaVideoVo> selectVideoPage(MediaVideoQuery query);

    /**
     * 查询视频详情
     *
     * @param videoId 视频ID
     * @return 视频详情
     */
    MediaVideoDetailVo selectVideoDetail(Long videoId);

    /**
     * 删除视频
     *
     * @param videoId 视频ID
     * @return 结果
     */
    int deleteVideo(Long videoId);

    /**
     * Range 流式播放视频
     *
     * @param videoId 视频ID
     * @param request  HTTP请求
     * @param response HTTP响应
     */
    void playVideo(Long videoId, HttpServletRequest request, HttpServletResponse response);

    /**
     * 点赞视频
     *
     * @param videoId 视频ID
     * @return 结果
     */
    boolean likeVideo(Long videoId);

    /**
     * 取消点赞
     *
     * @param videoId 视频ID
     * @return 结果
     */
    boolean unlikeVideo(Long videoId);
}
```

- [ ] **Step 2: 创建 MediaVideoServiceImpl.java (上传/查询/删除部分)**

由于文件较长，按功能分批编写。先写基础骨架 + 上传 + 查询 + 删除：

```java
package com.hmoob.media.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.oss.core.OssClient;
import com.hmoob.common.oss.entity.UploadResult;
import com.hmoob.common.oss.factory.OssFactory;
import com.hmoob.common.satoken.utils.LoginHelper;
import com.hmoob.media.domain.*;
import com.hmoob.media.domain.bo.MediaVideoQuery;
import com.hmoob.media.domain.bo.MediaVideoUploadBo;
import com.hmoob.media.domain.vo.*;
import com.hmoob.media.mapper.*;
import com.hmoob.media.service.IMediaVideoService;
import com.hmoob.media.service.IMediaVideoAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 视频管理服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MediaVideoServiceImpl implements IMediaVideoService {

    private static final ConcurrentMap<Long, Object> userLockMap = new ConcurrentHashMap<>();

    private final MediaVideoMapper videoMapper;
    private final MediaVideoTagMapper videoTagMapper;
    private final MediaVideoActionMapper actionMapper;
    private final MediaVideoAttachmentMapper attachmentMapper;
    private final IMediaVideoAuthService authService;
    private final TransactionTemplate transactionTemplate;

    private Object getUserLock(Long userId) {
        return userLockMap.computeIfAbsent(userId, k -> new Object());
    }

    // ==================== 上传 ====================

    @Override
    public Long uploadVideo(MultipartFile file, MultipartFile thumbnail, MediaVideoUploadBo bo) {
        try {
            // 上传视频文件到 OSS
            OssClient ossClient = OssFactory.instance();
            UploadResult videoResult = ossClient.uploadSuffix(
                file.getBytes(), file.getOriginalFilename(), file.getContentType());
            String videoPath = videoResult.getUrl();

            // 上传缩略图到 OSS
            String thumbnailPath = null;
            if (thumbnail != null && !thumbnail.isEmpty()) {
                UploadResult thumbResult = ossClient.uploadSuffix(
                    thumbnail.getBytes(), thumbnail.getOriginalFilename(), thumbnail.getContentType());
                thumbnailPath = thumbResult.getUrl();
            }

            // 构建视频实体
            MediaVideo video = new MediaVideo();
            video.setTitle(bo.getTitle());
            video.setDescription(bo.getDescription());
            video.setAuthorId(bo.getAuthorId());
            video.setVideoPath(videoPath);
            video.setThumbnail(thumbnailPath);
            video.setFileSize(file.getSize());
            video.setFileType(getFileExtension(file.getOriginalFilename()));
            video.setViewCount(0L);
            video.setLikeCount(0L);
            video.setAuthType(bo.getAuthType() != null ? bo.getAuthType() : 0);
            video.setStatus(1);

            // 事务：保存视频 + 关联标签
            transactionTemplate.executeWithoutResult(status -> {
                videoMapper.insert(video);
                Long videoId = video.getVideoId();
                // 关联分类
                if (CollUtil.isNotEmpty(bo.getCategoryIds())) {
                    for (Long categoryId : bo.getCategoryIds()) {
                        MediaVideoTag vt = new MediaVideoTag();
                        vt.setVideoId(videoId);
                        vt.setTagId(categoryId);
                        videoTagMapper.insert(vt);
                    }
                }
                // 关联方向
                if (CollUtil.isNotEmpty(bo.getDirectionIds())) {
                    for (Long directionId : bo.getDirectionIds()) {
                        MediaVideoTag vt = new MediaVideoTag();
                        vt.setVideoId(videoId);
                        vt.setTagId(directionId);
                        videoTagMapper.insert(vt);
                    }
                }
            });

            return video.getVideoId();
        } catch (IOException e) {
            log.error("视频上传失败", e);
            throw new ServiceException("视频上传失败：" + e.getMessage());
        }
    }

    // ==================== 分页查询 ====================

    @Override
    public TableDataInfo<MediaVideoVo> selectVideoPage(MediaVideoQuery query) {
        Long userId = LoginHelper.getUserId();
        Long deptId = LoginHelper.getDeptId();

        // 获取用户有权观看的视频ID
        List<Long> authorizedIds = authService.getAuthorizedVideoIds(userId, deptId);

        // 构建查询
        LambdaQueryWrapper<MediaVideo> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.and(w -> w.like(MediaVideo::getTitle, query.getKeyword()));
        }
        // 排序
        if ("hottest".equals(query.getSort())) {
            wrapper.orderByDesc(MediaVideo::getLikeCount);
        } else {
            wrapper.orderByDesc(MediaVideo::getCreateTime);
        }

        // 权限过滤: 公开视频 OR 用户有权限的视频
        wrapper.and(w -> {
            w.eq(MediaVideo::getAuthType, 0);
            if (CollUtil.isNotEmpty(authorizedIds)) {
                w.or().in(MediaVideo::getVideoId, authorizedIds);
            }
        });

        Page<MediaVideoVo> page = videoMapper.selectVoPage(query.build(), wrapper);
        return TableDataInfo.build(page);
    }

    // ==================== 详情 ====================

    @Override
    public MediaVideoDetailVo selectVideoDetail(Long videoId) {
        MediaVideo video = videoMapper.selectById(videoId);
        if (video == null) {
            return null;
        }
        MediaVideoDetailVo detail = new MediaVideoDetailVo();
        detail.setVideoId(video.getVideoId());
        detail.setTitle(video.getTitle());
        detail.setDescription(video.getDescription());
        detail.setAuthorId(video.getAuthorId());
        detail.setFileType(video.getFileType());
        detail.setFileSize(video.getFileSize());
        detail.setDuration(video.getDuration());
        detail.setViewCount(video.getViewCount());
        detail.setLikeCount(video.getLikeCount());
        detail.setAuthType(video.getAuthType());
        detail.setCreateTime(video.getCreateTime());
        detail.setUpdateTime(video.getUpdateTime());
        detail.setIsAuthor(Objects.equals(video.getAuthorId(), LoginHelper.getUserId()));

        // 缩略图 URL
        try {
            detail.setThumbnailUrl(OssFactory.instance().getDomain() + "/" + video.getThumbnail());
        } catch (Exception e) {
            detail.setThumbnailUrl(video.getThumbnail());
        }

        // 附件列表
        List<MediaVideoAttachment> attachments = attachmentMapper.selectList(
            new LambdaQueryWrapper<MediaVideoAttachment>()
                .eq(MediaVideoAttachment::getVideoId, videoId));
        detail.setAttachments(MapstructUtils.convert(attachments, MediaVideoAttachmentVo.class));

        return detail;
    }

    // ==================== 删除 ====================

    @Override
    public int deleteVideo(Long videoId) {
        return transactionTemplate.execute(status -> {
            // 逻辑删除附件
            attachmentMapper.selectList(
                new LambdaQueryWrapper<MediaVideoAttachment>()
                    .eq(MediaVideoAttachment::getVideoId, videoId))
                .forEach(a -> attachmentMapper.deleteById(a.getAttachmentId()));
            // 删除视频
            return videoMapper.deleteById(videoId);
        });
    }

    // ==================== Range 播放 (Task 8 实现) ====================

    @Override
    public void playVideo(Long videoId, HttpServletRequest request, HttpServletResponse response) {
        // Task 8 实现
    }

    @Override
    public boolean likeVideo(Long videoId) {
        return false; // Task 8 实现
    }

    @Override
    public boolean unlikeVideo(Long videoId) {
        return false; // Task 8 实现
    }

    // ==================== 工具方法 ====================

    private String getFileExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        int idx = fileName.lastIndexOf(".");
        if (idx == -1 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/service/IMediaVideoService.java ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/service/impl/MediaVideoServiceImpl.java
git commit -m "feat(media): 实现视频 Service 核心业务(上传/查询/删除)"
```

---

### Task 8: Range 流式播放 + 点赞

**Files:**
- Modify: `service/impl/MediaVideoServiceImpl.java` — 补全 playVideo/likeVideo/unlikeVideo

- [ ] **Step 1: 实现 playVideo Range 流式播放**

替换 `MediaVideoServiceImpl` 中的 `playVideo` 方法:

```java
@Override
public void playVideo(Long videoId, HttpServletRequest request, HttpServletResponse response) {
    Long userId = LoginHelper.getUserId();
    Long deptId = LoginHelper.getDeptId();

    // 权限校验
    MediaVideo video = videoMapper.selectById(videoId);
    if (video == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
    }
    if (!isVideoVisible(video, userId, deptId)) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
    }

    try {
        OssClient ossClient = OssFactory.instance();
        String objectKey = video.getVideoPath();

        // 获取文件内容(全量)
        InputStream inputStream = ossClient.getObjectContent(objectKey);

        // 简化处理：获取全部数据后按 Range 裁剪
        byte[] allBytes = inputStream.readAllBytes();
        long fileLength = allBytes.length;

        // 解析 Range 头
        long startRange = 0;
        long endRange = fileLength - 1;
        String rangeHeader = request.getHeader("Range");

        if (StrUtil.isNotBlank(rangeHeader)) {
            try {
                String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                startRange = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && StrUtil.isNotBlank(ranges[1])) {
                    endRange = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException e) {
                startRange = 0;
                endRange = fileLength - 1;
            }
        }

        // 校验范围
        if (startRange < 0 || startRange >= fileLength || endRange >= fileLength) {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader("Content-Range", "bytes */" + fileLength);
            return;
        }

        long contentLength = endRange - startRange + 1;

        response.reset();
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setContentType("video/" + getFileExtension(video.getVideoPath()));
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("Content-Range",
            String.format("bytes %d-%d/%d", startRange, endRange, fileLength));
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        try (OutputStream os = response.getOutputStream()) {
            os.write(allBytes, (int) startRange, (int) contentLength);
            os.flush();
        }

        // 记录观看
        recordWatch(videoId, userId);

    } catch (Exception e) {
        log.error("视频播放失败: videoId={}", videoId, e);
        if (!response.isCommitted()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

/**
 * 判断视频对用户是否可见
 */
private boolean isVideoVisible(MediaVideo video, Long userId, Long deptId) {
    // 公开
    if (video.getAuthType() == null || video.getAuthType() == 0) {
        return true;
    }
    // 检查授权
    List<Long> authorizedIds = authService.getAuthorizedVideoIds(userId, deptId);
    return authorizedIds.contains(video.getVideoId());
}

/**
 * 记录观看
 */
private void recordWatch(Long videoId, Long userId) {
    synchronized (getUserLock(userId)) {
        transactionTemplate.executeWithoutResult(status -> {
            // 检查是否已观看
            Long count = actionMapper.selectCount(
                new LambdaQueryWrapper<MediaVideoAction>()
                    .eq(MediaVideoAction::getVideoId, videoId)
                    .eq(MediaVideoAction::getUserId, userId)
                    .eq(MediaVideoAction::getActionType, 2));
            if (count > 0) {
                return; // 已看过，不重复计数
            }
            // view_count +1
            videoMapper.update(null,
                new LambdaQueryWrapper<MediaVideo>()
                    .eq(MediaVideo::getVideoId, videoId)
                    .setSql("view_count = view_count + 1"));
            // 插入观看记录
            MediaVideoAction action = new MediaVideoAction();
            action.setVideoId(videoId);
            action.setUserId(userId);
            action.setActionType(2);
            action.setCreateTime(new Date());
            actionMapper.insert(action);
        });
    }
}
```

- [ ] **Step 2: 实现点赞/取消点赞**

替换 `likeVideo` 和 `unlikeVideo` 方法:

```java
@Override
public boolean likeVideo(Long videoId) {
    Long userId = LoginHelper.getUserId();
    synchronized (getUserLock(userId)) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            // 检查是否已点赞
            Long count = actionMapper.selectCount(
                new LambdaQueryWrapper<MediaVideoAction>()
                    .eq(MediaVideoAction::getVideoId, videoId)
                    .eq(MediaVideoAction::getUserId, userId)
                    .eq(MediaVideoAction::getActionType, 1));
            if (count > 0) {
                throw new ServiceException("已点赞");
            }
            // like_count +1
            videoMapper.update(null,
                new LambdaQueryWrapper<MediaVideo>()
                    .eq(MediaVideo::getVideoId, videoId)
                    .setSql("like_count = like_count + 1"));
            // 插入点赞记录
            MediaVideoAction action = new MediaVideoAction();
            action.setVideoId(videoId);
            action.setUserId(userId);
            action.setActionType(1);
            action.setCreateTime(new Date());
            actionMapper.insert(action);
            return true;
        }));
    }
}

@Override
public boolean unlikeVideo(Long videoId) {
    Long userId = LoginHelper.getUserId();
    synchronized (getUserLock(userId)) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            // 查找点赞记录
            MediaVideoAction action = actionMapper.selectOne(
                new LambdaQueryWrapper<MediaVideoAction>()
                    .eq(MediaVideoAction::getVideoId, videoId)
                    .eq(MediaVideoAction::getUserId, userId)
                    .eq(MediaVideoAction::getActionType, 1));
            if (action == null) {
                throw new ServiceException("未点赞");
            }
            // like_count -1
            videoMapper.update(null,
                new LambdaQueryWrapper<MediaVideo>()
                    .eq(MediaVideo::getVideoId, videoId)
                    .setSql("like_count = like_count - 1"));
            // 删除点赞记录
            actionMapper.deleteById(action.getId());
            return true;
        }));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/service/impl/MediaVideoServiceImpl.java
git commit -m "feat(media): 实现 Range 流式播放 + 点赞/取消点赞"
```

---

### Task 9: 附件/分享 Service

**Files:**
- Create: `service/IMediaVideoAttachmentService.java` + impl
- Create: `service/IMediaVideoShareService.java` + impl

- [ ] **Step 1: 创建附件 Service**

**IMediaVideoAttachmentService.java:**
```java
package com.hmoob.media.service;

import com.hmoob.media.domain.vo.MediaVideoAttachmentVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 视频附件服务接口
 */
public interface IMediaVideoAttachmentService {

    /**
     * 上传附件
     *
     * @param videoId 视频ID
     * @param file    附件文件
     * @return 附件ID
     */
    Long uploadAttachment(Long videoId, MultipartFile file);

    /**
     * 查询视频附件列表
     *
     * @param videoId 视频ID
     * @return 附件列表
     */
    List<MediaVideoAttachmentVo> selectAttachments(Long videoId);

    /**
     * 下载附件
     *
     * @param attachmentId 附件ID
     * @return 文件输入流
     */
    InputStream downloadAttachment(Long attachmentId);
}
```

**MediaVideoAttachmentServiceImpl.java:**
```java
package com.hmoob.media.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.oss.core.OssClient;
import com.hmoob.common.oss.entity.UploadResult;
import com.hmoob.common.oss.factory.OssFactory;
import com.hmoob.media.domain.MediaVideoAttachment;
import com.hmoob.media.domain.vo.MediaVideoAttachmentVo;
import com.hmoob.media.mapper.MediaVideoAttachmentMapper;
import com.hmoob.media.service.IMediaVideoAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MediaVideoAttachmentServiceImpl implements IMediaVideoAttachmentService {

    private final MediaVideoAttachmentMapper baseMapper;

    @Override
    public Long uploadAttachment(Long videoId, MultipartFile file) {
        try {
            OssClient ossClient = OssFactory.instance();
            UploadResult result = ossClient.uploadSuffix(
                file.getBytes(), file.getOriginalFilename(), file.getContentType());

            MediaVideoAttachment attachment = new MediaVideoAttachment();
            attachment.setVideoId(videoId);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setFilePath(result.getUrl());
            attachment.setFileSize(file.getSize());
            attachment.setFileFormat(getFileExtension(file.getOriginalFilename()));
            baseMapper.insert(attachment);
            return attachment.getAttachmentId();
        } catch (IOException e) {
            throw new ServiceException("附件上传失败：" + e.getMessage());
        }
    }

    @Override
    public List<MediaVideoAttachmentVo> selectAttachments(Long videoId) {
        return baseMapper.selectVoList(
            new LambdaQueryWrapper<MediaVideoAttachment>()
                .eq(MediaVideoAttachment::getVideoId, videoId));
    }

    @Override
    public InputStream downloadAttachment(Long attachmentId) {
        MediaVideoAttachment attachment = baseMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new ServiceException("附件不存在");
        }
        try {
            return OssFactory.instance().getObjectContent(attachment.getFilePath());
        } catch (IOException e) {
            throw new ServiceException("附件下载失败");
        }
    }

    private String getFileExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) return "";
        int idx = fileName.lastIndexOf(".");
        if (idx == -1 || idx == fileName.length() - 1) return "";
        return fileName.substring(idx + 1).toLowerCase();
    }
}
```

- [ ] **Step 2: 创建分享 Service**

**IMediaVideoShareService.java:**
```java
package com.hmoob.media.service;

import com.hmoob.media.domain.bo.MediaVideoShareBo;
import com.hmoob.media.domain.vo.MediaVideoShareVo;

/**
 * 视频分享服务接口
 */
public interface IMediaVideoShareService {

    /**
     * 创建分享链接
     */
    MediaVideoShareVo createShare(MediaVideoShareBo bo);

    /**
     * 通过分享令牌访问
     */
    MediaVideoShareVo accessByToken(String shareToken);
}
```

**MediaVideoShareServiceImpl.java:**
```java
package com.hmoob.media.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.media.domain.MediaVideoShare;
import com.hmoob.media.domain.bo.MediaVideoShareBo;
import com.hmoob.media.domain.vo.MediaVideoShareVo;
import com.hmoob.media.mapper.MediaVideoShareMapper;
import com.hmoob.media.service.IMediaVideoShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@RequiredArgsConstructor
@Service
public class MediaVideoShareServiceImpl implements IMediaVideoShareService {

    private final MediaVideoShareMapper baseMapper;

    @Override
    public MediaVideoShareVo createShare(MediaVideoShareBo bo) {
        MediaVideoShare share = new MediaVideoShare();
        share.setVideoId(bo.getVideoId());
        share.setShareToken(IdUtil.fastSimpleUUID());
        share.setExpireTime(bo.getExpireTime());
        share.setMaxAccess(bo.getMaxAccess() != null ? bo.getMaxAccess() : 0);
        share.setAccessCount(0);
        baseMapper.insert(share);

        MediaVideoShareVo vo = new MediaVideoShareVo();
        vo.setId(share.getId());
        vo.setShareToken(share.getShareToken());
        vo.setShareUrl("/media/video/share/" + share.getShareToken());
        return vo;
    }

    @Override
    public MediaVideoShareVo accessByToken(String shareToken) {
        MediaVideoShare share = baseMapper.selectOne(
            new LambdaQueryWrapper<MediaVideoShare>()
                .eq(MediaVideoShare::getShareToken, shareToken));
        if (share == null) {
            throw new ServiceException("分享链接不存在");
        }
        if (share.getExpireTime() != null && share.getExpireTime().before(new Date())) {
            throw new ServiceException("分享链接已过期");
        }
        if (share.getMaxAccess() > 0 && share.getAccessCount() >= share.getMaxAccess()) {
            throw new ServiceException("分享链接访问次数已达上限");
        }
        // 访问计数 +1
        share.setAccessCount(share.getAccessCount() + 1);
        baseMapper.updateById(share);

        MediaVideoShareVo vo = new MediaVideoShareVo();
        vo.setId(share.getId());
        vo.setShareToken(share.getShareToken());
        return vo;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/service/
git commit -m "feat(media): 实现附件和分享 Service"
```

---

### Task 10: Controller 层

**Files:**
- Create: `controller/MediaVideoController.java`
- Create: `controller/MediaVideoAuthController.java`

- [ ] **Step 1: 创建 MediaVideoController.java**

```java
package com.hmoob.media.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.media.domain.bo.*;
import com.hmoob.media.domain.vo.*;
import com.hmoob.media.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 视频管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/media/video")
public class MediaVideoController extends BaseController {

    private final IMediaVideoService videoService;
    private final IMediaVideoAttachmentService attachmentService;
    private final IMediaVideoShareService shareService;

    /**
     * 上传视频
     */
    @SaCheckPermission("media:video:add")
    @PostMapping("/upload")
    public R<Long> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
        MediaVideoUploadBo bo) {
        return R.ok(videoService.uploadVideo(file, thumbnail, bo));
    }

    /**
     * 分页查询视频列表
     */
    @GetMapping("/page")
    public TableDataInfo<MediaVideoVo> page(MediaVideoQuery query) {
        return videoService.selectVideoPage(query);
    }

    /**
     * 查询视频详情
     */
    @GetMapping("/{videoId}")
    public R<MediaVideoDetailVo> detail(@PathVariable Long videoId) {
        return R.ok(videoService.selectVideoDetail(videoId));
    }

    /**
     * 删除视频
     */
    @SaCheckPermission("media:video:remove")
    @DeleteMapping("/{videoId}")
    public R<Void> remove(@PathVariable Long videoId) {
        return toAjax(videoService.deleteVideo(videoId));
    }

    /**
     * Range 流式播放
     */
    @GetMapping("/play/{videoId}")
    public void play(@PathVariable Long videoId,
                     HttpServletRequest request,
                     HttpServletResponse response) {
        videoService.playVideo(videoId, request, response);
    }

    /**
     * 点赞
     */
    @PostMapping("/like")
    public R<Boolean> like(@Validated @RequestBody MediaVideoLikeBo bo) {
        return R.ok(videoService.likeVideo(bo.getVideoId()));
    }

    /**
     * 取消点赞
     */
    @PostMapping("/unlike")
    public R<Boolean> unlike(@Validated @RequestBody MediaVideoLikeBo bo) {
        return R.ok(videoService.unlikeVideo(bo.getVideoId()));
    }

    /**
     * 创建分享链接
     */
    @PostMapping("/share")
    public R<MediaVideoShareVo> share(@Validated @RequestBody MediaVideoShareBo bo) {
        return R.ok(shareService.createShare(bo));
    }

    /**
     * 通过分享令牌访问
     */
    @GetMapping("/share/{shareToken}")
    public R<MediaVideoShareVo> accessShare(@PathVariable String shareToken) {
        return R.ok(shareService.accessByToken(shareToken));
    }

    /**
     * 查询附件列表
     */
    @GetMapping("/{videoId}/attachments")
    public R<List<MediaVideoAttachmentVo>> attachments(@PathVariable Long videoId) {
        return R.ok(attachmentService.selectAttachments(videoId));
    }

    /**
     * 上传附件
     */
    @SaCheckPermission("media:video:add")
    @PostMapping("/attachment")
    public R<Long> uploadAttachment(
        @RequestParam("file") MultipartFile file,
        @RequestParam("videoId") Long videoId) {
        return R.ok(attachmentService.uploadAttachment(videoId, file));
    }

    /**
     * 下载附件
     */
    @GetMapping("/attachment/{attachmentId}/download")
    public void downloadAttachment(@PathVariable Long attachmentId,
                                   HttpServletResponse response) {
        try (InputStream is = attachmentService.downloadAttachment(attachmentId);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
```

- [ ] **Step 2: 创建 MediaVideoAuthController.java**

```java
package com.hmoob.media.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.media.domain.bo.MediaVideoAuthBo;
import com.hmoob.media.domain.vo.MediaVideoAuthVo;
import com.hmoob.media.service.IMediaVideoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 视频授权管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/media/auth")
public class MediaVideoAuthController extends BaseController {

    private final IMediaVideoAuthService authService;

    /**
     * 新增授权
     */
    @SaCheckPermission("media:auth:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody MediaVideoAuthBo bo) {
        return toAjax(authService.insertAuth(bo));
    }

    /**
     * 删除授权
     */
    @SaCheckPermission("media:auth:remove")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(authService.deleteAuth(id));
    }

    /**
     * 查询视频授权列表
     */
    @GetMapping("/{videoId}")
    public TableDataInfo<MediaVideoAuthVo> list(@PathVariable Long videoId, PageQuery pageQuery) {
        return authService.selectAuthPage(videoId, pageQuery);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-modules/ruoyi-media/src/main/java/com/hmoob/media/controller/
git commit -m "feat(media): 实现所有 Controller 层"
```

---

### Task 11: 编译验证

- [ ] **Step 1: 执行 Maven 编译**

```bash
cd C:/Users/yanx/Desktop/ai-coding/sutra-doc/RuoYi-Vue-Plus
mvn compile -pl ruoyi-modules/ruoyi-media -am -DskipTests 2>&1 | tail -30
```

Expected: `BUILD SUCCESS`

如果有编译错误，根据错误信息修复后重新编译。

- [ ] **Step 2: 最终 Commit**

```bash
git add -A ruoyi-modules/ruoyi-media/
git commit -m "feat(media): 视频媒体模块完整实现"
```
