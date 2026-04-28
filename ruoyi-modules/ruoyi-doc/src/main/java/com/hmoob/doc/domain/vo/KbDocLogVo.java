package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档日志视图对象
 * 用于日志列表展示和导出
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
public class KbDocLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private Long visitId;

    /**
     * 序号（前端展示用）
     */
    private Integer rowNum;

    /**
     * 操作类型: 1-上传, 2-预览, 3-下载, 4-删除, 5-编辑, 6-分享
     */
    @ExcelProperty(value = "操作类型")
    private Integer visitType;

    /**
     * 操作类型名称
     */
    @ExcelProperty(value = "操作类型")
    private String visitTypeText;

    /**
     * 操作人ID
     */
    private Long userId;

    /**
     * 操作人名称
     */
    @ExcelProperty(value = "操作人")
    private String userName;

    /**
     * 关键字/搜索关键词
     */
    @ExcelProperty(value = "关键字")
    private String keywords;

    /**
     * IP地址
     */
    @ExcelProperty(value = "IP地址")
    private String sourceIp;

    /**
     * 创建日期
     */
    @ExcelProperty(value = "创建日期")
    private Date createTime;

}