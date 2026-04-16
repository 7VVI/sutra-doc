package com.hmoob.doc.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 搜索统计Vo对象
 *
 * @author hmoob
 */
@Data
public class SearchStatVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 热门关键词列表
     */
    private List<String> hotKeywords;

    /**
     * 总搜索次数
     */
    private Long totalSearchCount;

    /**
     * 今日搜索次数
     */
    private Long todaySearchCount;

}