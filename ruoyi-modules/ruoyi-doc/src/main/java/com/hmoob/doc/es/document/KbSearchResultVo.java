package com.hmoob.doc.es.document;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB搜索结果视图对象
 * 参考旧系统 KmSearchResultVO 设计，包含ES搜索结果和DB补充信息
 *
 * @author hmoob
 */
@Data
public class KbSearchResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    private Long docId;

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 预览文件ID(PDF转换后)
     */
    private Long previewFileId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 文档标题(可能包含高亮标签)
     */
    private String docTitle;

    /**
     * 高亮标题(ES高亮后的标题)
     */
    private String highlightTitle;

    /**
     * 高亮内容片段(ES高亮后的内容摘要)
     */
    private String contentSnippet;

    /**
     * 原始内容(未高亮)
     */
    private String content;

    /**
     * 关键词
     */
    private String keywords;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件夹ID
     */
    private Long folderId;

    /**
     * 文件夹路径
     */
    private String folderPath;

    /**
     * 分类
     */
    private String category;

    /**
     * 主题编码列表
     */
    private String[] topicCodes;

    /**
     * 业务类型列表
     */
    private String[] businessTypes;

    /**
     * 组织编码
     */
    private String orgCode;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 是否可下载
     */
    private Integer downloadFlag;

    /**
     * 是否公开备注
     */
    private Integer publicRemark;

    /**
     * 备注
     */
    private String remark;

    /**
     * 浏览次数
     */
    private Long viewCount;

    /**
     * 下载次数
     */
    private Long downloadCount;

    /**
     * ES相关性评分
     */
    private Float score;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 发布状态
     */
    private Integer releaseFlag;

    /**
     * 是否已收藏(当前用户)
     */
    private Integer favourite;
}