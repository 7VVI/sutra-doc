package com.hmoob.doc.service;

import com.hmoob.doc.domain.bo.KbDocFavouriteBo;
import com.hmoob.doc.domain.vo.KbDocFavouriteVo;

import java.util.List;

/**
 * KB文档收藏 服务层
 *
 * @author hmoob
 */
public interface IKbDocFavouriteService {

    /**
     * 根据用户ID查询收藏列表
     *
     * @param userId 用户ID
     * @return 收藏列表
     */
    List<KbDocFavouriteVo> selectFavouritesByUserId(Long userId);

    /**
     * 根据收藏ID查询收藏信息
     *
     * @param favouriteId 收藏ID
     * @return 收藏信息
     */
    KbDocFavouriteVo selectFavouriteById(Long favouriteId);

    /**
     * 添加收藏
     *
     * @param bo 收藏业务对象
     * @return 影响行数
     */
    int addFavourite(KbDocFavouriteBo bo);

    /**
     * 移除收藏
     *
     * @param favouriteId 收藏ID
     * @return 影响行数
     */
    int removeFavourite(Long favouriteId);

    /**
     * 根据文档ID和用户ID移除收藏
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 影响行数
     */
    int removeFavouriteByDocId(Long docId, Long userId);

    /**
     * 检查用户是否收藏了指定文档
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 是否已收藏
     */
    boolean checkFavourite(Long docId, Long userId);

    /**
     * 获取用户对指定文档的收藏信息
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 收藏信息(未收藏返回null)
     */
    KbDocFavouriteVo getFavouriteInfo(Long docId, Long userId);

}