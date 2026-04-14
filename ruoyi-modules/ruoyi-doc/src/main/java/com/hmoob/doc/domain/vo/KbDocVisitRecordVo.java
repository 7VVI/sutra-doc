package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbDocVisitRecord;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档访问记录视图对象 kb_doc_visit_record
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbDocVisitRecord.class)
public class KbDocVisitRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问记录ID
     */
    @ExcelProperty(value = "访问记录ID")
    private Long visitId;

    /**
     * 文档ID(搜索记录可为空)
     */
    @ExcelProperty(value = "文档ID")
    private Long docId;

    /**
     * 目录ID(浏览目录时记录)
     */
    @ExcelProperty(value = "目录ID")
    private Long folderId;

    /**
     * 访问类型: 1-上传, 2-预览, 3-下载, 4-删除, 5-编辑, 6-分享
     */
    @ExcelProperty(value = "访问类型")
    private Integer visitType;

    /**
     * 搜索关键词(搜索时记录)
     */
    @ExcelProperty(value = "搜索关键词")
    private String keywords;

    /**
     * 最大匹配关键词
     */
    private String keywordsMax;

    /**
     * 来源IP地址
     */
    @ExcelProperty(value = "来源IP")
    private String sourceIp;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 设备类型: pc, mobile, tablet
     */
    @ExcelProperty(value = "设备类型")
    private String deviceType;

    /**
     * 浏览器类型
     */
    @ExcelProperty(value = "浏览器类型")
    private String browser;

    /**
     * 来源页面
     */
    private String referer;

    /**
     * 停留时长,单位:秒
     */
    @ExcelProperty(value = "停留时长(秒)")
    private Integer durationSeconds;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "访问时间")
    private Date createTime;

}