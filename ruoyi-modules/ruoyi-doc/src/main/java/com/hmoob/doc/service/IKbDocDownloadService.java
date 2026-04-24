package com.hmoob.doc.service;

/**
 * KB文档下载限制 服务层
 * 基于Redis统计用户每日下载次数，超限时发布事件
 *
 * @author hmoob
 */
public interface IKbDocDownloadService {

    /**
     * 检查用户是否可以下载指定文档
     * 未超限返回true，超限返回false
     *
     * @param userId 用户ID
     * @param docId  文档ID
     * @return 是否允许下载
     */
    boolean checkDownloadAllowed(Long userId, Long docId);

    /**
     * 记录一次下载并递增计数
     * 如果超限，发布 KbDownloadLimitExceedEvent 事件
     *
     * @param userId 用户ID
     * @param docId  文档ID
     * @return 递增后的下载次数
     */
    long recordDownload(Long userId, Long docId);

    /**
     * 获取用户当日对指定文档的下载次数
     *
     * @param userId 用户ID
     * @param docId  文档ID
     * @return 下载次数
     */
    long getDownloadCount(Long userId, Long docId);

    /**
     * 获取用户当日全局下载次数
     *
     * @param userId 用户ID
     * @return 下载次数
     */
    long getGlobalDownloadCount(Long userId);

}
