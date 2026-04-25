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

/**
 * 视频分享服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class MediaVideoShareServiceImpl implements IMediaVideoShareService {

    private final MediaVideoShareMapper baseMapper;

    /**
     * 创建分享链接
     *
     * @param bo 分享参数
     * @return 分享信息
     */
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

    /**
     * 通过分享令牌访问
     *
     * @param shareToken 分享令牌
     * @return 分享信息
     */
    @Override
    public MediaVideoShareVo accessByToken(String shareToken) {
        MediaVideoShare share = baseMapper.selectOne(new LambdaQueryWrapper<MediaVideoShare>()
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
        share.setAccessCount(share.getAccessCount() + 1);
        baseMapper.updateById(share);
        MediaVideoShareVo vo = new MediaVideoShareVo();
        vo.setId(share.getId());
        vo.setShareToken(share.getShareToken());
        return vo;
    }
}
