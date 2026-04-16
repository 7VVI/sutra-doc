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
 * KB文档ES文档对象
 * 映射索引: kb_doc
 */
@Data
@Document(indexName = "kb_doc")
@Setting(settingPath = "elasticsearch/kb_doc_index_template.json")
public class KbDocDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @Id
    private Long docId;

    /**
     * 文档名称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String docName;

    /**
     * 文档标题
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String docTitle;

    /**
     * 文档内容
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 关键词
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String keywords;

    /**
     * 文件夹ID
     */
    @Field(type = FieldType.Long)
    private Long folderId;

    /**
     * 文件夹路径
     */
    @Field(type = FieldType.Keyword)
    private String folderPath;

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
     * 文件类型
     */
    @Field(type = FieldType.Keyword)
    private String fileType;

    /**
     * 分类
     */
    @Field(type = FieldType.Keyword)
    private String category;

    /**
     * 组织编码
     */
    @Field(type = FieldType.Keyword)
    private String orgCode;

    /**
     * 租户ID
     */
    @Field(type = FieldType.Keyword)
    private String tenantId;

    /**
     * 状态
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 发布标识
     */
    @Field(type = FieldType.Integer)
    private Integer releaseFlag;

    /**
     * 浏览次数
     */
    @Field(type = FieldType.Long)
    private Long viewCount;

    /**
     * 下载次数
     */
    @Field(type = FieldType.Long)
    private Long downloadCount;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 创建者
     */
    @Field(type = FieldType.Keyword)
    private String createBy;
}