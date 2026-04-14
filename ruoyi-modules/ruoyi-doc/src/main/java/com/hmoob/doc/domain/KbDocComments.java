package com.hmoob.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * KB文档评论实体类
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_doc_comments")
public class KbDocComments extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "comment_id")
    private Long commentId;

    /**
     * 文档ID
     */
    private Long docId;

    /**
     * 父评论ID(用于回复,0表示一级评论)
     */
    private Long parentId;

    /**
     * 回复目标用户ID
     */
    private Long replyToUserId;

    /**
     * 评论内容
     */
    private String commentContent;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 是否匿名: 0-否, 1-是
     */
    private Integer isAnonymous;

    /**
     * 状态: 1-正常, 0-已删除, 2-待审核
     */
    private Integer status;

    /**
     * 删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    private String delFlag;

}