package com.hmoob.doc.service;

import com.hmoob.doc.domain.vo.KbDocPreviewVo;
import com.hmoob.doc.domain.vo.KbDocVo;

/**
 * KB文档预览服务接口
 *
 * @author hmoob
 */
public interface IKbDocPreviewService {

    /**
     * 获取文档预览信息
     *
     * @param docVo 文档信息
     * @return 预览信息对象
     */
    KbDocPreviewVo getPreviewInfo(KbDocVo docVo);

    /**
     * 获取文档预览URL
     *
     * @param docId 文档ID
     * @return 预览URL
     */
    String getPreviewUrl(Long docId);

    /**
     * 获取文档下载URL
     *
     * @param docId 文档ID
     * @return 下载URL
     */
    String getDownloadUrl(Long docId);

    /**
     * 检查文件是否支持预览
     *
     * @param fileType 文件类型（扩展名）
     * @return 是否支持预览
     */
    boolean isPreviewSupported(String fileType);

    /**
     * 获取预览类型
     *
     * @param fileType 文件类型
     * @return 预览类型：pdf/image/text/office/unsupported
     */
    String getPreviewType(String fileType);

}