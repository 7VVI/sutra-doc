package com.hmoob.doc.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 关键词统计Vo对象
 *
 * @author hmoob
 */
@Data
public class KeywordStatVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键词
     */
    private String keywords;

    /**
     * 搜索次数
     */
    private Long searchCount;

    /**
     * 平均结果数量
     */
    private Double avgResultCount;

}