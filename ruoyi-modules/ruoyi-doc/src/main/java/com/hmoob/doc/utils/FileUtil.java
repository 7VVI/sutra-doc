package com.hmoob.doc.utils;

import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 文件工具类
 *
 * @author hmoob
 */
public class FileUtil {

    /**
     * 支持的文件类型集合
     */
    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "rtf", "odt", "ods", "odp", "html", "xml",
        "jpg", "jpeg", "png", "gif", "bmp", "zip", "rar"
    );

    /**
     * 需要转换预览的文件类型
     */
    public static final Set<String> CONVERT_FILE_TYPES = Set.of(
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf"
    );

    /**
     * 计算文件的SHA256哈希值
     *
     * @param file 文件
     * @return SHA256哈希值（十六进制字符串）
     * @throws ServiceException 计算失败时抛出异常
     */
    public static String calculateSha256(MultipartFile file) {
        try {
            return cn.hutool.crypto.digest.DigestUtil.sha256Hex(file.getInputStream());
        } catch (Exception e) {
            throw new ServiceException("计算文件哈希值失败");
        }
    }

    /**
     * 检查文件类型是否支持
     *
     * @param fileType 文件类型（扩展名）
     * @return true-支持，false-不支持
     */
    public static boolean isFileTypeSupported(String fileType) {
        if (StringUtils.isBlank(fileType)) {
            return false;
        }
        return SUPPORTED_FILE_TYPES.contains(fileType.toLowerCase());
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 文件扩展名（小写），如果没有扩展名则返回空字符串
     */
    public static String getFileExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 从文件名中提取文件标题（去掉扩展名）
     *
     * @param fileName 文件名
     * @return 文件标题，如果文件名为空则返回空字符串
     */
    public static String extractFileTitle(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, lastDotIndex);
    }

    /**
     * 获取文档标题
     * 如果提供了自定义标题则使用，否则从文件名中提取（去掉扩展名）
     *
     * @param customTitle 自定义标题
     * @param fileName 文件名
     * @return 文档标题
     */
    public static String getDocTitle(String customTitle, String fileName) {
        return StringUtils.isNotBlank(customTitle) ? customTitle : extractFileTitle(fileName);
    }

    /**
     * 检查文件是否需要转换预览
     * PDF文件本身即可预览，无需转换；CONVERT_FILE_TYPES中的文件需要转换为PDF
     *
     * @param fileType 文件类型（扩展名）
     * @return true-需要转换，false-不需要转换
     */
    public static boolean needsConversion(String fileType) {
        if (StringUtils.isBlank(fileType)) {
            return false;
        }
        String lowerFileType = fileType.toLowerCase();
        // PDF文件不需要转换，其他在CONVERT_FILE_TYPES中的文件需要转换
        return !"pdf".equals(lowerFileType) && CONVERT_FILE_TYPES.contains(lowerFileType);
    }

    /**
     * 检查文件是否已经是PDF格式（可直接预览）
     *
     * @param fileType 文件类型（扩展名）
     * @return true-是PDF，false-不是PDF
     */
    public static boolean isPdf(String fileType) {
        return "pdf".equalsIgnoreCase(fileType);
    }
}
