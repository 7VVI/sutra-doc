package com.hmoob.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * KB文件监控配置属性
 *
 * @author hmoob
 */
@Data
@Component
@ConfigurationProperties(prefix = "kb.file-watch")
public class KbFileWatchProperties {

    /**
     * 是否启用文件监控
     */
    private boolean enabled = true;

    /**
     * 全量快照扫描间隔（秒），默认5分钟
     */
    private int scanInterval = 300;

    /**
     * 事件防抖时间（毫秒），默认500ms
     */
    private int debounceMs = 500;

    /**
     * 默认排除模式（逗号分隔）
     */
    private String defaultExcludePattern = "*.tmp,~$*,*.swp,.DS_Store,Thumbs.db";

    /**
     * 文件Hash并发线程数
     */
    private int hashThreads = 4;

    /**
     * 启动时是否自动开始监控
     */
    private boolean autoStart = true;
}
