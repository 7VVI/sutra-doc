package com.hmoob.doc.service.impl;

import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.hmoob.doc.domain.dto.DocumentParseResult;
import com.hmoob.doc.service.IKbDocParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * KB文档解析服务实现
 * 使用Spring AI Alibaba TikaDocumentParser解析文档内容
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KbDocParserServiceImpl implements IKbDocParserService {

    private final TikaDocumentParser tikaDocumentParser;

    /**
     * 支持的文件类型集合
     */
    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "rtf", "odt", "ods", "odp", "html", "xml"
    );

    /**
     * 从文件路径解析文档内容
     *
     * @param filePath 文件物理路径
     * @return 解析后的文本内容
     */
    @Override
    public String parseContent(String filePath) {
        try (InputStream inputStream = Files.newInputStream(Path.of(filePath))) {
            return parseContent(inputStream);
        } catch (Exception e) {
            log.error("解析文档失败: {}", filePath, e);
            return "";
        }
    }

    /**
     * 从输入流解析文档内容
     *
     * @param inputStream 输入流
     * @return 解析后的文本内容
     */
    @Override
    public String parseContent(InputStream inputStream) {
        try {
            List<Document> documents = tikaDocumentParser.parse(inputStream);
            if (documents == null || documents.isEmpty()) {
                return "";
            }
            // 合并所有文档内容
            return documents.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isEmpty())
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("从输入流解析文档失败", e);
            return "";
        }
    }

    /**
     * 解析文档内容并返回元数据
     *
     * @param inputStream 输入流
     * @return 包含内容和元数据的解析结果
     */
    @Override
    public DocumentParseResult parseWithMetadata(InputStream inputStream) {
        DocumentParseResult result = new DocumentParseResult();
        try {
            List<Document> documents = tikaDocumentParser.parse(inputStream);
            if (documents == null || documents.isEmpty()) {
                return result;
            }
            // 合并所有文档内容
            String content = documents.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isEmpty())
                .collect(Collectors.joining("\n"));
            result.setContent(content);
            result.setContentLength(content != null ? (long) content.length() : 0L);

            // 合并所有文档的元数据
            Map<String, String> mergedMetadata = new HashMap<>();
            for (Document document : documents) {
                Map<String, Object> metadata = document.getMetadata();
                if (metadata != null) {
                    // 转换Object类型为String类型
                    metadata.forEach((key, value) -> {
                        if (value != null) {
                            mergedMetadata.put(key, String.valueOf(value));
                        }
                    });
                }
            }
            result.setMetadata(mergedMetadata);

            // 提取常用元数据字段
            result.setTitle(mergedMetadata.getOrDefault("title", ""));
            result.setAuthor(mergedMetadata.getOrDefault("author", ""));
            result.setMimeType(mergedMetadata.getOrDefault("Content-Type", ""));
        } catch (Exception e) {
            log.error("解析文档内容并返回元数据失败", e);
        }
        return result;
    }

    /**
     * 获取支持的文件类型列表
     *
     * @return 支持的文件扩展名列表
     */
    @Override
    public List<String> getSupportedFileTypes() {
        return new ArrayList<>(SUPPORTED_TYPES);
    }

    /**
     * 检查文件类型是否支持
     *
     * @param fileType 文件扩展名
     * @return 是否支持该文件类型
     */
    @Override
    public boolean isSupported(String fileType) {
        if (fileType == null) {
            return false;
        }
        return SUPPORTED_TYPES.contains(fileType.toLowerCase());
    }

}