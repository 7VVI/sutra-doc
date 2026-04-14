package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.lang.tree.Tree;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.core.validate.AddGroup;
import com.hmoob.common.core.validate.EditGroup;
import com.hmoob.common.idempotent.annotation.RepeatSubmit;
import com.hmoob.common.log.annotation.Log;
import com.hmoob.common.log.enums.BusinessType;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.bo.KbDocCommentsBo;
import com.hmoob.doc.domain.vo.KbDocCommentsVo;
import com.hmoob.doc.service.IKbDocCommentsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KB文档评论管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/comments")
public class KbDocCommentsController extends BaseController {

    private final IKbDocCommentsService commentsService;

    /**
     * 查询评论列表(树形结构)
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:comments:list")
    @GetMapping("/list/{docId}")
    public R<List<KbDocCommentsVo>> list(@NotNull(message = "文档ID不能为空") @PathVariable Long docId) {
        return R.ok(commentsService.selectCommentsByDocId(docId));
    }

    /**
     * 获取评论详细信息
     *
     * @param commentId 评论ID
     */
    @SaCheckPermission("kb:comments:query")
    @GetMapping("/{commentId}")
    public R<KbDocCommentsVo> getInfo(@NotNull(message = "评论ID不能为空") @PathVariable Long commentId) {
        return R.ok(commentsService.selectCommentById(commentId));
    }

    /**
     * 获取评论的回复列表
     *
     * @param parentId 父评论ID
     */
    @SaCheckPermission("kb:comments:list")
    @GetMapping("/replies/{parentId}")
    public R<List<KbDocCommentsVo>> getReplies(@PathVariable Long parentId) {
        return R.ok(commentsService.selectRepliesByParentId(parentId));
    }

    /**
     * 新增评论
     */
    @SaCheckPermission("kb:comments:add")
    @Log(title = "评论管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KbDocCommentsBo bo) {
        return toAjax(commentsService.insertComment(bo));
    }

    /**
     * 回复评论
     */
    @SaCheckPermission("kb:comments:add")
    @Log(title = "评论管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/reply")
    public R<Void> reply(@Validated(AddGroup.class) @RequestBody KbDocCommentsBo bo) {
        return toAjax(commentsService.insertComment(bo));
    }

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     */
    @SaCheckPermission("kb:comments:remove")
    @Log(title = "评论管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{commentId}")
    public R<Void> remove(@PathVariable Long commentId) {
        return toAjax(commentsService.deleteComment(commentId));
    }

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     */
    @SaCheckPermission("kb:comments:like")
    @Log(title = "评论管理", businessType = BusinessType.OTHER)
    @PostMapping("/like/{commentId}")
    public R<Void> like(@PathVariable Long commentId) {
        return toAjax(commentsService.likeComment(commentId));
    }

    /**
     * 更新评论状态
     *
     * @param commentId 评论ID
     * @param status    状态
     */
    @SaCheckPermission("kb:comments:edit")
    @Log(title = "评论管理", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{commentId}/{status}")
    public R<Void> updateStatus(
        @PathVariable Long commentId,
        @PathVariable Integer status) {
        return toAjax(commentsService.updateCommentStatus(commentId, status));
    }

}