package com.hmoob.doc.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 文档预览Vo对象
 *
 * @author hmoob
 */
@Data
public class KbDocPreviewVo implements Serializable {

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
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 预览URL
     */
    private String previewUrl;

    /**
     * 原始文件URL（用于下载）
     */
    private String originalUrl;

    /**
     * 预览类型
     * pdf - PDF预览
     * image - 图片预览
     * text - 文本预览
     * office - Office文档预览（需转换）
     * unsupported - 不支持预览
     */
    private String previewType;

    /**
     * 是否支持预览
     */
    private Boolean canPreview;

    /**
     * 是否需要转换
     */
    private Boolean needConvert;

    /**
     * 预览消息（如不支持预览时提示）
     */
    private String previewMessage;

    /**
     * 文档内容（文本类型时直接返回内容）
     */
    private String content;

}