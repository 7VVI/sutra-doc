package com.hmoob.doc.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.oss.core.OssClient;
import com.hmoob.common.oss.factory.OssFactory;
import com.hmoob.doc.domain.KbFile;
import com.hmoob.doc.domain.bo.KbFileBo;
import com.hmoob.doc.domain.vo.KbFileVo;
import com.hmoob.doc.mapper.KbFileMapper;
import com.hmoob.doc.mapper.KbDocMapper;
import com.hmoob.doc.enums.StorageTypeEnum;
import com.hmoob.doc.service.IKbFileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * KB文件管理 服务实现
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KbFileServiceImpl implements IKbFileService {

    private final KbFileMapper baseMapper;
    private final KbDocMapper docMapper;

    /**
     * 根据文件ID查询信息
     *
     * @param fileId 文件ID
     * @return 文件信息
     */
    @Override
    public KbFileVo selectFileById(Long fileId) {
        return baseMapper.selectVoById(fileId);
    }

    /**
     * 根据SHA256哈希值查询文件
     *
     * @param sha256 SHA256哈希值
     * @return 文件信息
     */
    @Override
    public KbFileVo selectBySha256(String sha256) {
        if (StringUtils.isBlank(sha256)) {
            return null;
        }
        return baseMapper.selectBySha256(sha256);
    }

    /**
     * 计算文件的SHA256哈希值
     *
     * @param inputStream 输入流
     * @return SHA256哈希值
     */
    @Override
    public String calculateSha256(InputStream inputStream) {
        try {
            return DigestUtil.sha256Hex(inputStream);
        } finally {
            IoUtil.close(inputStream);
        }
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件信息
     */
    @Override
    public KbFileVo uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        try {
            // 计算SHA256
            String sha256 = calculateSha256(file.getInputStream());

            // 检查是否存在相同文件（去重）
            KbFileVo existingFile = selectBySha256(sha256);
            if (ObjectUtil.isNotNull(existingFile)) {
                // 文件已存在，返回现有文件信息（去重）
                return existingFile;
            }

            // 获取文件信息
            String originalName = file.getOriginalFilename();
            String fileType = getFileExtension(originalName);
            String mimeType = file.getContentType();
            Long fileSize = file.getSize();

            // 构建文件实体（物理存储路径需要在实际存储后设置）
            KbFile kbFile = new KbFile();
            kbFile.setSha256(sha256);
            kbFile.setOriginalName(originalName);
            kbFile.setFileType(fileType);
            kbFile.setMimeType(mimeType);
            kbFile.setFileSize(fileSize);
            kbFile.setStatus(1);

            // 保存文件信息（物理存储由外部处理）
            baseMapper.insert(kbFile);

            return baseMapper.selectVoById(kbFile.getFileId());
        } catch (IOException e) {
            throw new ServiceException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名
     */
    private String getFileExtension(String fileName) {
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
     * 新增保存文件信息
     *
     * @param bo 文件信息
     * @return 结果
     */
    @Override
    public int insertFile(KbFileBo bo) {
        KbFile file = MapstructUtils.convert(bo, KbFile.class);
        // 设置初始值
        if (file.getStatus() == null) {
            file.setStatus(1);
        }
        return baseMapper.insert(file);
    }

    /**
     * 删除文件管理信息
     *
     * @param fileId 文件ID
     * @return 结果
     */
    @Override
    public int deleteFileById(Long fileId) {
        // 校验文件是否被文档引用
        boolean existDoc = docMapper.exists(new LambdaQueryWrapper<com.hmoob.doc.domain.KbDoc>()
            .eq(com.hmoob.doc.domain.KbDoc::getFileId, fileId)
            .or()
            .eq(com.hmoob.doc.domain.KbDoc::getPreviewFileId, fileId)
            .or()
            .eq(com.hmoob.doc.domain.KbDoc::getOriginalPreviewFileId, fileId));
        if (existDoc) {
            throw new ServiceException("文件被文档引用，不允许删除");
        }
        return baseMapper.deleteById(fileId);
    }

    /**
     * 根据文件ID获取文件实体
     *
     * @param fileId 文件ID
     * @return 文件实体
     */
    @Override
    public KbFile getFileEntityById(Long fileId) {
        return baseMapper.selectById(fileId);
    }

    /**
     * 输出文件内容到响应流
     *
     * @param fileId        文件ID
     * @param response      HTTP响应
     * @param forceDownload 是否强制下载
     */
    @Override
    public boolean serveFile(Long fileId, HttpServletResponse response, boolean forceDownload) {
        KbFile file = baseMapper.selectById(fileId);
        if (file == null) {
            writeError(response, 404, "文件不存在");
            return false;
        }

        if (StorageTypeEnum.isLocal(file.getStorageType())) {
            return serveLocalFile(file, response, forceDownload);
        } else {
            // OSS存储: 从MinIO下载文件并流式输出
            return serveOssFile(file, response, forceDownload);
        }
    }

    /**
     * 从OSS(MinIO)下载文件并流式输出
     */
    private boolean serveOssFile(KbFile file, HttpServletResponse response, boolean forceDownload) {
        try {
            OssClient storage = OssFactory.instance();
            String objectKey = storage.removeBaseUrl(file.getPhysicalPath());

            String contentType = StringUtils.isNotBlank(file.getMimeType())
                ? file.getMimeType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            response.setContentType(contentType);

            String encodedName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

            if (forceDownload) {
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encodedName);
            } else {
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename*=UTF-8''" + encodedName);
            }

            storage.download(objectKey, response.getOutputStream(), response::setContentLengthLong);
            return true;
        } catch (Exception e) {
            log.error("从OSS下载文件失败: fileId={}, path={}", file.getFileId(), file.getPhysicalPath(), e);
            writeError(response, 500, "获取文件失败");
            return false;
        }
    }

    /**
     * 从本地磁盘读取文件并流式输出
     */
    private boolean serveLocalFile(KbFile file, HttpServletResponse response, boolean forceDownload) {
        File localFile = new File(file.getPhysicalPath());
        if (!localFile.exists()) {
            writeError(response, 404, "本地文件不存在");
            return false;
        }

        String contentType = StringUtils.isNotBlank(file.getMimeType())
            ? file.getMimeType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        response.setContentType(contentType);
        response.setContentLengthLong(localFile.length());

        String encodedName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
            .replaceAll("\\+", "%20");

        if (forceDownload) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedName);
        } else {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename*=UTF-8''" + encodedName);
        }

        try (FileInputStream fis = new FileInputStream(localFile);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            return true;
        } catch (IOException e) {
            log.error("输出本地文件失败: fileId={}, path={}", file.getFileId(), file.getPhysicalPath(), e);
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + message + "\",\"data\":null}");
        } catch (IOException ignored) {
        }
    }

}