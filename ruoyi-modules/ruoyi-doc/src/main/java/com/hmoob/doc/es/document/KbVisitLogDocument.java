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
 * KB访问日志ES文档对象
 * 映射索引: kb_visit_log
 */
@Data
@Document(indexName = "kb_visit_log")
@Setting(settingPath = "elasticsearch/kb_visit_log_template.json")
public class KbVisitLogDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问ID
     */
    @Id
    private Long visitId;

    /**
     * 文档ID
     */
    @Field(type = FieldType.Long)
    private Long docId;

    /**
     * 文件夹ID
     */
    @Field(type = FieldType.Long)
    private Long folderId;

    /**
     * 访问类型
     */
    @Field(type = FieldType.Integer)
    private Integer visitType;

    /**
     * 关键词
     */
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String keywords;

    /**
     * 关键词(最大分词)
     */
    @Field(type = FieldType.Keyword)
    private String keywordsMax;

    /**
     * 来源IP
     */
    @Field(type = FieldType.Ip)
    private String sourceIp;

    /**
     * 用户ID
     */
    @Field(type = FieldType.Keyword)
    private String userId;

    /**
     * 设备类型
     */
    @Field(type = FieldType.Keyword)
    private String deviceType;

    /**
     * 浏览器
     */
    @Field(type = FieldType.Keyword)
    private String browser;

    /**
     * 访问时长(秒)
     */
    @Field(type = FieldType.Integer)
    private Integer durationSeconds;

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