package com.hmoob.doc.service.impl;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.oss.entity.UploadResult;
import com.hmoob.common.oss.factory.OssFactory;
import com.hmoob.common.satoken.utils.LoginHelper;
import com.hmoob.doc.config.KbUploadProperties;
import com.hmoob.doc.domain.KbDoc;
import com.hmoob.doc.domain.KbFile;
import com.hmoob.doc.domain.KbDocTopicType;
import com.hmoob.doc.domain.KbDocBusinessType;
import com.hmoob.doc.domain.bo.KbDocBo;
import com.hmoob.doc.domain.dto.KbDocUploadDto;
import com.hmoob.doc.domain.vo.KbDocVo;
import com.hmoob.doc.domain.vo.KbFileVo;
import com.hmoob.doc.enums.DocStatusEnum;
import com.hmoob.doc.enums.ConvertStatusEnum;
import com.hmoob.doc.enums.ReleaseFlagEnum;
import com.hmoob.doc.enums.PublicRemarkEnum;
import com.hmoob.doc.enums.FtiFlagEnum;
import com.hmoob.doc.mapper.KbDocMapper;
import com.hmoob.doc.mapper.KbFileMapper;
import com.hmoob.doc.mapper.KbDocTopicTypeMapper;
import com.hmoob.doc.mapper.KbDocBusinessTypeMapper;
import com.hmoob.doc.service.IKbDocService;
import com.hmoob.doc.service.IKbDocParserService;
import com.hmoob.doc.es.document.KbDocDocument;
import com.hmoob.doc.es.service.IKbEsIndexService;
import com.hmoob.doc.utils.FileUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * KB文档管理 服务实现
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KbDocServiceImpl implements IKbDocService {

    private final KbDocMapper baseMapper;
    private final KbFileMapper fileMapper;
    private final KbDocTopicTypeMapper topicTypeMapper;
    private final KbDocBusinessTypeMapper businessTypeMapper;
    private final IKbDocParserService parserService;
    private final IKbEsIndexService esIndexService;
    private final KbUploadProperties uploadProperties;

    /**
     * 分页查询文档管理数据
     */
    @Override
    public TableDataInfo<KbDocVo> selectPageDocList(KbDocBo bo, PageQuery pageQuery) {
        Page<KbDocVo> page = baseMapper.selectPageDocList(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    /**
     * 查询文档管理数据
     */
    @Override
    public List<KbDocVo> selectDocList(KbDocBo bo) {
        return baseMapper.selectDocList(buildQueryWrapper(bo));
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<KbDoc> buildQueryWrapper(KbDocBo bo) {
        LambdaQueryWrapper<KbDoc> lqw = Wrappers.lambdaQuery();
        lqw.eq(KbDoc::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getDocId()), KbDoc::getDocId, bo.getDocId());
        lqw.eq(ObjectUtil.isNotNull(bo.getFolderId()), KbDoc::getFolderId, bo.getFolderId());
        lqw.like(StringUtils.isNotBlank(bo.getDocName()), KbDoc::getDocName, bo.getDocName());
        lqw.like(StringUtils.isNotBlank(bo.getDocTitle()), KbDoc::getDocTitle, bo.getDocTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getSerialNumber()), KbDoc::getSerialNumber, bo.getSerialNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getFileType()), KbDoc::getFileType, bo.getFileType());
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), KbDoc::getCategory, bo.getCategory());
        lqw.like(StringUtils.isNotBlank(bo.getKeywords()), KbDoc::getKeywords, bo.getKeywords());
        lqw.eq(ObjectUtil.isNotNull(bo.getStatus()), KbDoc::getStatus, bo.getStatus());
        lqw.eq(ObjectUtil.isNotNull(bo.getReleaseFlag()), KbDoc::getReleaseFlag, bo.getReleaseFlag());
        lqw.orderByDesc(KbDoc::getCreateTime);
        return lqw;
    }

    /**
     * 根据文档ID查询信息
     */
    @Override
    public KbDocVo selectDocById(Long docId) {
        return baseMapper.selectVoById(docId);
    }

    /**
     * 根据文档编号查询信息
     */
    @Override
    public KbDocVo selectDocBySerialNumber(String serialNumber) {
        return baseMapper.selectBySerialNumber(serialNumber);
    }

    /**
     * 上传文档
     * 包含文件上传、文档保存、异步解析和ES索引
     * 支持 OSS / 本地存储 两种模式
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocVo uploadDoc(MultipartFile file, KbDocUploadDto dto) {
        // 1. 校验文件
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String fileType = FileUtil.getFileExtension(originalName);

        // 检查文件类型是否支持
        if (!FileUtil.isFileTypeSupported(fileType)) {
            throw new ServiceException("不支持的文件格式: " + fileType);
        }

        // 2. 计算SHA256并检查重复
        String sha256 = FileUtil.calculateSha256(file);
        KbFileVo existingFile = checkFileDuplicate(sha256);
        if (existingFile != null) {
            throw new ServiceException("该文件已经存在，无需重复上传。源文件: " + existingFile.getOriginalName());
        }

        // 3. 保存文件（OSS 或 本地）
        String physicalPath;
        String storageType;
        if (uploadProperties.useLocalStorage()) {
            // 本地存储模式
            physicalPath = saveToLocal(file, originalName);
            storageType = "local";
            log.info("文件保存到本地: {}", physicalPath);
        } else {
            // OSS 模式（含降级）
            try {
                UploadResult uploadResult = OssFactory.instance()
                    .uploadSuffix(file.getBytes(), originalName, file.getContentType());
                physicalPath = uploadResult.getUrl();
                storageType = "oss";
                log.info("文件上传到OSS: {}", physicalPath);
            } catch (Exception e) {
                log.warn("OSS上传失败，尝试降级到本地存储: {}", e.getMessage());
                if (uploadProperties.allowFallback()) {
                    physicalPath = saveToLocal(file, originalName);
                    storageType = "local";
                    log.info("降级保存到本地: {}", physicalPath);
                } else {
                    throw new ServiceException("文件上传失败: " + e.getMessage());
                }
            }
        }

        // 4. 保存文件信息
        KbFile kbFile = new KbFile();
        kbFile.setSha256(sha256);
        kbFile.setPhysicalPath(physicalPath);
        kbFile.setStorageType(storageType);
        kbFile.setOriginalName(originalName);
        kbFile.setFileSize(file.getSize());
        kbFile.setFileType(fileType);
        kbFile.setMimeType(file.getContentType());
        kbFile.setStatus(1);
        fileMapper.insert(kbFile);
        Long fileId = kbFile.getFileId();

        // 5. 创建文档记录
        KbDoc doc = new KbDoc();
        doc.setFolderId(dto.getFolderId() != null ? dto.getFolderId() : 0L);
        doc.setFileId(fileId);
        doc.setDocName(originalName);
        doc.setDocTitle(FileUtil.getDocTitle(dto.getDocTitle(), originalName));
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setCategory(dto.getCategory());
        doc.setKeywords(dto.getKeywords());
        doc.setOrgCode(dto.getOrgCode());
        doc.setDepId(dto.getDepId());
        doc.setPublicRemark(dto.getPublicRemark() != null ? dto.getPublicRemark() : PublicRemarkEnum.PRIVATE.getCode());
        doc.setRemark(dto.getRemark());
        doc.setStatus(DocStatusEnum.PENDING.getCode());
        doc.setReleaseFlag(ReleaseFlagEnum.PUBLISHED.getCode());
        doc.setCurrentVersion(1);
        doc.setViewCount(0L);
        doc.setDownloadCount(0L);
        doc.setCommentCount(0L);
        doc.setFavouriteCount(0L);

        // 生成文档编号
        doc.setSerialNumber(generateSerialNumber());

        // 设置转换状态
        if (FileUtil.isPdf(fileType)) {
            doc.setConvertFlag(ConvertStatusEnum.NONE.getCode());
            doc.setPreviewFileId(fileId);
            doc.setOriginalPreviewFileId(fileId);
        } else if (FileUtil.needsConversion(fileType)) {
            doc.setConvertFlag(ConvertStatusEnum.PROCESSING.getCode());
        } else {
            doc.setConvertFlag(ConvertStatusEnum.NONE.getCode());
        }

        // 设置全文检索状态
        doc.setFtiFlag(FtiFlagEnum.NO.getCode());

        baseMapper.insert(doc);
        Long docId = doc.getDocId();

        // 6. 保存主题关联
        if (dto.getTopicIds() != null && !dto.getTopicIds().isEmpty()) {
            for (Long topicId : dto.getTopicIds()) {
                KbDocTopicType topicType = new KbDocTopicType();
                topicType.setDocId(docId);
                topicType.setTopicId(topicId);
                topicTypeMapper.insert(topicType);
            }
        }

        // 7. 保存业务类型关联
        if (dto.getBusinessTypes() != null && !dto.getBusinessTypes().isEmpty()) {
            for (String businessType : dto.getBusinessTypes()) {
                KbDocBusinessType docBusinessType = new KbDocBusinessType();
                docBusinessType.setDocId(docId);
                docBusinessType.setBusinessType(businessType);
                businessTypeMapper.insert(docBusinessType);
            }
        }

        // 8. 异步处理（解析内容、索引ES）
        asyncProcessDoc(docId);

        log.info("文档上传成功: docId={}, docName={}, fileId={}, storageType={}", docId, originalName, fileId, storageType);

        return baseMapper.selectVoById(docId);
    }

    /**
     * 异步处理文档（解析内容、索引ES）
     */
    @Async
    @Override
    public void asyncProcessDoc(Long docId) {
        try {
            parseAndIndexDoc(docId);
        } catch (Exception e) {
            log.error("异步处理文档失败: docId={}", docId, e);
            // 更新文档状态为处理失败
            baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
                .set(KbDoc::getFtiFlag, FtiFlagEnum.FAILED.getCode())
                .set(KbDoc::getProcessMsg, "异步处理失败: " + e.getMessage())
                .eq(KbDoc::getDocId, docId));
        }
    }

    /**
     * 解析文档内容并索引到ES
     * 支持 OSS / 本地 两种读取方式
     */
    @Override
    public void parseAndIndexDoc(Long docId) {
        KbDoc doc = baseMapper.selectById(docId);
        if (doc == null) {
            log.warn("文档不存在: docId={}", docId);
            return;
        }

        KbFile file = fileMapper.selectById(doc.getFileId());
        if (file == null) {
            log.warn("文件不存在: fileId={}", doc.getFileId());
            return;
        }

        // 更新状态为处理中
        baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
            .set(KbDoc::getFtiFlag, FtiFlagEnum.PROCESSING.getCode())
            .eq(KbDoc::getDocId, docId));

        try {
            // 解析文档内容
            String content = "";
            if (parserService.isSupported(doc.getFileType())) {
                content = readFileContent(file);
            }

            // 构建ES文档对象
            KbDocDocument document = new KbDocDocument();
            document.setDocId(docId);
            document.setDocName(doc.getDocName());
            document.setDocTitle(doc.getDocTitle());
            document.setContent(content);
            document.setKeywords(doc.getKeywords());
            document.setFolderId(doc.getFolderId());
            document.setFileType(doc.getFileType());
            document.setStatus(doc.getStatus());
            document.setReleaseFlag(doc.getReleaseFlag());
            document.setViewCount(doc.getViewCount());
            document.setDownloadCount(doc.getDownloadCount());
            document.setCreateTime(doc.getCreateTime());
            document.setCreateBy(doc.getCreateBy() != null ? String.valueOf(doc.getCreateBy()) : null);
            document.setTenantId(LoginHelper.getTenantId());

            // 索引到ES
            esIndexService.indexDocument(document);

            // 更新文档状态为已处理
            baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
                .set(KbDoc::getFtiFlag, FtiFlagEnum.YES.getCode())
                .set(KbDoc::getIndexId, String.valueOf(docId))
                .eq(KbDoc::getDocId, docId));

            log.info("文档解析并索引ES成功: docId={}, contentLength={}", docId, content.length());

        } catch (Exception e) {
            log.error("文档解析或ES索引失败: docId={}", docId, e);
            baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
                .set(KbDoc::getFtiFlag, FtiFlagEnum.FAILED.getCode())
                .set(KbDoc::getProcessMsg, "解析或索引失败: " + e.getMessage())
                .eq(KbDoc::getDocId, docId));
        }
    }

    /**
     * 发布文档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishDoc(Long docId) {
        KbDoc doc = baseMapper.selectById(docId);
        if (ObjectUtil.isNull(doc)) {
            throw new ServiceException("文档不存在，无法发布");
        }
        if (doc.getStatus().equals(DocStatusEnum.PUBLISHED.getCode())) {
            throw new ServiceException("文档已发布，请勿重复操作");
        }
        if (doc.getStatus().equals(DocStatusEnum.ARCHIVED.getCode())) {
            throw new ServiceException("文档已归档，不允许发布");
        }

        int result = baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
            .set(KbDoc::getStatus, DocStatusEnum.PUBLISHED.getCode())
            .set(KbDoc::getReleaseFlag, ReleaseFlagEnum.PUBLISHED.getCode())
            .eq(KbDoc::getDocId, docId));

        // 更新ES索引中的发布状态
        if (result > 0) {
            try {
                updateEsIndex(docId);
            } catch (Exception e) {
                log.warn("更新ES索引发布状态失败: docId={}", docId, e);
            }
        }
        return result;
    }

    /**
     * 撤回文档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int withdrawDoc(Long docId) {
        KbDoc doc = baseMapper.selectById(docId);
        if (ObjectUtil.isNull(doc)) {
            throw new ServiceException("文档不存在，无法撤回");
        }
        if (!doc.getStatus().equals(DocStatusEnum.PUBLISHED.getCode())) {
            throw new ServiceException("只有已发布的文档才能撤回");
        }

        int result = baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
            .set(KbDoc::getStatus, DocStatusEnum.WITHDRAWN.getCode())
            .set(KbDoc::getReleaseFlag, ReleaseFlagEnum.UNPUBLISHED.getCode())
            .eq(KbDoc::getDocId, docId));

        // 更新ES索引中的发布状态
        if (result > 0) {
            try {
                updateEsIndex(docId);
            } catch (Exception e) {
                log.warn("更新ES索引撤回状态失败: docId={}", docId, e);
            }
        }
        return result;
    }

    /**
     * 增加浏览次数
     */
    @Override
    public int incrementViewCount(Long docId) {
        return baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
            .setSql("view_count = view_count + 1")
            .eq(KbDoc::getDocId, docId));
    }

    /**
     * 增加下载次数
     */
    @Override
    public int incrementDownloadCount(Long docId) {
        return baseMapper.update(null, new LambdaUpdateWrapper<KbDoc>()
            .setSql("download_count = download_count + 1")
            .eq(KbDoc::getDocId, docId));
    }

    /**
     * 校验文档名称是否唯一
     */
    @Override
    public boolean checkDocNameUnique(KbDocBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<KbDoc>()
            .eq(KbDoc::getDocName, bo.getDocName())
            .eq(KbDoc::getFolderId, bo.getFolderId())
            .ne(ObjectUtil.isNotNull(bo.getDocId()), KbDoc::getDocId, bo.getDocId()));
        return !exist;
    }

    /**
     * 新增保存文档信息
     */
    @Override
    public int insertDoc(KbDocBo bo) {
        KbDoc doc = MapstructUtils.convert(bo, KbDoc.class);
        if (doc.getStatus() == null) {
            doc.setStatus(DocStatusEnum.PENDING.getCode());
        }
        if (doc.getReleaseFlag() == null) {
            doc.setReleaseFlag(ReleaseFlagEnum.UNPUBLISHED.getCode());
        }
        if (doc.getCurrentVersion() == null) {
            doc.setCurrentVersion(1);
        }
        if (doc.getViewCount() == null) {
            doc.setViewCount(0L);
        }
        if (doc.getDownloadCount() == null) {
            doc.setDownloadCount(0L);
        }
        if (doc.getCommentCount() == null) {
            doc.setCommentCount(0L);
        }
        if (doc.getFavouriteCount() == null) {
            doc.setFavouriteCount(0L);
        }
        if (doc.getFtiFlag() == null) {
            doc.setFtiFlag(FtiFlagEnum.NO.getCode());
        }
        if (doc.getConvertFlag() == null) {
            doc.setConvertFlag(0);
        }
        return baseMapper.insert(doc);
    }

    /**
     * 修改保存文档信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDoc(KbDocBo bo) {
        KbDoc doc = MapstructUtils.convert(bo, KbDoc.class);
        KbDoc oldDoc = baseMapper.selectById(doc.getDocId());
        if (ObjectUtil.isNull(oldDoc)) {
            throw new ServiceException("文档不存在，无法修改");
        }

        int result = baseMapper.updateById(doc);

        // 检查是否需要更新ES索引(ES相关字段变化时)
        if (result > 0 && needUpdateEsIndex(oldDoc, doc)) {
            try {
                updateEsIndex(doc.getDocId());
            } catch (Exception e) {
                log.warn("修改文档后更新ES索引失败: docId={}", doc.getDocId(), e);
            }
        }
        return result;
    }

    /**
     * 删除文档管理信息
     */
    @Override
    public int deleteDocById(Long docId) {
        // 删除ES索引
        try {
            esIndexService.deleteDocument(docId);
        } catch (Exception e) {
            log.warn("删除ES索引失败: docId={}", docId, e);
        }
        return baseMapper.deleteById(docId);
    }


    /**
     * 检查文件是否重复
     */
    private KbFileVo checkFileDuplicate(String sha256) {
        return fileMapper.selectBySha256(sha256);
    }

    /**
     * 检查文件类型是否支持
     */
    @Override
    public boolean isFileTypeSupported(String fileType) {
        return FileUtil.isFileTypeSupported(fileType);
    }

    /**
     * 更新ES索引中的文档信息
     * 当文档的ES相关字段变化时调用(如发布/撤回、标题/关键词变更等)
     */
    @Override
    public void updateEsIndex(Long docId) {
        KbDoc doc = baseMapper.selectById(docId);
        if (doc == null) {
            log.warn("文档不存在，无法更新ES索引: docId={}", docId);
            return;
        }

        // 检查是否已索引
        if (!Objects.equals(doc.getFtiFlag(), FtiFlagEnum.YES.getCode())) {
            log.info("文档尚未完成索引，跳过更新: docId={}, ftiFlag={}", docId, doc.getFtiFlag());
            return;
        }

        // 构建ES文档更新对象(只更新必要字段)
        KbDocDocument document = new KbDocDocument();
        document.setDocId(docId);
        document.setDocName(doc.getDocName());
        document.setDocTitle(doc.getDocTitle());
        document.setKeywords(doc.getKeywords());
        document.setFolderId(doc.getFolderId());
        document.setFileType(doc.getFileType());
        document.setCategory(doc.getCategory());
        document.setStatus(doc.getStatus());
        document.setReleaseFlag(doc.getReleaseFlag());
        document.setViewCount(doc.getViewCount());
        document.setDownloadCount(doc.getDownloadCount());
        document.setOrgCode(doc.getOrgCode());
        document.setTenantId(LoginHelper.getTenantId());

        try {
            esIndexService.updateDocument(document);
            log.info("更新ES索引成功: docId={}", docId);
        } catch (Exception e) {
            log.error("更新ES索引失败: docId={}", docId, e);
        }
    }

    /**
     * 判断是否需要更新ES索引
     * 检查ES搜索相关的字段是否发生变化
     */
    private boolean needUpdateEsIndex(KbDoc oldDoc, KbDoc newDoc) {
        // ES搜索相关字段列表
        return !ObjectUtil.equal(oldDoc.getDocName(), newDoc.getDocName())
            || !ObjectUtil.equal(oldDoc.getDocTitle(), newDoc.getDocTitle())
            || !ObjectUtil.equal(oldDoc.getKeywords(), newDoc.getKeywords())
            || !ObjectUtil.equal(oldDoc.getFolderId(), newDoc.getFolderId())
            || !ObjectUtil.equal(oldDoc.getFileType(), newDoc.getFileType())
            || !ObjectUtil.equal(oldDoc.getCategory(), newDoc.getCategory())
            || !ObjectUtil.equal(oldDoc.getStatus(), newDoc.getStatus())
            || !ObjectUtil.equal(oldDoc.getReleaseFlag(), newDoc.getReleaseFlag())
            || !ObjectUtil.equal(oldDoc.getOrgCode(), newDoc.getOrgCode());
    }

    /**
     * 生成文档编号
     */
    private String generateSerialNumber() {
        return "KB" + System.currentTimeMillis() + StrUtil.sub(cn.hutool.core.util.IdUtil.fastSimpleUUID(), 0, 6);
    }

    // ========== 本地存储辅助方法 ==========

    /**
     * 保存文件到本地磁盘
     * 路径规则: {localPath}/{yyyy-MM-dd}/{uuid}.{ext}
     *
     * @return 文件绝对路径
     */
    private String saveToLocal(MultipartFile file, String originalName) {
        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String ext = FileUtil.getFileExtension(originalName);
            String fileName = cn.hutool.core.util.IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(ext) ? "." + ext : "");

            Path dirPath = Paths.get(uploadProperties.getLocalPath(), dateDir);
            Files.createDirectories(dirPath);

            Path filePath = dirPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("保存文件到本地失败: {}", originalName, e);
            throw new ServiceException("保存文件到本地失败: " + e.getMessage());
        }
    }

    /**
     * 读取文件内容（自动根据 storageType 选择读取方式）
     */
    private String readFileContent(KbFile file) {
        if ("local".equals(file.getStorageType())) {
            return readFromLocal(file.getPhysicalPath());
        }
        return readFromOss(file.getPhysicalPath());
    }

    /**
     * 从本地磁盘读取文件并解析内容
     */
    private String readFromLocal(String localPath) {
        try {
            File localFile = new File(localPath);
            if (!localFile.exists()) {
                log.warn("本地文件不存在: {}", localPath);
                return "";
            }
            try (InputStream is = new FileInputStream(localFile)) {
                return parserService.parseContent(is);
            }
        } catch (Exception e) {
            log.error("从本地读取文件失败: {}", localPath, e);
            return "";
        }
    }

    /**
     * 从OSS读取文件并解析内容
     */
    private String readFromOss(String ossUrl) {
        try (InputStream inputStream = OssFactory.instance().getObjectContent(ossUrl)) {
            return parserService.parseContent(inputStream);
        } catch (Exception e) {
            log.warn("从OSS获取文件内容失败: {}", e.getMessage());
            return "";
        }
    }

}
