package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbSearchRecord;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB搜索记录视图对象 kb_search_record
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbSearchRecord.class)
public class KbSearchRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索记录ID
     */
    @ExcelProperty(value = "搜索记录ID")
    private Long searchId;

    /**
     * 搜索关键词
     */
    @ExcelProperty(value = "搜索关键词")
    private String keywords;

    /**
     * 最大匹配关键词
     */
    private String keywordsMax;

    /**
     * 搜索结果标题
     */
    @ExcelProperty(value = "搜索结果标题")
    private String searchTitle;

    /**
     * 搜索内容片段
     */
    private String searchContent;

    /**
     * 主题编码(逗号分隔)
     */
    private String topicCodes;

    /**
     * 业务类型(逗号分隔)
     */
    private String businessTypes;

    /**
     * 目录ID列表(逗号分隔)
     */
    private String folderIds;

    /**
     * 来源IP地址
     */
    @ExcelProperty(value = "来源IP")
    private String sourceIp;

    /**
     * 搜索结果数量
     */
    @ExcelProperty(value = "搜索结果数量")
    private Integer resultCount;

    /**
     * 搜索耗时,单位:毫秒
     */
    @ExcelProperty(value = "搜索耗时(ms)")
    private Integer searchDurationMs;

    /**
     * 搜索类型: 1-关键词搜索, 2-高级搜索, 3-全文检索
     */
    @ExcelProperty(value = "搜索类型")
    private Integer searchType;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "搜索时间")
    private Date createTime;

}