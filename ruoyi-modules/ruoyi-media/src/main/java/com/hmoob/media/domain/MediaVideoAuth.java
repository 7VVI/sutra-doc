package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hmoob.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * 视频授权实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("media_video_auth")
public class MediaVideoAuth extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 授权ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 授权对象类型: 1-用户, 2-角色
     */
    private Integer authTargetType;

    /**
     * 授权对象ID
     */
    private Long authTargetId;

    /**
     * 授权对象名称
     */
    private String authTargetName;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    private String delFlag;

}
