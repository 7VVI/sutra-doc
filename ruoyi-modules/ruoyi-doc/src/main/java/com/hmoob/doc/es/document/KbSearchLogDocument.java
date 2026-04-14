package com.hmoob.doc.es.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB搜索日志ES文档对象
 * 映射索引: kb_search_log
 */
@Data
@Document(indexName = "kb_search_log")
@Setting(settingPath = "elasticsearch/kb_search_log_template.json")
public class KbSearchLogDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索ID
     */
    @Id
    private Long searchId;

    /**
     * 搜索关键词
     */
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String keywords;

    /**
     * 搜索关键词(最大分词)
     */
    @Field(type = FieldType.Keyword)
    private String keywordsMax;

    /**
     * 搜索标题
     */
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String searchTitle;

    /**
     * 搜索内容
     */
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String searchContent;

    /**
     * 主题编码列表
     */
    @Field(type = FieldType.Keyword)
    private String[] topicCodes;

    /**
     * 业务类型列表
     */
    @Field(type = FieldType.Keyword)
    private String[] businessTypes;

    /**
     * 文件夹ID列表
     */
    @Field(type = FieldType.Keyword)
    private String[] folderIds;

    /**
     * 来源IP
     */
    @Field(type = FieldType.Ip)
    private String sourceIp;

    /**
     * 结果数量
     */
    @Field(type = FieldType.Integer)
    private Integer resultCount;

    /**
     * 搜索耗时(毫秒)
     */
    @Field(type = FieldType.Integer)
    private Integer searchDurationMs;

    /**
     * 搜索类型
     */
    @Field(type = FieldType.Integer)
    private Integer searchType;

    /**
     * 用户ID
     */
    @Field(type = FieldType.Keyword)
    private String userId;

    /**
     * 租户ID
     */
    @Field(type = FieldType.Keyword)
    private String tenantId;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}