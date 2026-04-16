package com.hmoob.doc.service.impl;

import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.doc.domain.vo.KbDocPreviewVo;
import com.hmoob.doc.domain.vo.KbDocVo;
import com.hmoob.doc.service.IKbDocConvertService;
import com.hmoob.doc.service.IKbDocPreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * KB文档预览服务实现
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KbDocPreviewServiceImpl implements IKbDocPreviewService {

    private final IKbDocConvertService convertService;

    /**
     * 直接支持预览的文件类型（无需转换）
     */
    private static final Set<String> DIRECT_PREVIEW_TYPES = Set.of(
        "pdf", "jpg", "jpeg", "png", "gif", "bmp", "webp",
        "txt", "md", "json", "xml", "html", "csv"
    );

    /**
     * 需要转换的文件类型（Office文档）
     */
    private static final Set<String> CONVERT_PREVIEW_TYPES = Set.of(
        "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "odt", "ods", "odp", "rtf"
    );

    /**
     * 图片文件类型
     */
    private static final Set<String> IMAGE_TYPES = Set.of(
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    /**
     * 文本文件类型
     */
    private static final Set<String> TEXT_TYPES = Set.of(
        "txt", "md", "json", "xml", "html", "csv", "log"
    );

    /**
     * 预览URL前缀（可通过配置修改）
     */
    private static final String PREVIEW_URL_PREFIX = "/kb/doc/preview/";
    private static final String DOWNLOAD_URL_PREFIX = "/kb/doc/download/";

    @Override
    public KbDocPreviewVo getPreviewInfo(KbDocVo docVo) {
        if (docVo == null) {
            return null;
        }

        KbDocPreviewVo previewVo = new KbDocPreviewVo();
        previewVo.setDocId(docVo.getDocId());
        previewVo.setDocName(docVo.getDocName());
        previewVo.setDocTitle(docVo.getDocTitle());
        previewVo.setFileType(docVo.getFileType());
        previewVo.setFileSize(docVo.getFileSize());

        // 设置URL
        previewVo.setPreviewUrl(getPreviewUrl(docVo.getDocId()));
        previewVo.setOriginalUrl(getDownloadUrl(docVo.getDocId()));

        // 获取文件类型
        String fileType = docVo.getFileType();
        if (StringUtils.isBlank(fileType)) {
            previewVo.setPreviewType("unsupported");
            previewVo.setCanPreview(false);
            previewVo.setPreviewMessage("未知文件类型，不支持预览");
            return previewVo;
        }

        fileType = fileType.toLowerCase();

        // 判断预览类型
        String previewType = getPreviewType(fileType);
        previewVo.setPreviewType(previewType);

        // 设置预览状态
        previewVo.setCanPreview(isPreviewSupported(fileType));
        previewVo.setNeedConvert(CONVERT_PREVIEW_TYPES.contains(fileType));

        // 设置提示消息
        if (!previewVo.getCanPreview()) {
            previewVo.setPreviewMessage("该文件类型不支持在线预览，请下载后查看");
        } else if (previewVo.getNeedConvert() && !convertService.isServiceAvailable()) {
            previewVo.setPreviewMessage("Office文档预览需要转换服务，当前服务未启用");
            previewVo.setCanPreview(false);
        }

        return previewVo;
    }

    @Override
    public String getPreviewUrl(Long docId) {
        if (docId == null) {
            return null;
        }
        return PREVIEW_URL_PREFIX + docId;
    }

    @Override
    public String getDownloadUrl(Long docId) {
        if (docId == null) {
            return null;
        }
        return DOWNLOAD_URL_PREFIX + docId;
    }

    @Override
    public boolean isPreviewSupported(String fileType) {
        if (StringUtils.isBlank(fileType)) {
            return false;
        }
        String type = fileType.toLowerCase();
        return DIRECT_PREVIEW_TYPES.contains(type) || CONVERT_PREVIEW_TYPES.contains(type);
    }

    @Override
    public String getPreviewType(String fileType) {
        if (StringUtils.isBlank(fileType)) {
            return "unsupported";
        }

        String type = fileType.toLowerCase();

        if ("pdf".equals(type)) {
            return "pdf";
        }

        if (IMAGE_TYPES.contains(type)) {
            return "image";
        }

        if (TEXT_TYPES.contains(type)) {
            return "text";
        }

        if (CONVERT_PREVIEW_TYPES.contains(type)) {
            return "office";
        }

        return "unsupported";
    }

}