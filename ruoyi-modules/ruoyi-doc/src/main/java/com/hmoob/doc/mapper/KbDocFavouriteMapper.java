package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbDocFavourite;
import com.hmoob.doc.domain.vo.KbDocFavouriteVo;

import java.util.List;

/**
 * KB文档收藏 Mapper接口
 *
 * @author hmoob
 */
public interface KbDocFavouriteMapper extends BaseMapperPlus<KbDocFavourite, KbDocFavouriteVo> {

    /**
     * 根据用户ID查询收藏列表
     *
     * @param userId 用户ID
     * @return 收藏列表
     */
    default List<KbDocFavouriteVo> selectByUserId(Long userId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocFavourite>()
            .eq(KbDocFavourite::getUserId, userId)
            .orderByDesc(KbDocFavourite::getCreateTime));
    }

    /**
     * 查询用户是否收藏了某文档
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 收藏Vo对象
     */
    default KbDocFavouriteVo selectByDocIdAndUserId(Long docId, Long userId) {
        return this.selectVoOne(new LambdaQueryWrapper<KbDocFavourite>()
            .eq(KbDocFavourite::getDocId, docId)
            .eq(KbDocFavourite::getUserId, userId));
    }

}