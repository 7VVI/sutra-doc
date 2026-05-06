package com.hmoob.media.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hmoob.common.tenant.core.TenantEntity;
import com.hmoob.media.domain.vo.MediaVideoVo;
import io.github.linpeilie.annotations.AutoMapper;
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

    /**
     * 租户编号
     */
    @TableField(exist = false)
    private String tenantId;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE,exist = false)
    private Date updateTime;

    /**
     * 创建部门
     */
    @TableField(fill = FieldFill.INSERT,exist = false)
    private Long createDept;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE,exist = false)
    private Long updateBy;
}
