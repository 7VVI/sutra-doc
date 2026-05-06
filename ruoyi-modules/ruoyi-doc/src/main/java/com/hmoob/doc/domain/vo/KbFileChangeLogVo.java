package com.hmoob.doc.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbFileChangeLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文件变化记录视图对象
 *
 * @author hmoob
 */
@Data
@AutoMapper(target = KbFileChangeLog.class)
public class KbFileChangeLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long logId;
    private Long configId;
    private String filePath;
    private String fileName;
    private Integer isDirectory;
    private String changeType;
    private Long fileSize;
    private String contentHash;
    private String oldHash;
    private Long docId;
    private Long folderId;
    private Integer processStatus;
    private String processMsg;
    private Date processTime;
    private String tenantId;
    private Date createTime;
}
