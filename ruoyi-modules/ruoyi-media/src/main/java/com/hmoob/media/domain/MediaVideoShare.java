package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hmoob.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * 视频分享实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_video_share")
public class MediaVideoShare extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分享ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 分享令牌
     */
    private String shareToken;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 已访问次数
     */
    private Integer accessCount;

    /**
     * 最大访问次数
     */
    private Integer maxAccess;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    private String delFlag;

    /**
     * 租户编号
     */
    @TableField(exist = false)
    private String tenantId;
}
