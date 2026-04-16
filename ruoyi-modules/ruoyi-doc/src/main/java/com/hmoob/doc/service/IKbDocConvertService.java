package com.hmoob.doc.service;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * KB文档转换服务接口
 * 提供文档格式转换功能，主要用于将文档转换为PDF格式以支持预览
 *
 * @author hmoob
 */
public interface IKbDocConvertService {

    /**
     * 将文档转换为PDF格式
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标PDF文件路径
     * @return 转换是否成功
     */
    boolean convertToPdf(String sourcePath, String targetPath);

    /**
     * 将文档转换为PDF格式
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标PDF文件路径
     * @return 转换是否成功
     */
    boolean convertToPdf(Path sourcePath, Path targetPath);

    /**
     * 从输入流转换文档为PDF
     *
     * @param inputStream  源文件输入流
     * @param fileType     源文件类型（扩展名）
     * @param outputStream PDF输出流
     * @return 转换是否成功
     */
    boolean convertToPdf(InputStream inputStream, String fileType, java.io.OutputStream outputStream);

    /**
     * 获取支持的转换格式列表
     * 支持转换为PDF的源文件格式
     *
     * @return 支持的文件扩展名列表
     */
    java.util.List<String> getSupportedFormats();

    /**
     * 检查文件格式是否支持转换
     *
     * @param fileType 文件扩展名
     * @return 是否支持该格式转换
     */
    boolean isSupported(String fileType);

    /**
     * 获取转换服务状态
     * 检查LibreOffice服务是否可用
     *
     * @return 服务是否可用
     */
    boolean isServiceAvailable();

}