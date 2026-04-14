package com.hmoob.doc.service;

import com.hmoob.doc.domain.bo.KbDocCommentsBo;
import com.hmoob.doc.domain.vo.KbDocCommentsVo;

import java.util.List;

/**
 * KB文档评论 服务层
 *
 * @author hmoob
 */
public interface IKbDocCommentsService {

    /**
     * 根据文档ID查询评论列表(树形结构)
     * 包含一级评论及其回复
     *
     * @param docId 文档ID
     * @return 评论树列表
     */
    List<KbDocCommentsVo> selectCommentsByDocId(Long docId);

    /**
     * 根据评论ID查询评论信息
     *
     * @param commentId 评论ID
     * @return 评论信息
     */
    KbDocCommentsVo selectCommentById(Long commentId);

    /**
     * 根据父评论ID查询回复列表
     *
     * @param parentId 父评论ID
     * @return 回复列表
     */
    List<KbDocCommentsVo> selectRepliesByParentId(Long parentId);

    /**
     * 新增评论
     *
     * @param bo 评论业务对象
     * @return 影响行数
     */
    int insertComment(KbDocCommentsBo bo);

    /**
     * 删除评论
     * 级联删除所有子评论(回复)
     *
     * @param commentId 评论ID
     * @return 影响行数
     */
    int deleteComment(Long commentId);

    /**
     * 点赞评论
     * 增加点赞计数
     *
     * @param commentId 评论ID
     * @return 影响行数
     */
    int likeComment(Long commentId);

    /**
     * 更新评论状态
     *
     * @param commentId 评论ID
     * @param status    状态(1-正常, 0-已删除, 2-待审核)
     * @return 影响行数
     */
    int updateCommentStatus(Long commentId, Integer status);

}