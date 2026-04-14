package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbDocComments;
import com.hmoob.doc.domain.vo.KbDocCommentsVo;

import java.util.List;

/**
 * KB文档评论 Mapper接口
 *
 * @author hmoob
 */
public interface KbDocCommentsMapper extends BaseMapperPlus<KbDocComments, KbDocCommentsVo> {

    /**
     * 根据文档ID查询评论列表(一级评论)
     *
     * @param docId 文档ID
     * @return 评论列表
     */
    default List<KbDocCommentsVo> selectCommentsByDocId(Long docId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocComments>()
            .eq(KbDocComments::getDocId, docId)
            .eq(KbDocComments::getParentId, 0)
            .eq(KbDocComments::getStatus, 1)
            .orderByDesc(KbDocComments::getCreateTime));
    }

    /**
     * 根据父评论ID查询回复列表
     *
     * @param parentId 父评论ID
     * @return 回复列表
     */
    default List<KbDocCommentsVo> selectRepliesByParentId(Long parentId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDocComments>()
            .eq(KbDocComments::getParentId, parentId)
            .eq(KbDocComments::getStatus, 1)
            .orderByAsc(KbDocComments::getCreateTime));
    }

}