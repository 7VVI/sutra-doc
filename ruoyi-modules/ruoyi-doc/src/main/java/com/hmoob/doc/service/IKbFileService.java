package com.hmoob.doc.service;

import com.hmoob.doc.domain.bo.KbFileBo;
import com.hmoob.doc.domain.vo.KbFileVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * KB文件管理 服务层
 *
 * @author hmoob
 */
public interface IKbFileService {

    /**
     * 根据文件ID查询信息
     *
     * @param fileId 文件ID
     * @return 文件信息
     */
    KbFileVo selectFileById(Long fileId);

    /**
     * 根据SHA256哈希值查询文件
     *
     * @param sha256 SHA256哈希值
     * @return 文件信息
     */
    KbFileVo selectBySha256(String sha256);

    /**
     * 计算文件的SHA256哈希值
     *
     * @param inputStream 输入流
     * @return SHA256哈希值
     */
    String calculateSha256(InputStream inputStream);

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件信息
     */
    KbFileVo uploadFile(MultipartFile file);

    /**
     * 新增保存文件信息
     *
     * @param bo 文件信息
     * @return 结果
     */
    int insertFile(KbFileBo bo);

    /**
     * 删除文件管理信息
     *
     * @param fileId 文件ID
     * @return 结果
     */
    int deleteFileById(Long fileId);

}