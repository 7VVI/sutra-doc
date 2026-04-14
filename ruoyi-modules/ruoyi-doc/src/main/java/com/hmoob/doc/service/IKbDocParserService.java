package com.hmoob.doc.service;

import com.hmoob.doc.domain.dto.DocumentParseResult;

import java.io.InputStream;
import java.util.List;

/**
 * KB文档解析服务接口
 * 使用Spring AI Alibaba TikaDocumentParser解析文档内容
 *
 * @author hmoob
 */
public interface IKbDocParserService {

    /**
     * 从文件路径解析文档内容
     *
     * @param filePath 文件物理路径
     * @return 解析后的文本内容
     */
    String parseContent(String filePath);

    /**
     * 从输入流解析文档内容
     *
     * @param inputStream 输入流
     * @return 解析后的文本内容
     */
    String parseContent(InputStream inputStream);

    /**
     * 解析文档内容并返回元数据
     *
     * @param inputStream 输入流
     * @return 包含内容和元数据的解析结果
     */
    DocumentParseResult parseWithMetadata(InputStream inputStream);

    /**
     * 获取支持的文件类型列表
     *
     * @return 支持的文件扩展名列表
     */
    List<String> getSupportedFileTypes();

    /**
     * 检查文件类型是否支持
     *
     * @param fileType 文件扩展名
     * @return 是否支持该文件类型
     */
    boolean isSupported(String fileType);

}