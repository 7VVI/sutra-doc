package com.hmoob.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.hmoob.doc.enums.StorageTypeEnum;

/**
 * KB文件上传配置
 *
 * @author hmoob
 */
@Data
@Component
@ConfigurationProperties(prefix = "kb.upload")
public class KbUploadProperties {

    /**
     * 存储模式: oss-对象存储, local-本地存储
     * @see StorageTypeEnum
     */
    private String mode = StorageTypeEnum.OSS.getCode();

    /**
     * 本地存储根路径
     */
    private String localPath = "./upload/kb";

    /**
     * 本地文件访问URL前缀(用于拼接下载/预览URL)
     */
    private String localUrlPrefix = "/kb/file/content";

    /**
     * 最大文件大小(MB)
     */
    private Integer maxFileSize = 100;

    /**
     * 是否允许OSS不可用时自动降级到本地存储
     */
    private Boolean fallbackToLocal = true;

    /**
     * 判断是否使用本地存储
     */
    public boolean useLocalStorage() {
        return StorageTypeEnum.LOCAL.getCode().equalsIgnoreCase(mode);
    }

    /**
     * 判断是否允许降级到本地存储
     */
    public boolean allowFallback() {
        return fallbackToLocal != null && fallbackToLocal;
    }
}