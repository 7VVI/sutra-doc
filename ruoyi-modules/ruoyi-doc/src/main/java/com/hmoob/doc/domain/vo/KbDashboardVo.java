package com.hmoob.doc.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * KB首页卡片统计视图对象
 *
 * @author hmoob
 */
@Data
public class KbDashboardVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件总数
     */
    private Long fileTotal;

    /**
     * 文件总数环比上月增长百分比
     */
    private Double fileTotalGrowthRate;

    /**
     * 本周新增文件数
     */
    private Long weeklyNewFiles;

    /**
     * 本周新增环比上周增长百分比
     */
    private Double weeklyGrowthRate;

    /**
     * 活跃部门数量(有文档的部门数)
     */
    private Long activeDeptCount;

    /**
     * 今日检索次数累计
     */
    private Long todaySearchCount;

}
