package com.hmoob.media.service;

import com.hmoob.media.domain.bo.MediaVideoShareBo;
import com.hmoob.media.domain.vo.MediaVideoShareVo;

/**
 * 视频分享服务接口
 *
 * @author hmoob
 */
public interface IMediaVideoShareService {

    /**
     * 创建分享链接
     *
     * @param bo 分享参数
     * @return 分享信息
     */
    MediaVideoShareVo createShare(MediaVideoShareBo bo);

    /**
     * 通过分享令牌访问
     *
     * @param shareToken 分享令牌
     * @return 分享信息
     */
    MediaVideoShareVo accessByToken(String shareToken);
}
