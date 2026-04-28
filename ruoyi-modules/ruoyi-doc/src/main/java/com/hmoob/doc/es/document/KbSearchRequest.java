package com.hmoob.doc.es.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * KB搜索请求对象
 * 参考旧系统 KmDocEsParamVO 设计
 *
 * @author hmoob
 */
@Data
public class KbSearchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 高级检索标识: false-普通检索, true-高级检索
     * 普通检索: 标题、关键字、全文合并检索(should)
     * 高级检索: 各条件独立且必须满足(must)
     */
    private Boolean advSearchFlag;

    /**
     * 是否精确检索标识: false-模糊匹配, true-短语精确匹配
     */
    private Boolean phraseMatchSearchFlag;

    /**
     * 标题(单独搜索)
     */
    private String title;

    /**
     * 搜索关键词(通用关键词搜索)
     */
    private String keywords;

    /**
     * 内容(全文检索)
     */
    private String content;

    /**
     * 分类列表(筛选条件)
     */
    private List<String> category;

    /**
     * 业务类型列表(筛选条件)
     */
    private String[] businessTypes;

    /**
     * 主题编码列表(筛选条件，支持前缀模糊匹配)
     */
    private String[] topicCodes;

    /**
     * 文件夹ID列表(筛选条件)
     */
    private Long[] folderIds;

    /**
     * 部门ID列表(筛选条件，支持多部门过滤)
     */
    private Long[] deptIds;

    /**
     * 文件类型(筛选条件)
     */
    private String fileType;

    /**
     * 结果中检索的过滤条件(二次筛选)
     */
    private List<KbSearchFilterParam> filterParams;

    /**
     * 创建时间起始
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date createTimeStart;

    /**
     * 创建时间截止
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date createTimeEnd;

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
     * 排序字段(createTime, viewCount, downloadCount, score)
     */
    private String column;

    /**
     * 排序方式(asc, desc)
     */
    private String order;

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
        // 最大每页100条
        pageSize = Math.min(pageSize, 100);
        return (pageNum - 1) * pageSize;
    }

    /**
     * 获取排序字段（兼容旧系统字段名映射）
     */
    public String getSortField() {
        if (column == null) {
            return null;
        }
        // 字典文本字段去掉后缀
        if (column.endsWith("_dictText")) {
            return column.substring(0, column.lastIndexOf("_dictText"));
        }
        return column;
    }

    /**
     * 获取排序方向
     */
    public String getSortOrder() {
        return order;
    }
}