package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbDocComments;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * KB文档评论视图对象 kb_doc_comments
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbDocComments.class)
public class KbDocCommentsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @ExcelProperty(value = "评论ID")
    private Long commentId;

    /**
     * 文档ID
     */
    @ExcelProperty(value = "文档ID")
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
    @ExcelProperty(value = "评论内容")
    private String commentContent;

    /**
     * 点赞数
     */
    @ExcelProperty(value = "点赞数")
    private Long likeCount;

    /**
     * 是否匿名: 0-否, 1-是
     */
    @ExcelProperty(value = "是否匿名")
    private Integer isAnonymous;

    /**
     * 状态: 1-正常, 0-已删除, 2-待审核
     */
    @ExcelProperty(value = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 子评论列表(用于回复树形结构)
     */
    private List<KbDocCommentsVo> children = new ArrayList<>();

}