package com.hmoob.doc.domain.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hmoob.common.mybatis.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.util.Date;

/**
 * KB文档日志查询对象
 * 用于日志列表的分页查询
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KbDocLogQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 默认构造器
     */
    public KbDocLogQuery() {
        super(10, 1);
    }

    /**
     * 创建时间起始
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeStart;

    /**
     * 创建时间截止
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeEnd;

    /**
     * 操作类型: 1-上传, 2-预览, 3-下载, 4-删除, 5-编辑, 6-分享
     */
    private Integer visitType;

    /**
     * 操作人ID
     */
    private Long userId;

    /**
     * 操作人名称（模糊查询）
     */
    private String userName;

}