package com.hmoob.media.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.media.domain.MediaVideo;

import java.io.Serial;

/**
 * 视频信息业务对象 media_video
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MediaVideo.class, reverseConvertGenerate = false)
public class MediaVideoBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频描述
     */
    private String description;

    /**
     * 授权类型: 0-公开, 1-指定用户/角色
     */
    private Integer authType;

    /**
     * 状态: 0-待审核, 1-已发布, 2-已下架
     */
    private Integer status;

}
