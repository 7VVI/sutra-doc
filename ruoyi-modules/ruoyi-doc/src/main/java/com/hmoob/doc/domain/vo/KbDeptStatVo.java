package com.hmoob.doc.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * KB部门文件明细统计视图对象
 *
 * @author hmoob
 */
@Data
public class KbDeptStatVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 文件总数
     */
    private Long fileCount;

    /**
     * 比上周新增数量
     */
    private Long weeklyNewCount;

    /**
     * 最新新增文件名
     */
    private String latestFileName;

}
