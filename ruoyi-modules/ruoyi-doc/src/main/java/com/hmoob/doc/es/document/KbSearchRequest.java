package com.hmoob.doc.es.document;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * KB搜索请求对象
 *
 * @author hmoob
 */
@Data
public class KbSearchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键词
     */
    private String keywords;

    /**
     * 文件夹ID列表(筛选条件)
     */
    private Long[] folderIds;

    /**
     * 主题编码列表(筛选条件)
     */
    private String[] topicCodes;

    /**
     * 业务类型列表(筛选条件)
     */
    private String[] businessTypes;

    /**
     * 文件类型(筛选条件)
     */
    private String fileType;

    /**
     * 租户ID(租户隔离)
     */
    private String tenantId;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 排序字段(createTime, viewCount, score)
     */
    private String sortField;

    /**
     * 排序方式(asc, desc)
     */
    private String sortOrder;

    /**
     * 是否只搜索已发布文档
     */
    private Boolean publishedOnly = true;

    /**
     * 获取偏移量
     */
    public int getOffset() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        return (pageNum - 1) * pageSize;
    }
}