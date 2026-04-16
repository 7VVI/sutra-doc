package com.hmoob.doc.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 文档统计Vo对象
 *
 * @author hmoob
 */
@Data
public class DocStatVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目录ID
     */
    private Long folderId;

    /**
     * 文档数量
     */
    private Long docCount;

    /**
     * 总大小(字节)
     */
    private Long totalSize;

    /**
     * 总下载次数
     */
    private Long totalDownloads;

    /**
     * 总浏览次数
     */
    private Long totalViews;

}