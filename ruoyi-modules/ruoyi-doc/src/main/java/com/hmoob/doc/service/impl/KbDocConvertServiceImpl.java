package com.hmoob.doc.service.impl;

import com.hmoob.doc.service.IKbDocConvertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * KB文档转换服务实现
 *
 * 注意：PDF转换功能依赖外部LibreOffice服务。
 * 当前实现为基础版本，仅支持PDF文件直接处理。
 * 如需完整的文档转换功能，需配置JODConverter并安装LibreOffice服务。
 *
 * JODConverter配置步骤：
 * 1. 安装LibreOffice服务（推荐版本7.x）
 * 2. 在pom.xml添加JODConverter依赖
 * 3. 配置LibreOffice服务端口和连接参数
 *
 * @author hmoob
 */
@Slf4j
@Service
public class KbDocConvertServiceImpl implements IKbDocConvertService {

    /**
     * 支持转换为PDF的文件格式集合
     * 注：实际转换能力取决于LibreOffice服务配置
     */
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "odt", "ods", "odp", "rtf", "txt", "html"
    );

    /**
     * PDF文件格式集合 - 无需转换
     */
    private static final Set<String> PDF_FORMATS = Set.of("pdf");

    /**
     * 服务可用状态
     * 默认为false，当检测到LibreOffice服务可用时设置为true
     */
    private volatile boolean serviceAvailable = false;

    /**
     * 将文档转换为PDF格式
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标PDF文件路径
     * @return 转换是否成功
     */
    @Override
    public boolean convertToPdf(String sourcePath, String targetPath) {
        return convertToPdf(Path.of(sourcePath), Path.of(targetPath));
    }

    /**
     * 将文档转换为PDF格式
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标PDF文件路径
     * @return 转换是否成功
     */
    @Override
    public boolean convertToPdf(Path sourcePath, Path targetPath) {
        try {
            // 检查源文件是否存在
            if (!Files.exists(sourcePath)) {
                log.error("源文件不存在: {}", sourcePath);
                return false;
            }

            // 获取文件类型
            String fileName = sourcePath.getFileName().toString();
            String fileType = getFileExtension(fileName);

            // PDF文件无需转换，直接复制
            if (PDF_FORMATS.contains(fileType.toLowerCase())) {
                Files.copy(sourcePath, targetPath);
                log.info("PDF文件直接复制: {} -> {}", sourcePath, targetPath);
                return true;
            }

            // 其他格式需要LibreOffice转换
            if (!isServiceAvailable()) {
                log.warn("LibreOffice服务不可用，无法转换文件: {}", fileName);
                return false;
            }

            // 使用JODConverter进行转换（需要配置后启用）
            // 以下为预留实现，配置JODConverter后替换
            log.warn("文档转换功能未启用，需配置LibreOffice服务");
            return false;

        } catch (Exception e) {
            log.error("文档转换失败: {} -> {}", sourcePath, targetPath, e);
            return false;
        }
    }

    /**
     * 从输入流转换文档为PDF
     *
     * @param inputStream  源文件输入流
     * @param fileType     源文件类型（扩展名）
     * @param outputStream PDF输出流
     * @return 转换是否成功
     */
    @Override
    public boolean convertToPdf(InputStream inputStream, String fileType, java.io.OutputStream outputStream) {
        try {
            // PDF文件无需转换
            if (PDF_FORMATS.contains(fileType.toLowerCase())) {
                // 直接复制流数据
                inputStream.transferTo(outputStream);
                return true;
            }

            // 其他格式需要转换
            if (!isServiceAvailable()) {
                log.warn("LibreOffice服务不可用，无法转换文件类型: {}", fileType);
                return false;
            }

            // 使用JODConverter进行转换（需要配置后启用）
            log.warn("文档转换功能未启用，需配置LibreOffice服务");
            return false;

        } catch (Exception e) {
            log.error("从输入流转换文档失败: {}", fileType, e);
            return false;
        }
    }

    /**
     * 获取支持的转换格式列表
     *
     * @return 支持的文件扩展名列表
     */
    @Override
    public List<String> getSupportedFormats() {
        return new ArrayList<>(SUPPORTED_FORMATS);
    }

    /**
     * 检查文件格式是否支持转换
     *
     * @param fileType 文件扩展名
     * @return 是否支持该格式转换
     */
    @Override
    public boolean isSupported(String fileType) {
        if (fileType == null) {
            return false;
        }
        return SUPPORTED_FORMATS.contains(fileType.toLowerCase());
    }

    /**
     * 获取转换服务状态
     *
     * @return 服务是否可用
     */
    @Override
    public boolean isServiceAvailable() {
        // 检查LibreOffice服务可用性
        // 实际环境中应通过JODConverter检查服务连接状态
        return serviceAvailable;
    }

    /**
     * 设置服务可用状态（供外部配置调用）
     *
     * @param available 服务是否可用
     */
    public void setServiceAvailable(boolean available) {
        this.serviceAvailable = available;
        log.info("PDF转换服务状态更新: {}", available ? "可用" : "不可用");
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名（不含点号）
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

}