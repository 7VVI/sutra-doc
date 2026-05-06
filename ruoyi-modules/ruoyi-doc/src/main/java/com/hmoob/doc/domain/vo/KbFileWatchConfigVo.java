package com.hmoob.doc.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbFileWatchConfig;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文件监控配置视图对象
 *
 * @author hmoob
 */
@Data
@AutoMapper(target = KbFileWatchConfig.class)
public class KbFileWatchConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long configId;
    private String watchPath;
    private String watchName;
    private Long folderId;
    private Integer recursive;
    private String includePattern;
    private String excludePattern;
    private Integer autoIndex;
    private Integer autoAddDoc;
    private Integer scanInterval;
    private Integer debounceMs;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
