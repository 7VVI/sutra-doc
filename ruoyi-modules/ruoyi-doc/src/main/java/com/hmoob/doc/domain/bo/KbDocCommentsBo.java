package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbDocComments;

import java.io.Serial;

/**
 * KB文档评论业务对象 kb_doc_comments
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbDocComments.class, reverseConvertGenerate = false)
public class KbDocCommentsBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
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
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容长度不能超过{max}个字符")
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

}