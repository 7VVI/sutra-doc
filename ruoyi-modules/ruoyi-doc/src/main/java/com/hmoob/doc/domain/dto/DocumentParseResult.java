package com.hmoob.doc.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 文档解析结果DTO
 * 用于存储文档解析后的内容和元数据
 *
 * @author hmoob
 */
@Data
public class DocumentParseResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 解析后的文本内容
     */
    private String content;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档作者
     */
    private String author;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 内容长度
     */
    private Long contentLength;

    /**
     * 其他元数据信息
     */
    private Map<String, String> metadata;

}