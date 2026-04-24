package com.hmoob.doc.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * KB文档下载次数超限事件
 * 当用户下载次数超过限制时发布此事件，可订阅用于消息通知等
 *
 * @author hmoob
 */
@Data
public class KbDownloadLimitExceedEvent implements Serializable {

    @Serial
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
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 当前下载次数
     */
    private long currentCount;

    /**
     * 下载次数限制
     */
    private long limitCount;

    /**
     * 租户ID
     */
    private String tenantId;

}
