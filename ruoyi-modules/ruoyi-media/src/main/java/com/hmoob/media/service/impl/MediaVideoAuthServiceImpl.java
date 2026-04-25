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
        List<Long> result = new ArrayList<>();
        if (deptId != null) {
            List<MediaVideoAuth> orgAuths = baseMapper.selectList(
                new LambdaQueryWrapper<MediaVideoAuth>()
                    .eq(MediaVideoAuth::getAuthTargetType, 1)
                    .eq(MediaVideoAuth::getAuthTargetId, deptId));
            result.addAll(orgAuths.stream().map(MediaVideoAuth::getVideoId).collect(Collectors.toList()));
        }
        List<MediaVideoAuth> userAuths = baseMapper.selectList(
            new LambdaQueryWrapper<MediaVideoAuth>()
                .eq(MediaVideoAuth::getAuthTargetType, 2)
                .eq(MediaVideoAuth::getAuthTargetId, userId)
                .and(w -> w.isNull(MediaVideoAuth::getExpireTime)
                    .or().gt(MediaVideoAuth::getExpireTime, new Date())));
        result.addAll(userAuths.stream().map(MediaVideoAuth::getVideoId).collect(Collectors.toList()));
        return result.stream().distinct().collect(Collectors.toList());
    }
}
