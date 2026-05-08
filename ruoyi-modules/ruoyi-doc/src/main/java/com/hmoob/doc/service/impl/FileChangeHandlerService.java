package com.hmoob.doc.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.doc.domain.*;
import com.hmoob.doc.domain.bo.KbFolderBo;
import com.hmoob.doc.enums.ChangeProcessStatusEnum;
import com.hmoob.doc.enums.FileChangeTypeEnum;
import com.hmoob.doc.mapper.KbDocMapper;
import com.hmoob.doc.mapper.KbFileChangeLogMapper;
import com.hmoob.doc.mapper.KbFileMapper;
import com.hmoob.doc.mapper.KbFolderMapper;
import com.hmoob.doc.service.IKbDocService;
import com.hmoob.doc.service.IKbFolderService;
import com.hmoob.doc.es.service.IKbEsIndexService;
import com.hmoob.doc.utils.FileUtil;
import com.hmoob.doc.watch.ChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件变化处理服务
 * 负责将监控到的文件变化同步到数据库和ES
 * <p>
 * 内部职责划分：
 * - FileHandler: 文件变化处理
 * - DirectoryHandler: 目录变化处理
 * - ChangeLogHelper: 变化日志记录
 * - PathHelper: 路径处理工具
 *
 * @author hmoob
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileChangeHandlerService {

    private final KbFileChangeLogMapper changeLogMapper;
    private final KbDocMapper docMapper;
    private final KbFileMapper fileMapper;
    private final KbFolderMapper folderMapper;
    private final IKbDocService docService;
    private final IKbFolderService folderService;
    private final IKbEsIndexService esIndexService;
    private final TransactionTemplate transactionTemplate;

    // ========== 内部职责处理器 ==========

    /** 文件变化处理器 */
    private final FileHandler fileHandler = new FileHandler();
    /** 目录变化处理器 */
    private final DirectoryHandler directoryHandler = new DirectoryHandler();
    /** 变化日志助手 */
    private final ChangeLogHelper changeLogHelper = new ChangeLogHelper();
    /** 路径处理助手 */
    private final PathHelper pathHelper = new PathHelper();

    // ========== 核心入口方法 ==========

    /**
     * 异步处理文件变化事件
     * 注意：异步线程中无Sa-Token上下文，需忽略数据权限检查
     *
     * @param event  变化事件
     * @param config 监控配置
     */
    @Async
    public void handleChangeEvent(ChangeEvent event, KbFileWatchConfig config) {
        log.info("处理文件变化事件: type={}, path={}, isDir={}", event.type(), event.path(), event.isDirectory());

        // 忽略数据权限（异步线程无登录上下文）
        InterceptorIgnoreHelper.handle(
            com.baomidou.mybatisplus.core.plugins.IgnoreStrategy.builder().dataPermission(true).build()
        );

        KbFileChangeLog changeLog = null;
        try {
            // 1. 记录变化日志
            changeLog = changeLogHelper.buildChangeLog(event, config);
            changeLogMapper.insert(changeLog);

            // 2. 处理文件/目录变化
            if (event.isDirectory()) {
                directoryHandler.handleDirectoryChange(event, config, changeLog);
            } else {
                fileHandler.handleFileChange(event, config, changeLog);
            }

            // 3. 更新处理状态为成功
            changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.SUCCESS, "处理成功");

        } catch (Exception e) {
            log.error("处理文件变化事件失败: path={}", event.path(), e);
            if (changeLog != null) {
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.FAILED, e.getMessage());
            }
        } finally {
            // 清除忽略策略
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    // ========== 公共辅助方法（供内部类共享使用） ==========

    /**
     * 安全从ES删除文档（忽略异常）
     *
     * @param docId 文档ID
     */
    private void safeDeleteFromEs(Long docId) {
        try {
            esIndexService.deleteDocument(docId);
        } catch (Exception e) {
            log.warn("从 ES 删除文档失败: docId={}", docId, e);
        }
    }

    /**
     * 根据文件ID查找文档
     *
     * @param fileId 文件ID
     * @return 文档实体
     */
    private KbDoc findDocByFileId(Long fileId) {
        LambdaQueryWrapper<KbDoc> query = Wrappers.lambdaQuery();
        query.eq(KbDoc::getFileId, fileId);
        return docMapper.selectOne(query);
    }

    // ========== 文件变化处理内部类 ==========

    /**
     * 文件变化处理器
     * 负责处理文件的创建、修改、删除事件
     */
    private class FileHandler {

        /**
         * 处理文件变化
         *
         * @param event     变化事件
         * @param config    监控配置
         * @param changeLog 变化日志
         */
        public void handleFileChange(ChangeEvent event, KbFileWatchConfig config, KbFileChangeLog changeLog) throws IOException {
            Path filePath = Path.of(event.path());

            switch (event.type()) {
                case CREATED -> handleFileCreated(filePath, config, changeLog);
                case MODIFIED -> handleFileModified(filePath, config, changeLog);
                case DELETED -> handleFileDeleted(filePath, config, changeLog);
                case RENAMED -> handleFileRenamed(filePath, event.oldPath(), config, changeLog);
            }
        }

        /**
         * 处理文件新增：自动添加到文档库和ES
         *
         * @param filePath  文件路径
         * @param config    监控配置
         * @param changeLog 变化日志
         */
        private void handleFileCreated(Path filePath, KbFileWatchConfig config, KbFileChangeLog changeLog) throws IOException {
            // 检查是否启用自动添加
            if (!isAutoAddEnabled(config)) {
                log.info("自动添加文档未启用，跳过: {}", filePath);
                return;
            }

            String fileName = filePath.getFileName().toString();
            String fileType = FileUtil.getFileExtension(fileName);

            // 检查文件类型是否支持
            if (!FileUtil.isFileTypeSupported(fileType)) {
                log.info("不支持的文件类型，跳过: {} ({})", fileName, fileType);
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.IGNORED, "不支持的文件类型: " + fileType);
                return;
            }

            // 检查文件是否已存在（去重）
            String sha256 = DigestUtil.sha256Hex(Files.newInputStream(filePath));
            KbFile existFile = findFileBySha256(sha256);
            if (existFile != null) {
                log.info("文件已存在(SHA256相同), 跳过新增: {}", fileName);
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.IGNORED, "文件已存在(hash相同)");
                return;
            }

            // 在事务中创建文件和文档记录
            transactionTemplate.executeWithoutResult(status -> {
                createFileAndDoc(filePath, config, changeLog, sha256, fileName, fileType);
            });
        }

        /**
         * 处理文件修改：更新文件信息和ES索引
         *
         * @param filePath  文件路径
         * @param config    监控配置
         * @param changeLog 变化日志
         */
        private void handleFileModified(Path filePath, KbFileWatchConfig config, KbFileChangeLog changeLog) throws IOException {
            String filePathStr = pathHelper.normalizePath(filePath);

            // 通过物理路径查找已有文件记录
            KbFile existFile = findFileByPhysicalPath(filePathStr);
            if (existFile == null) {
                log.info("文件未在文档库中，按新增处理: {}", filePath);
                handleFileCreated(filePath, config, changeLog);
                return;
            }

            // 计算新hash
            String newSha256 = DigestUtil.sha256Hex(Files.newInputStream(filePath));
            changeLog.setOldHash(existFile.getSha256());
            changeLog.setContentHash(newSha256);

            // 更新文件信息
            existFile.setSha256(newSha256);
            existFile.setFileSize(Files.size(filePath));
            fileMapper.updateById(existFile);

            // 查找关联文档并更新
            updateDocForModifiedFile(existFile, config, changeLog);

            changeLogMapper.updateById(changeLog);
            log.info("文件修改处理完成: {}", filePath);
        }

        /**
         * 处理文件重命名：更新文件路径和文档名称
         *
         * @param newFilePath  新文件路径
         * @param oldFilePath  旧文件路径
         * @param config       监控配置
         * @param changeLog    变化日志
         */
        private void handleFileRenamed(Path newFilePath, String oldFilePath, KbFileWatchConfig config, KbFileChangeLog changeLog) {
            String oldPathStr = pathHelper.normalizePath(Path.of(oldFilePath));
            String newPathStr = pathHelper.normalizePath(newFilePath);
            String newFileName = newFilePath.getFileName().toString();

            log.info("处理文件重命名: {} -> {}", oldFilePath, newPathStr);

            // 通过旧路径查找文件记录
            KbFile existFile = findFileByPhysicalPath(oldPathStr);
            if (existFile == null) {
                log.info("旧文件未在文档库中，忽略重命名事件: {}", oldFilePath);
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.IGNORED, "旧文件不在文档库中");
                return;
            }

            // 更新文件记录
            existFile.setPhysicalPath(newPathStr);
            existFile.setOriginalName(newFileName);
            fileMapper.updateById(existFile);

            // 查找关联文档并更新名称
            KbDoc doc = findDocByFileId(existFile.getFileId());
            if (doc != null) {
                doc.setDocName(newFileName);
                doc.setDocTitle(FileUtil.extractFileTitle(newFileName));
                docMapper.updateById(doc);
                changeLog.setDocId(doc.getDocId());

                // 更新ES索引中的文档名称
                if (isAutoIndexEnabled(config)) {
                    safeUpdateEsIndex(doc.getDocId());
                }
            }

            changeLogMapper.updateById(changeLog);
            log.info("文件重命名处理完成: {} -> {} (docId={})", oldFilePath, newPathStr, doc != null ? doc.getDocId() : null);
        }

        /**
         * 处理文件删除：更新文档状态和ES
         * 注意：重命名事件已在Watcher层面合并，此方法处理真正的删除操作
         *
         * @param filePath  文件路径
         * @param config    监控配置
         * @param changeLog 变化日志
         */
        private void handleFileDeleted(Path filePath, KbFileWatchConfig config, KbFileChangeLog changeLog) {
            // 检查文件是否真的不存在了
            if (Files.exists(filePath)) {
                log.info("文件仍然存在，忽略删除事件: {}", filePath);
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.IGNORED, "文件仍然存在");
                return;
            }

            String filePathStr = pathHelper.normalizePath(filePath);
            KbFile existFile = findFileByPhysicalPath(filePathStr);
            if (existFile == null) {
                log.info("文件未在文档库中，忽略删除事件: {}", filePath);
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.IGNORED, "文件不在文档库中");
                return;
            }

            // 在事务中删除文件和文档记录
            transactionTemplate.executeWithoutResult(status -> {
                deleteFileAndDoc(existFile, changeLog);
            });

            log.info("文件删除处理完成: {}", filePath);
        }

        // ========== 文件处理辅助方法 ==========

        /**
         * 检查是否启用自动添加文档
         */
        private boolean isAutoAddEnabled(KbFileWatchConfig config) {
            return config.getAutoAddDoc() != null && config.getAutoAddDoc() == 1;
        }

        /**
         * 根据SHA256查找文件
         */
        private KbFile findFileBySha256(String sha256) {
            LambdaQueryWrapper<KbFile> query = Wrappers.lambdaQuery();
            query.eq(KbFile::getSha256, sha256);
            return fileMapper.selectOne(query);
        }

        /**
         * 根据物理路径查找文件
         */
        private KbFile findFileByPhysicalPath(String physicalPath) {
            LambdaQueryWrapper<KbFile> query = Wrappers.lambdaQuery();
            query.eq(KbFile::getPhysicalPath, physicalPath);
            return fileMapper.selectOne(query);
        }

        /**
         * 创建文件和文档记录
         */
        private void createFileAndDoc(Path filePath, KbFileWatchConfig config, KbFileChangeLog changeLog,
                                        String sha256, String fileName, String fileType) {
            try {
                // 保存文件记录
                KbFile kbFile = buildKbFile(filePath, sha256, fileName, fileType);
                fileMapper.insert(kbFile);

                // 创建文档记录
                KbDoc doc = buildKbDoc(kbFile, config, fileName, fileType);
                docMapper.insert(doc);

                // 更新变化记录的关联文档ID
                changeLog.setDocId(doc.getDocId());
                changeLogMapper.updateById(changeLog);

                // 异步解析并索引到ES
                if (isAutoIndexEnabled(config)) {
                    safeAsyncProcessDoc(doc.getDocId());
                }

                log.info("文件自动添加到文档库成功: {} -> docId={}", fileName, doc.getDocId());
            } catch (IOException e) {
                throw new RuntimeException("创建文件记录失败: " + fileName, e);
            }
        }

        /**
         * 构建文件实体
         */
        private KbFile buildKbFile(Path filePath, String sha256, String fileName, String fileType) throws IOException {
            KbFile kbFile = new KbFile();
            kbFile.setSha256(sha256);
            kbFile.setPhysicalPath(pathHelper.normalizePath(filePath));
            kbFile.setStorageType("local");
            kbFile.setOriginalName(fileName);
            kbFile.setFileSize(Files.size(filePath));
            kbFile.setFileType(fileType);
            kbFile.setMimeType(Files.probeContentType(filePath));
            kbFile.setStatus(1);
            return kbFile;
        }

        /**
         * 构建文档实体
         */
        private KbDoc buildKbDoc(KbFile kbFile, KbFileWatchConfig config, String fileName, String fileType) {
            KbDoc doc = new KbDoc();
            doc.setFolderId(config.getFolderId() != null ? config.getFolderId() : 0L);
            doc.setFileId(kbFile.getFileId());
            doc.setDocName(fileName);
            doc.setDocTitle(FileUtil.extractFileTitle(fileName));
            doc.setFileType(fileType);
            doc.setFileSize(kbFile.getFileSize());
            doc.setStatus(1);
            doc.setReleaseFlag(1);
            doc.setCurrentVersion(1);
            doc.setViewCount(0L);
            doc.setDownloadCount(0L);
            doc.setCommentCount(0L);
            doc.setFavouriteCount(0L);
            return doc;
        }

        /**
         * 更新修改文件的关联文档
         */
        private void updateDocForModifiedFile(KbFile existFile, KbFileWatchConfig config, KbFileChangeLog changeLog) {
            KbDoc doc = findDocByFileId(existFile.getFileId());
            if (doc == null) {
                return;
            }

            doc.setFileSize(existFile.getFileSize());
            docMapper.updateById(doc);
            changeLog.setDocId(doc.getDocId());

            // 重新解析并索引到ES
            if (isAutoIndexEnabled(config)) {
                safeParseAndIndexDoc(doc.getDocId());
            }
        }

        /**
         * 删除文件和文档记录
         */
        private void deleteFileAndDoc(KbFile existFile, KbFileChangeLog changeLog) {
            KbDoc doc = findDocByFileId(existFile.getFileId());
            if (doc != null) {
                changeLog.setDocId(doc.getDocId());

                // 从 ES 中删除
                safeDeleteFromEs(doc.getDocId());

                // 删除文档记录
                docService.deleteDocById(doc.getDocId());
            }

            // 删除文件记录
            fileMapper.deleteById(existFile.getFileId());
            changeLogMapper.updateById(changeLog);
        }

        /**
         * 检查是否启用自动索引
         */
        private boolean isAutoIndexEnabled(KbFileWatchConfig config) {
            return config.getAutoIndex() != null && config.getAutoIndex() == 1;
        }

        /**
         * 安全执行异步文档处理
         */
        private void safeAsyncProcessDoc(Long docId) {
            try {
                docService.asyncProcessDoc(docId);
            } catch (Exception e) {
                log.warn("异步处理文档失败: docId={}", docId, e);
            }
        }

        /**
         * 安全执行文档解析和索引
         */
        private void safeParseAndIndexDoc(Long docId) {
            try {
                docService.parseAndIndexDoc(docId);
            } catch (Exception e) {
                log.warn("重新索引文档失败: docId={}", docId, e);
            }
        }

        /**
         * 安全更新ES索引（仅更新元数据，不重新解析内容）
         */
        private void safeUpdateEsIndex(Long docId) {
            try {
                docService.updateEsIndex(docId);
            } catch (Exception e) {
                log.warn("更新ES索引失败: docId={}", docId, e);
            }
        }
    }

    // ========== 目录变化处理内部类 ==========

    /**
     * 目录变化处理器
     * 负责处理目录的创建、修改、删除事件
     */
    private class DirectoryHandler {

        /** 目录创建锁（按路径分级，避免并发创建同名目录） */
        private final Map<String, Object> folderCreateLocks = new ConcurrentHashMap<>();

        /**
         * 处理目录变化
         *
         * @param event     变化事件
         * @param config    监控配置
         * @param changeLog 变化日志
         */
        public void handleDirectoryChange(ChangeEvent event, KbFileWatchConfig config, KbFileChangeLog changeLog) {
            Path dirPath = Path.of(event.path());
            String dirName = pathHelper.getDirName(dirPath);

            switch (event.type()) {
                case CREATED -> handleDirectoryCreated(dirPath, dirName, config, changeLog);
                case MODIFIED -> handleDirectoryModified(dirPath, dirName, config, changeLog);
                case DELETED -> handleDirectoryDeleted(dirPath, config, changeLog);
                case RENAMED -> handleDirectoryRenamed(dirPath, event.oldPath(), config, changeLog);
            }
        }

        /**
         * 处理目录创建
         */
        private void handleDirectoryCreated(Path dirPath, String dirName, KbFileWatchConfig config, KbFileChangeLog changeLog) {
            log.info("检测到新目录: {}", dirPath);
            try {
                Long folderId = createOrUpdateFolder(dirPath, config);
                changeLog.setFolderId(folderId);
                log.info("目录自动创建成功: {} -> folderId={}", dirPath, folderId);
            } catch (Exception e) {
                log.error("目录自动创建失败: {}", dirPath, e);
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.FAILED, "目录创建失败: " + e.getMessage());
            }
        }

        /**
         * 处理目录重命名
         * 更新目录本身及所有子目录、文件的路径信息
         *
         * @param newDirPath  新目录路径
         * @param oldDirPath  旧目录路径
         * @param config      监控配置
         * @param changeLog   变化日志
         */
        private void handleDirectoryRenamed(Path newDirPath, String oldDirPath, KbFileWatchConfig config, KbFileChangeLog changeLog) {
            String oldPathStr = pathHelper.normalizePath(Path.of(oldDirPath));
            String newPathStr = pathHelper.normalizePath(newDirPath);
            String newDirName = pathHelper.getDirName(newDirPath);

            log.info("处理目录重命名: {} -> {}", oldDirPath, newPathStr);

            // 通过旧路径查找目录记录
            Long kbParentFolderId = pathHelper.getKbParentFolderId(config);
            Path oldRelativePath = pathHelper.getRelativePath(Path.of(oldDirPath), config.getWatchPath());
            KbFolder existFolder = findFolderByRelativePath(oldRelativePath, kbParentFolderId);

            if (existFolder == null) {
                log.info("旧目录未在文档库中，忽略重命名事件: {}", oldDirPath);
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.IGNORED, "旧目录不在文档库中");
                return;
            }

            // 在事务中批量更新路径信息
            transactionTemplate.executeWithoutResult(status -> {
                // 1. 更新目录本身
                updateFolderPath(existFolder, newDirName, oldPathStr, newPathStr);
                changeLog.setFolderId(existFolder.getFolderId());

                // 2. 更新所有子目录的 folder_path
                updateChildFoldersPath(existFolder.getFolderId(), oldPathStr, newPathStr);

                // 3. 更新所有文件的 physical_path
                updateChildFilesPath(existFolder.getFolderId(), oldPathStr, newPathStr);

                log.info("目录重命名处理完成: {} -> {}, 已更新子目录和文件路径", oldDirPath, newPathStr);
            });
        }

        /**
         * 更新目录本身的路径信息
         */
        private void updateFolderPath(KbFolder folder, String newDirName, String oldPath, String newPath) {
            folder.setFolderName(newDirName);
            // folder_path 是逻辑路径（如 /父目录/当前目录），需要根据新物理路径计算
            // 这里简化处理：替换 folder_path 中的旧目录名
            if (folder.getFolderPath() != null) {
                String oldFolderName = Path.of(oldPath).getFileName().toString();
                String newFolderPath = folder.getFolderPath().replace(oldFolderName, newDirName);
                folder.setFolderPath(newFolderPath);
            }
            folderMapper.updateById(folder);
        }

        /**
         * 更新所有子目录的 folder_path（批量路径前缀替换）
         *
         * @param parentFolderId 父目录ID
         * @param oldPathPrefix  旧路径前缀
         * @param newPathPrefix  新路径前缀
         */
        private void updateChildFoldersPath(Long parentFolderId, String oldPathPrefix, String newPathPrefix) {
            // 查找所有子目录（folder_path 以旧路径开头）
            LambdaQueryWrapper<KbFolder> query = Wrappers.lambdaQuery();
            query.likeRight(KbFolder::getFolderPath, oldPathPrefix)
                .ne(KbFolder::getFolderId, parentFolderId);
            List<KbFolder> childFolders = folderMapper.selectList(query);

            for (KbFolder child : childFolders) {
                if (child.getFolderPath() != null) {
                    String newFolderPath = child.getFolderPath().replace(oldPathPrefix, newPathPrefix);
                    child.setFolderPath(newFolderPath);
                    folderMapper.updateById(child);
                    log.debug("更新子目录路径: folderId={}, newPath={}", child.getFolderId(), newFolderPath);
                }
            }

            log.info("已更新 {} 个子目录的路径", childFolders.size());
        }

        /**
         * 更新所有文件的 physical_path（批量路径前缀替换）
         *
         * @param parentFolderId 父目录ID
         * @param oldPathPrefix  旧路径前缀
         * @param newPathPrefix  新路径前缀
         */
        private void updateChildFilesPath(Long parentFolderId, String oldPathPrefix, String newPathPrefix) {
            // 查找该目录及其所有子目录下的文档
            List<Long> allFolderIds = collectAllFolderIds(parentFolderId);

            // 查找所有文件（physical_path 以旧路径开头）
            LambdaQueryWrapper<KbFile> query = Wrappers.lambdaQuery();
            query.likeRight(KbFile::getPhysicalPath, oldPathPrefix);
            List<KbFile> childFiles = fileMapper.selectList(query);

            int updatedCount = 0;
            for (KbFile file : childFiles) {
                if (file.getPhysicalPath() != null) {
                    String newPhysicalPath = file.getPhysicalPath().replace(oldPathPrefix, newPathPrefix);
                    file.setPhysicalPath(newPhysicalPath);
                    fileMapper.updateById(file);

                    // 更新关联文档的名称（如果文件名变化）
                    KbDoc doc = findDocByFileId(file.getFileId());
                    if (doc != null) {
                        String newFileName = Path.of(newPhysicalPath).getFileName().toString();
                        doc.setDocName(newFileName);
                        docMapper.updateById(doc);
                    }

                    updatedCount++;
                    log.debug("更新文件路径: fileId={}, newPath={}", file.getFileId(), newPhysicalPath);
                }
            }

            log.info("已更新 {} 个文件的路径", updatedCount);
        }

        /**
         * 收集目录及其所有子目录的ID
         */
        private List<Long> collectAllFolderIds(Long folderId) {
            List<Long> result = new ArrayList<>();
            result.add(folderId);

            List<KbFolder> children = findChildFolders(folderId);
            for (KbFolder child : children) {
                result.addAll(collectAllFolderIds(child.getFolderId()));
            }

            return result;
        }

        /**
         * 处理目录修改（子文件变化）
         */
        private void handleDirectoryModified(Path dirPath, String dirName, KbFileWatchConfig config, KbFileChangeLog changeLog) {
            log.info("检测到目录修改（子文件变化）: {}", dirPath);
            try {
                Long folderId = createOrUpdateFolder(dirPath, config);
                changeLog.setFolderId(folderId);
                updateFolderDocCount(folderId);
                log.info("目录更新成功，重新计算文档数量: folderId={}", folderId);
            } catch (Exception e) {
                log.error("目录更新失败: {}", dirPath, e);
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.FAILED, "目录更新失败: " + e.getMessage());
            }
        }

        /**
         * 处理目录删除
         * 递归删除该目录及其所有子目录、子目录下的文档
         */
        private void handleDirectoryDeleted(Path dirPath, KbFileWatchConfig config, KbFileChangeLog changeLog) {
            log.info("检测到目录删除: {}", dirPath);
            try {
                Long kbParentFolderId = pathHelper.getKbParentFolderId(config);
                Path relativePath = pathHelper.getRelativePath(dirPath, config.getWatchPath());

                KbFolder existFolder = findFolderByRelativePath(relativePath, kbParentFolderId);
                if (existFolder != null) {
                    // 在事务中递归删除目录及其子目录、文档
                    transactionTemplate.executeWithoutResult(status -> {
                        deleteFolderRecursively(existFolder.getFolderId());
                    });
                    changeLog.setFolderId(existFolder.getFolderId());
                    log.info("目录及子目录已删除: folderId={}, path={}", existFolder.getFolderId(), dirPath);
                } else {
                    log.warn("未找到对应的目录记录: {}", dirPath);
                }
            } catch (Exception e) {
                log.error("目录删除失败: {}", dirPath, e);
                changeLogHelper.updateStatus(changeLog, ChangeProcessStatusEnum.FAILED, "目录删除失败: " + e.getMessage());
            }
        }

        /**
         * 递归删除目录及其子目录、文档
         * 删除顺序：先删除子目录（深度优先），再删除当前目录
         *
         * @param folderId 目录ID
         */
        private void deleteFolderRecursively(Long folderId) {
            // 1. 查找并递归删除所有子目录
            List<KbFolder> childFolders = findChildFolders(folderId);
            for (KbFolder child : childFolders) {
                deleteFolderRecursively(child.getFolderId());
            }

            // 2. 删除当前目录下的所有文档
            deleteDocsInFolder(folderId);

            // 3. 删除当前目录
            folderMapper.deleteById(folderId);
            log.info("删除目录: folderId={}", folderId);
        }

        /**
         * 查找目录的所有子目录
         *
         * @param parentId 父目录ID
         * @return 子目录列表
         */
        private List<KbFolder> findChildFolders(Long parentId) {
            LambdaQueryWrapper<KbFolder> query = Wrappers.lambdaQuery();
            query.eq(KbFolder::getParentId, parentId)
                .eq(KbFolder::getDelFlag, SystemConstants.NORMAL);
            return folderMapper.selectList(query);
        }

        /**
         * 删除目录下的所有文档（包含ES索引）
         *
         * @param folderId 目录ID
         */
        private void deleteDocsInFolder(Long folderId) {
            LambdaQueryWrapper<KbDoc> query = Wrappers.lambdaQuery();
            query.eq(KbDoc::getFolderId, folderId);
            List<KbDoc> docs = docMapper.selectList(query);

            for (KbDoc doc : docs) {
                // 从 ES 中删除
                safeDeleteFromEs(doc.getDocId());

                // 删除关联的文件记录
                if (doc.getFileId() != null) {
                    fileMapper.deleteById(doc.getFileId());
                }

                // 删除文档记录
                docMapper.deleteById(doc.getDocId());
                log.info("删除文档: docId={}, folderId={}", doc.getDocId(), folderId);
            }
        }

        // ========== 目录处理辅助方法 ==========

        /**
         * 创建或更新目录记录
         * 监控根目录挂在配置的KB目录下，子目录保持文件系统的树状结构
         *
         * @param dirPath 目录路径
         * @param config  监控配置
         * @return 目录ID
         */
        private Long createOrUpdateFolder(Path dirPath, KbFileWatchConfig config) {
            Long kbParentFolderId = pathHelper.getKbParentFolderId(config);
            Path relativePath = pathHelper.getRelativePath(dirPath, config.getWatchPath());

            // 如果是监控根目录本身，直接挂在配置的父目录下
            if (relativePath.toString().isEmpty()) {
                return handleRootFolder(dirPath, kbParentFolderId);
            }

            // 递归创建/查找父目录链，保持树状结构
            return createFolderChain(relativePath, kbParentFolderId);
        }

        /**
         * 处理监控根目录
         */
        private Long handleRootFolder(Path dirPath, Long kbParentFolderId) {
            String rootDirName = pathHelper.getDirName(dirPath);
            KbFolder existRoot = findFolderByNameAndParent(rootDirName, kbParentFolderId);
            if (existRoot != null) {
                updateFolderDocCount(existRoot.getFolderId());
                return existRoot.getFolderId();
            }

            return createFolderAtPath(rootDirName, kbParentFolderId);
        }

        /**
         * 递归创建目录链
         */
        private Long createFolderChain(Path relativePath, Long kbParentFolderId) {
            Long currentParentId = kbParentFolderId;
            int nameCount = relativePath.getNameCount();

            for (int i = 0; i < nameCount; i++) {
                String folderName = relativePath.getName(i).toString();

                // 查找或创建当前层级的目录
                KbFolder existFolder = findFolderByNameAndParent(folderName, currentParentId);
                if (existFolder != null) {
                    currentParentId = existFolder.getFolderId();
                } else {
                    currentParentId = createFolderAtPath(folderName, currentParentId);
                }

                // 如果是最后一层，更新文档数量
                if (i == nameCount - 1 && currentParentId != null) {
                    updateFolderDocCount(currentParentId);
                }
            }

            return currentParentId;
        }

        /**
         * 在指定父目录下创建目录（使用细粒度锁避免并发创建）
         *
         * @param folderName     目录名称
         * @param parentFolderId 父目录ID
         * @return 新创建的目录ID
         */
        private Long createFolderAtPath(String folderName, Long parentFolderId) {
            String lockKey = "folder:" + parentFolderId + ":" + folderName;
            Object lock = folderCreateLocks.computeIfAbsent(lockKey, k -> new Object());

            synchronized (lock) {
                try {
                    // 锁内再次检查是否已存在
                    KbFolder existFolder = findFolderByNameAndParent(folderName, parentFolderId);
                    if (existFolder != null) {
                        log.debug("目录已存在，跳过创建: {} (parentId={})", folderName, parentFolderId);
                        return existFolder.getFolderId();
                    }

                    return doCreateFolder(folderName, parentFolderId);
                } finally {
                    folderCreateLocks.remove(lockKey);
                }
            }
        }

        /**
         * 执行创建目录
         */
        private Long doCreateFolder(String folderName, Long parentFolderId) {
            KbFolder parentFolder = null;
            if (parentFolderId > 0) {
                parentFolder = folderMapper.selectById(parentFolderId);
                if (parentFolder == null) {
                    log.warn("父目录不存在: parentFolderId={}", parentFolderId);
                }
            }

            KbFolderBo folderBo = buildFolderBo(folderName, parentFolderId, parentFolder);
            folderService.insertFolder(folderBo);

            // 查询刚创建的目录
            KbFolder newFolder = findLatestFolderByNameAndParent(folderName, parentFolderId);
            if (newFolder != null) {
                log.info("目录创建成功: {} -> folderId={}, deptId={}", folderName, newFolder.getFolderId(), newFolder.getDeptId());
                return newFolder.getFolderId();
            }

            log.warn("目录创建后未查询到记录: {} (parentId={})", folderName, parentFolderId);
            return null;
        }

        /**
         * 构建目录Bo对象
         */
        private KbFolderBo buildFolderBo(String folderName, Long parentFolderId, KbFolder parentFolder) {
            KbFolderBo folderBo = new KbFolderBo();
            folderBo.setFolderName(folderName);
            folderBo.setParentId(parentFolderId);
            folderBo.setStatus(1);
            folderBo.setSortOrder(0);
            folderBo.setDocCount(0L);

            if (parentFolder != null) {
                folderBo.setFolderLevel(parentFolder.getFolderLevel() + 1);
                folderBo.setFolderPath(parentFolder.getFolderPath() + "/" + folderName);
                folderBo.setDeptId(parentFolder.getDeptId());
            } else {
                folderBo.setFolderLevel(1);
                folderBo.setFolderPath("/" + folderName);
            }

            return folderBo;
        }

        /**
         * 根据目录名称和父目录ID查找目录
         */
        private KbFolder findFolderByNameAndParent(String folderName, Long parentId) {
            LambdaQueryWrapper<KbFolder> query = Wrappers.lambdaQuery();
            query.eq(KbFolder::getFolderName, folderName)
                .eq(KbFolder::getParentId, parentId)
                .last("LIMIT 1");
            return folderMapper.selectOne(query);
        }

        /**
         * 查找最新创建的目录（按创建时间倒序）
         */
        private KbFolder findLatestFolderByNameAndParent(String folderName, Long parentId) {
            LambdaQueryWrapper<KbFolder> query = Wrappers.lambdaQuery();
            query.eq(KbFolder::getFolderName, folderName)
                .eq(KbFolder::getParentId, parentId)
                .eq(KbFolder::getDelFlag, SystemConstants.NORMAL)
                .orderByDesc(KbFolder::getCreateTime)
                .last("LIMIT 1");
            return folderMapper.selectOne(query);
        }

        /**
         * 根据相对路径查找目录（沿路径逐层查找）
         */
        private KbFolder findFolderByRelativePath(Path relativePath, Long startParentId) {
            if (relativePath.toString().isEmpty()) {
                return null;
            }

            Long currentParentId = startParentId;
            int nameCount = relativePath.getNameCount();

            for (int i = 0; i < nameCount; i++) {
                String folderName = relativePath.getName(i).toString();
                KbFolder folder = findFolderByNameAndParent(folderName, currentParentId);

                if (folder == null) {
                    return null;
                }

                if (i == nameCount - 1) {
                    return folder;
                }

                currentParentId = folder.getFolderId();
            }

            return null;
        }

        /**
         * 更新目录文档数量
         */
        private void updateFolderDocCount(Long folderId) {
            if (folderId == null) {
                return;
            }

            LambdaQueryWrapper<KbDoc> query = Wrappers.lambdaQuery();
            query.eq(KbDoc::getFolderId, folderId);
            Long count = docMapper.selectCount(query);

            KbFolder folder = new KbFolder();
            folder.setFolderId(folderId);
            folder.setDocCount(count);
            folderMapper.updateById(folder);
        }
    }

    // ========== 变化日志处理内部类 ==========

    /**
     * 变化日志助手
     * 负责构建和更新变化日志记录
     */
    private class ChangeLogHelper {

        /**
         * 构建变化记录日志
         *
         * @param event  变化事件
         * @param config 监控配置
         * @return 变化日志实体
         */
        public KbFileChangeLog buildChangeLog(ChangeEvent event, KbFileWatchConfig config) {
            KbFileChangeLog changeLog = new KbFileChangeLog();
            changeLog.setConfigId(config.getConfigId());
            changeLog.setFilePath(event.path());
            changeLog.setFileName(Path.of(event.path()).getFileName().toString());
            changeLog.setIsDirectory(event.isDirectory() ? 1 : 0);
            changeLog.setChangeType(event.type().getCode());
            changeLog.setFolderId(config.getFolderId());
            changeLog.setProcessStatus(ChangeProcessStatusEnum.PENDING.getCode());
            changeLog.setTenantId(config.getTenantId());
            changeLog.setCreateTime(new Date());

            // 重命名事件，记录旧路径到 processMsg
            if (event.type() == FileChangeTypeEnum.RENAMED && event.oldPath() != null) {
                changeLog.setProcessMsg("旧路径: " + event.oldPath());
            }

            // 如果文件存在，计算大小和hash
            fillFileInfoIfExists(event, changeLog);

            return changeLog;
        }

        /**
         * 如果文件存在，填充文件信息
         */
        private void fillFileInfoIfExists(ChangeEvent event, KbFileChangeLog changeLog) {
            if (event.isDirectory()) {
                return;
            }

            Path path = Path.of(event.path());
            if (!Files.exists(path)) {
                return;
            }

            try {
                changeLog.setFileSize(Files.size(path));
                changeLog.setContentHash(DigestUtil.sha256Hex(Files.newInputStream(path)));
            } catch (IOException e) {
                log.warn("读取文件信息失败: {}", event.path(), e);
            }
        }

        /**
         * 更新变化记录处理状态
         *
         * @param changeLog 变化日志
         * @param status    处理状态
         * @param msg       处理消息
         */
        public void updateStatus(KbFileChangeLog changeLog, ChangeProcessStatusEnum status, String msg) {
            changeLog.setProcessStatus(status.getCode());
            changeLog.setProcessMsg(StrUtil.sub(msg, 0, 500));
            changeLog.setProcessTime(new Date());
            changeLogMapper.updateById(changeLog);
        }
    }

    // ========== 路径处理内部类 ==========

    /**
     * 路径处理助手
     * 负责路径相关的计算和处理
     */
    private class PathHelper {

        /**
         * 规范化路径（绝对路径）
         *
         * @param path 路径对象
         * @return 规范化后的路径字符串
         */
        public String normalizePath(Path path) {
            return path.toAbsolutePath().normalize().toString();
        }

        /**
         * 获取目录名称
         *
         * @param dirPath 目录路径
         * @return 目录名称
         */
        public String getDirName(Path dirPath) {
            return dirPath.getFileName() != null ? dirPath.getFileName().toString() : dirPath.toString();
        }

        /**
         * 获取KB父目录ID
         *
         * @param config 监控配置
         * @return 父目录ID，如果未配置则返回0
         */
        public Long getKbParentFolderId(KbFileWatchConfig config) {
            return (config.getFolderId() != null && config.getFolderId() > 0) ? config.getFolderId() : 0L;
        }

        /**
         * 计算相对路径
         *
         * @param targetPath  目标路径
         * @param basePath    基础路径
         * @return 相对路径
         */
        public Path getRelativePath(Path targetPath, String basePath) {
            return Path.of(basePath).relativize(targetPath);
        }
    }
}