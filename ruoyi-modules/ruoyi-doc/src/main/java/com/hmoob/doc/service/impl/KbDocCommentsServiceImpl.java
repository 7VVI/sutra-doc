package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.doc.domain.KbDocComments;
import com.hmoob.doc.domain.bo.KbDocCommentsBo;
import com.hmoob.doc.domain.vo.KbDocCommentsVo;
import com.hmoob.doc.mapper.KbDocCommentsMapper;
import com.hmoob.doc.service.IKbDocCommentsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * KB文档评论 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbDocCommentsServiceImpl implements IKbDocCommentsService {

    private final KbDocCommentsMapper baseMapper;

    /**
     * 根据文档ID查询评论列表(树形结构)
     * 包含一级评论及其回复
     *
     * @param docId 文档ID
     * @return 评论树列表
     */
    @Override
    public List<KbDocCommentsVo> selectCommentsByDocId(Long docId) {
        // 查询一级评论
        List<KbDocCommentsVo> comments = baseMapper.selectCommentsByDocId(docId);
        // 查询每个一级评论的回复
        for (KbDocCommentsVo comment : comments) {
            List<KbDocCommentsVo> replies = baseMapper.selectRepliesByParentId(comment.getCommentId());
            comment.setChildren(replies);
        }
        return comments;
    }

    /**
     * 根据评论ID查询评论信息
     *
     * @param commentId 评论ID
     * @return 评论信息
     */
    @Override
    public KbDocCommentsVo selectCommentById(Long commentId) {
        return baseMapper.selectVoById(commentId);
    }

    /**
     * 根据父评论ID查询回复列表
     *
     * @param parentId 父评论ID
     * @return 回复列表
     */
    @Override
    public List<KbDocCommentsVo> selectRepliesByParentId(Long parentId) {
        return baseMapper.selectRepliesByParentId(parentId);
    }

    /**
     * 新增评论
     *
     * @param bo 评论业务对象
     * @return 影响行数
     */
    @Override
    public int insertComment(KbDocCommentsBo bo) {
        KbDocComments comment = MapstructUtils.convert(bo, KbDocComments.class);
        // 设置默认值
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0L);
        }
        if (comment.getIsAnonymous() == null) {
            comment.setIsAnonymous(0);
        }
        if (comment.getStatus() == null) {
            comment.setStatus(1);
        }
        return baseMapper.insert(comment);
    }

    /**
     * 删除评论
     * 级联删除所有子评论(回复)
     *
     * @param commentId 评论ID
     * @return 影响行数
     */
    @Override
    public int deleteComment(Long commentId) {
        // 校验评论是否存在
        KbDocCommentsVo comment = selectCommentById(commentId);
        if (ObjectUtil.isNull(comment)) {
            throw new ServiceException("评论不存在");
        }
        // 删除子评论(回复)
        deleteRepliesRecursively(commentId);
        // 删除当前评论
        return baseMapper.deleteById(commentId);
    }

    /**
     * 点赞评论
     * 增加点赞计数
     *
     * @param commentId 评论ID
     * @return 影响行数
     */
    @Override
    public int likeComment(Long commentId) {
        // 校验评论是否存在
        KbDocCommentsVo comment = selectCommentById(commentId);
        if (ObjectUtil.isNull(comment)) {
            throw new ServiceException("评论不存在");
        }
        // 更新点赞数
        LambdaUpdateWrapper<KbDocComments> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(KbDocComments::getCommentId, commentId);
        updateWrapper.set(KbDocComments::getLikeCount, comment.getLikeCount() + 1);
        return baseMapper.update(null, updateWrapper);
    }

    /**
     * 更新评论状态
     *
     * @param commentId 评论ID
     * @param status    状态(1-正常, 0-已删除, 2-待审核)
     * @return 影响行数
     */
    @Override
    public int updateCommentStatus(Long commentId, Integer status) {
        // 校验评论是否存在
        KbDocCommentsVo comment = selectCommentById(commentId);
        if (ObjectUtil.isNull(comment)) {
            throw new ServiceException("评论不存在");
        }
        // 更新状态
        LambdaUpdateWrapper<KbDocComments> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(KbDocComments::getCommentId, commentId);
        updateWrapper.set(KbDocComments::getStatus, status);
        return baseMapper.update(null, updateWrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 递归删除评论的所有回复
     *
     * @param parentId 父评论ID
     */
    private void deleteRepliesRecursively(Long parentId) {
        List<KbDocCommentsVo> replies = baseMapper.selectRepliesByParentId(parentId);
        for (KbDocCommentsVo reply : replies) {
            // 递归删除子回复
            deleteRepliesRecursively(reply.getCommentId());
            // 删除当前回复
            baseMapper.deleteById(reply.getCommentId());
        }
    }

}