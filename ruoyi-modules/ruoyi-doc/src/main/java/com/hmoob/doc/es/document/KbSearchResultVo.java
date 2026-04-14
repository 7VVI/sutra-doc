package com.hmoob.doc.es.document;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB搜索结果视图对象
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
     * 文档名称
     */
    private String docName;

    /**
     * 文档标题
     */
    private String docTitle;

    /**
     * 高亮内容片段
     */
    private String contentSnippet;

    /**
     * 关键词
     */
    private String keywords;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件夹ID
     */
    private Long folderId;

    /**
     * 文件夹路径
     */
    private String folderPath;

    /**
     * 主题编码列表
     */
    private String[] topicCodes;

    /**
     * 业务类型列表
     */
    private String[] businessTypes;

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
}