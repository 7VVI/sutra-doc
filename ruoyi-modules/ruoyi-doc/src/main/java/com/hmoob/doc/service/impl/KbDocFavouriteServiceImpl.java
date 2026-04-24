package com.hmoob.doc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.doc.domain.KbDocFavourite;
import com.hmoob.doc.domain.bo.KbDocFavouriteBo;
import com.hmoob.doc.domain.vo.KbDocFavouriteVo;
import com.hmoob.doc.mapper.KbDocFavouriteMapper;
import com.hmoob.doc.service.IKbDocFavouriteService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * KB文档收藏 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbDocFavouriteServiceImpl implements IKbDocFavouriteService {

    private final KbDocFavouriteMapper baseMapper;

    /**
     * 根据用户ID查询收藏列表
     *
     * @param userId 用户ID
     * @return 收藏列表
     */
    @Override
    public List<KbDocFavouriteVo> selectFavouritesByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    /**
     * 根据收藏ID查询收藏信息
     *
     * @param favouriteId 收藏ID
     * @return 收藏信息
     */
    @Override
    public KbDocFavouriteVo selectFavouriteById(Long favouriteId) {
        return baseMapper.selectVoById(favouriteId);
    }

    /**
     * 添加收藏
     *
     * @param bo 收藏业务对象
     * @return 影响行数
     */
    @Override
    public int addFavourite(KbDocFavouriteBo bo) {
        // 检查是否已收藏
        if (checkFavourite(bo.getDocId(), bo.getUserId())) {
            throw new ServiceException("已收藏该文档");
        }
        KbDocFavourite favourite = MapstructUtils.convert(bo, KbDocFavourite.class);
        return baseMapper.insert(favourite);
    }

    /**
     * 移除收藏
     *
     * @param favouriteId 收藏ID
     * @return 影响行数
     */
    @Override
    public int removeFavourite(Long favouriteId) {
        // 校验收藏是否存在
        KbDocFavouriteVo favourite = selectFavouriteById(favouriteId);
        if (ObjectUtil.isNull(favourite)) {
            throw new ServiceException("收藏不存在");
        }
        return baseMapper.deleteById(favouriteId);
    }

    /**
     * 根据文档ID和用户ID移除收藏
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Override
    public int removeFavouriteByDocId(Long docId, Long userId) {
        // 查询收藏记录
        KbDocFavouriteVo favourite = baseMapper.selectByDocIdAndUserId(docId, userId);
        if (ObjectUtil.isNull(favourite)) {
            throw new ServiceException("未收藏该文档");
        }
        return baseMapper.deleteById(favourite.getFavouriteId());
    }

    /**
     * 检查用户是否收藏了指定文档
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 是否已收藏
     */
    @Override
    public boolean checkFavourite(Long docId, Long userId) {
        KbDocFavouriteVo favourite = baseMapper.selectByDocIdAndUserId(docId, userId);
        return ObjectUtil.isNotNull(favourite);
    }

    /**
     * 获取用户对指定文档的收藏信息
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 收藏信息(未收藏返回null)
     */
    @Override
    public KbDocFavouriteVo getFavouriteInfo(Long docId, Long userId) {
        return baseMapper.selectByDocIdAndUserId(docId, userId);
    }

    /**
     * 获取用户在指定文档中已收藏的文档ID集合
     *
     * @param userId 用户ID
     * @param docIds 文档ID列表
     * @return 已收藏的文档ID集合
     */
    @Override
    public Set<Long> getFavouriteDocIds(Long userId, List<Long> docIds) {
        if (userId == null || CollUtil.isEmpty(docIds)) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<KbDocFavourite> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KbDocFavourite::getDocId, docIds)
            .eq(KbDocFavourite::getUserId, userId)
            .eq(KbDocFavourite::getDelFlag, "0");
        List<KbDocFavourite> favourites = baseMapper.selectList(wrapper);
        return favourites.stream()
            .map(KbDocFavourite::getDocId)
            .collect(Collectors.toSet());
    }

}