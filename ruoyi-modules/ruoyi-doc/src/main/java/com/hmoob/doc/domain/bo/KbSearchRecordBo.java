package com.hmoob.doc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.hmoob.common.mybatis.core.domain.BaseEntity;
import com.hmoob.doc.domain.KbSearchRecord;

import java.io.Serial;

/**
 * KB搜索记录业务对象 kb_search_record
 *
 * @author hmoob
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KbSearchRecord.class, reverseConvertGenerate = false)
public class KbSearchRecordBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索记录ID
     */
    private Long searchId;

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 500, message = "搜索关键词长度不能超过{max}个字符")
    private String keywords;

    /**
     * 最大匹配关键词
     */
    private String keywordsMax;

    /**
     * 搜索结果标题
     */
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
    private String sourceIp;

    /**
     * 搜索结果数量
     */
    private Integer resultCount;

    /**
     * 搜索耗时,单位:毫秒
     */
    private Integer searchDurationMs;

    /**
     * 搜索类型: 1-关键词搜索, 2-高级搜索, 3-全文检索
     */
    @NotNull(message = "搜索类型不能为空")
    private Integer searchType;

    /**
     * 用户ID
     */
    private Long userId;

}