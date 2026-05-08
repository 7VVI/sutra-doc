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
 *
 * 核心特性：
 * 1. 目录删除/修改时，只触发目录事件，自动处理子项（不触发子项事件）
 * 2. 文件增删改重命名都需要触发事件
 * 3. 所有事件按顺序处理（由 KbFileWatchService 保证）
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
    /** 路径处理助手 */
    private final PathHelper pathHelper = new PathHelper();

    // ========== 核心入口方法 ==========

    /**
     * 处理文件变化事件（同步处理，由 KbFileWatchService 调度）
     * 注意：数据权限检查已在外层处理
     *
     * @param event  变化事件
     * @param config 监控配置
     * @return 处理结果（包含关联的 docId 和 folderId）
     */
    public ChangeProcessResult handleChangeEvent(ChangeEvent event, KbFileWatchConfig config) {
        log.info("处理文件变化事件: type={}, path={}, isDir={}", event.type(), event.path(), event.isDirectory());

        // 忽略数据权限（异步线程无登录上下文）
        InterceptorIgnoreHelper.handle(
            com.baomidou.mybatisplus.core.plugins.IgnoreStrategy.builder().dataPermission(true).build()
        );

        ChangeProcessResult result = new ChangeProcessResult();
        try {
            // 处理文件/目录变化
            if (event.isDirectory()) {
                result = directoryHandler.handleDirectoryChange(event, config);
            } else {
                result = fileHandler.handleFileChange(event, config);
            }

        } catch (IOException e) {
            log.error("处理文件变化事件失败(IOException): path={}", event.path(), e);
            throw new RuntimeException("处理文件变化事件失败", e);
        } catch (Exception e) {
            log.error("处理文件变化事件失败: path={}", event.path(), e);
            throw e;
        } finally {
            // 清除忽略策略
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
        return result;
    }

    /**
     * 处理结果对象
     */
    public static class ChangeProcessResult {
        private Long docId;
        private Long folderId;
        private String message;

        public Long getDocId() { return docId; }
        public void setDocId(Long docId) { this.docId = docId; }
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
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
     * 负责处理文件的创建、修改、删除、重命名事件
     */
    private class FileHandler {

        /**
         * 处理文件变化
         *
         * @param event     变化事件
         * @param config    监控配置
         * @return 处理结果（包含docId）
         */
        public ChangeProcessResult handleFileChange(ChangeEvent event, KbFileWatchConfig config) throws IOException {
            Path filePath = Path.of(event.path());
            ChangeProcessResult result = new ChangeProcessResult();

            switch (event.type()) {
                case CREATED -> result = handleFileCreated(filePath, config);
                case MODIFIED -> result = handleFileModified(filePath, config);
                case DELETED -> result = handleFileDeleted(filePath, config);
                case RENAMED -> result = handleFileRenamed(filePath, event.oldPath(), config);
            }
            return result;
        }

        /**
         * 处理文件新增：自动添加到文档库和ES
         *
         * @param filePath  文件路径
         * @param config    监控配置
         * @return 处理结果
         */
        private ChangeProcessResult handleFileCreated(Path filePath, KbFileWatchConfig config) throws IOException {
            ChangeProcessResult result = new ChangeProcessResult();
            // 检查是否启用自动添加
            if (!isAutoAddEnabled(config)) {
                log.info("自动添加文档未启用，跳过: {}", filePath);
                result.setMessage("自动添加文档未启用");
                return result;
            }

            String fileName = filePath.getFileName().toString();
            String fileType = FileUtil.getFileExtension(fileName);

            // 检查文件类型是否支持
            if (!FileUtil.isFileTypeSupported(fileType)) {
                log.info("不支持的文件类型，跳过: {} ({})", fileName, fileType);
                result.setMessage("不支持的文件类型: " + fileType);
                return result;
            }

            // 检查文件是否已存在（去重）
            String sha256;
            try (java.io.InputStream is = Files.newInputStream(filePath)) {
                sha256 = DigestUtil.sha256Hex(is);
            }
            KbFile existFile = findFileBySha256(sha256);
            if (existFile != null) {
                log.info("文件已存在(SHA256相同), 跳过新增: {}", fileName);
                result.setMessage("文件已存在(hash相同)");
                return result;
            }

            // 根据文件路径查找或创建对应的目录
            Long folderId = findOrCreateFolderForFile(filePath, config);

            // 获取目录的部门ID，确保文件部门与目录一致
            final Long finalDeptId;
            if (folderId != null && folderId > 0) {
                KbFolder folder = folderMapper.selectById(folderId);
                finalDeptId = folder != null ? folder.getDeptId() : null;
            } else {
                finalDeptId = null;
            }

            // 在事务中创建文件和文档记录
            final Long finalFolderId = folderId;
            Long docId = transactionTemplate.execute(status -> {
                return createFileAndDoc(filePath, config, sha256, fileName, fileType, finalFolderId, finalDeptId);
            });
            result.setDocId(docId);
            result.setFolderId(finalFolderId);
            return result;
        }

        /**
         * 根据文件路径查找或创建对应的目录
         *
         * @param filePath 文件路径
         * @param config   监控配置
         * @return 目录ID
         */
        private Long findOrCreateFolderForFile(Path filePath, KbFileWatchConfig config) {
            Path parentDir = filePath.getParent();
            if (parentDir == null) {
                return config.getFolderId() != null ? config.getFolderId() : 0L;
            }

            // 使用 DirectoryHandler 的方法创建或查找目录
            return directoryHandler.createOrUpdateFolder(parentDir, config);
        }

        /**
         * 处理文件修改：更新文件信息和ES索引
         *
         * @param filePath  文件路径
         * @param config    监控配置
         * @return 处理结果
         */
        private ChangeProcessResult handleFileModified(Path filePath, KbFileWatchConfig config) throws IOException {
            ChangeProcessResult result = new ChangeProcessResult();
            String filePathStr = pathHelper.normalizePath(filePath);

            // 通过物理路径查找已有文件记录
            KbFile existFile = findFileByPhysicalPath(filePathStr);
            if (existFile == null) {
                log.info("文件未在文档库中，按新增处理: {}", filePath);
                return handleFileCreated(filePath, config);
            }

            // 计算新hash
            String newSha256;
            try (java.io.InputStream is = Files.newInputStream(filePath)) {
                newSha256 = DigestUtil.sha256Hex(is);
            }

            // 更新文件信息
            existFile.setSha256(newSha256);
            existFile.setFileSize(Files.size(filePath));
            fileMapper.updateById(existFile);

            // 查找关联文档并更新
            KbDoc doc = updateDocForModifiedFile(existFile, config);
            if (doc != null) {
                result.setDocId(doc.getDocId());
            }
            log.info("文件修改处理完成: {}", filePath);
            return result;
        }

        /**
         * 处理文件重命名：更新文件路径和文档名称
         *
         * @param newFilePath  新文件路径
         * @param oldFilePath  旧文件路径
         * @param config       监控配置
         * @return 处理结果
         */
        private ChangeProcessResult handleFileRenamed(Path newFilePath, String oldFilePath, KbFileWatchConfig config) {
            ChangeProcessResult result = new ChangeProcessResult();
            String oldPathStr = pathHelper.normalizePath(Path.of(oldFilePath));
            String newPathStr = pathHelper.normalizePath(newFilePath);
            String newFileName = newFilePath.getFileName().toString();

            log.info("处理文件重命名: {} -> {}", oldFilePath, newPathStr);

            // 通过旧路径查找文件记录
            KbFile existFile = findFileByPhysicalPath(oldPathStr);
            if (existFile == null) {
                log.info("旧文件未在文档库中，忽略重命名事件: {}", oldFilePath);
                result.setMessage("旧文件不在文档库中");
                return result;
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

                // 更新ES索引中的文档名称
                if (isAutoIndexEnabled(config)) {
                    safeUpdateEsIndex(doc.getDocId());
                }
                result.setDocId(doc.getDocId());
            }

            log.info("文件重命名处理完成: {} -> {} (docId={})", oldFilePath, newPathStr, doc != null ? doc.getDocId() : null);
            return result;
        }

        /**
         * 处理文件删除：更新文档状态和ES
         *
         * @param filePath  文件路径
         * @param config    监控配置
         * @return 处理结果
         */
        private ChangeProcessResult handleFileDeleted(Path filePath, KbFileWatchConfig config) {
            ChangeProcessResult result = new ChangeProcessResult();
            // 检查文件是否真的不存在了
            if (Files.exists(filePath)) {
                log.info("文件仍然存在，忽略删除事件: {}", filePath);
                result.setMessage("文件仍然存在");
                return result;
            }

            String filePathStr = pathHelper.normalizePath(filePath);
            KbFile existFile = findFileByPhysicalPath(filePathStr);
            if (existFile == null) {
                log.info("文件未在文档库中，忽略删除事件: {}", filePath);
                result.setMessage("文件不在文档库中");
                return result;
            }

            // 查找关联文档获取docId（用于日志记录）
            KbDoc doc = findDocByFileId(existFile.getFileId());
            if (doc != null) {
                result.setDocId(doc.getDocId());
            }

            // 在事务中删除文件和文档记录
            transactionTemplate.executeWithoutResult(status -> {
                deleteFileAndDoc(existFile);
            });

            log.info("文件删除处理完成: {}", filePath);
            return result;
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
         * @param folderId 目录ID（根据文件路径查找的实际目录）
         * @param deptId   部门ID（与目录部门保持一致）
         * @return 文档ID
         */
        private Long createFileAndDoc(Path filePath, KbFileWatchConfig config, String sha256, String fileName, String fileType, Long folderId, Long deptId) {
            try {
                // 保存文件记录
                KbFile kbFile = buildKbFile(filePath, sha256, fileName, fileType);
                fileMapper.insert(kbFile);

                // 创建文档记录（使用实际的目录ID和部门ID）
                KbDoc doc = buildKbDoc(kbFile, folderId, deptId, fileName, fileType);
                docMapper.insert(doc);

                // 异步解析并索引到ES
                if (isAutoIndexEnabled(config)) {
                    safeAsyncProcessDoc(doc.getDocId());
                }

                log.info("文件自动添加到文档库成功: {} -> docId={}, folderId={}, deptId={}", fileName, doc.getDocId(), folderId, deptId);
                return doc.getDocId();
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
         * @param folderId 目录ID（根据文件路径计算的实际目录）
         * @param deptId   部门ID（与目录部门保持一致）
         */
        private KbDoc buildKbDoc(KbFile kbFile, Long folderId, Long deptId, String fileName, String fileType) {
            KbDoc doc = new KbDoc();
            doc.setFolderId(folderId != null ? folderId : 0L);
            doc.setDepId(deptId); // 部门ID与目录保持一致
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
         * @return 更新后的文档
         */
        private KbDoc updateDocForModifiedFile(KbFile existFile, KbFileWatchConfig config) {
            KbDoc doc = findDocByFileId(existFile.getFileId());
            if (doc == null) {
                return null;
            }

            doc.setFileSize(existFile.getFileSize());
            docMapper.updateById(doc);

            // 重新解析并索引到ES
            if (isAutoIndexEnabled(config)) {
                safeParseAndIndexDoc(doc.getDocId());
            }
            return doc;
        }

        /**
         * 删除文件和文档记录
         */
        private void deleteFileAndDoc(KbFile existFile) {
            KbDoc doc = findDocByFileId(existFile.getFileId());
            if (doc != null) {
                // 从 ES 中删除
                safeDeleteFromEs(doc.getDocId());

                // 删除文档记录
                docService.deleteDocById(doc.getDocId());
            }

            // 删除文件记录
            fileMapper.deleteById(existFile.getFileId());
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
     * 负责处理目录的创建、修改、删除、重命名事件
     *
     * 重要特性：
     * 1. 目录删除时，自动递归删除子目录和子文件（不触发子项事件）
     * 2. 目录重命名时，自动更新子目录和子文件的路径（不触发子项事件）
     * 3. 目录修改时，只更新目录信息
     */
    private class DirectoryHandler {

        /** 目录创建锁（按路径分级，避免并发创建同名目录） */
        private final Map<String, Object> folderCreateLocks = new ConcurrentHashMap<>();

        /**
         * 处理目录变化
         *
         * @param event     变化事件
         * @param config    监控配置
         */
        public ChangeProcessResult handleDirectoryChange(ChangeEvent event, KbFileWatchConfig config) {
            Path dirPath = Path.of(event.path());
            String dirName = pathHelper.getDirName(dirPath);
            ChangeProcessResult result = new ChangeProcessResult();

            switch (event.type()) {
                case CREATED -> result = handleDirectoryCreated(dirPath, dirName, config);
                case MODIFIED -> result = handleDirectoryModified(dirPath, dirName, config);
                case DELETED -> result = handleDirectoryDeleted(dirPath, config);
                case RENAMED -> result = handleDirectoryRenamed(dirPath, event.oldPath(), config);
            }
            return result;
        }

        /**
         * 处理目录创建
         */
        private ChangeProcessResult handleDirectoryCreated(Path dirPath, String dirName, KbFileWatchConfig config) {
            ChangeProcessResult result = new ChangeProcessResult();
            log.info("检测到新目录: {}", dirPath);
            try {
                Long folderId = createOrUpdateFolder(dirPath, config);
                result.setFolderId(folderId);
                log.info("目录自动创建成功: {} -> folderId={}", dirPath, folderId);
            } catch (Exception e) {
                log.error("目录自动创建失败: {}", dirPath, e);
                result.setMessage("目录创建失败: " + e.getMessage());
            }
            return result;
        }

        /**
         * 处理目录重命名
         * 更新目录本身及所有子目录、文件的路径信息
         *
         * 重要：不触发子项事件，直接批量更新路径
         *
         * @param newDirPath  新目录路径
         * @param oldDirPath  旧目录路径
         * @param config      监控配置
         */
        private ChangeProcessResult handleDirectoryRenamed(Path newDirPath, String oldDirPath, KbFileWatchConfig config) {
            ChangeProcessResult result = new ChangeProcessResult();
            String oldPathStr = pathHelper.normalizePath(Path.of(oldDirPath));
            String newPathStr = pathHelper.normalizePath(newDirPath);
            String oldDirName = Path.of(oldPathStr).getFileName().toString();
            String newDirName = pathHelper.getDirName(newDirPath);

            log.info("处理目录重命名: {} -> {}", oldDirPath, newPathStr);

            // 通过旧路径查找目录记录
            Long kbParentFolderId = pathHelper.getKbParentFolderId(config);
            Path oldRelativePath = pathHelper.getRelativePath(Path.of(oldDirPath), config.getWatchPath());
            KbFolder existFolder = findFolderByRelativePath(oldRelativePath, kbParentFolderId);

            if (existFolder == null) {
                log.info("旧目录未在文档库中，忽略重命名事件: {}", oldDirPath);
                result.setMessage("旧目录不在文档库中");
                return result;
            }

            result.setFolderId(existFolder.getFolderId());

            // 计算旧逻辑路径和新逻辑路径
            String oldFolderPath = existFolder.getFolderPath();
            String newFolderPath = oldFolderPath.replace(oldDirName, newDirName);

            // 在事务中批量更新路径信息
            transactionTemplate.executeWithoutResult(status -> {
                // 1. 更新目录本身
                existFolder.setFolderName(newDirName);
                existFolder.setFolderPath(newFolderPath);
                folderMapper.updateById(existFolder);
                log.debug("更新目录: folderId={}, newName={}, newPath={}", existFolder.getFolderId(), newDirName, newFolderPath);

                // 2. 递归更新所有子目录的 folder_path 和 folderName（基于 parentId）
                updateChildFoldersRecursively(existFolder.getFolderId(), oldFolderPath, newFolderPath);

                // 3. 递归更新所有文件的 physical_path（基于 folderId -> docId -> fileId）
                updateChildFilesRecursively(existFolder.getFolderId(), oldPathStr, newPathStr);

                log.info("目录重命名处理完成: {} -> {}, 已更新子目录和文件路径", oldDirPath, newPathStr);
            });
            return result;
        }

        /**
         * 递归更新所有子目录的 folder_path（基于 parentId 关系）
         *
         * @param parentFolderId    父目录ID
         * @param oldFolderPathPrefix 旧逻辑路径前缀
         * @param newFolderPathPrefix 新逻辑路径前缀
         */
        private void updateChildFoldersRecursively(Long parentFolderId, String oldFolderPathPrefix, String newFolderPathPrefix) {
            // 查找直接子目录（通过 parentId）
            List<KbFolder> childFolders = findChildFolders(parentFolderId);

            for (KbFolder child : childFolders) {
                // 更新子目录的逻辑路径
                if (child.getFolderPath() != null) {
                    String newChildPath = child.getFolderPath().replace(oldFolderPathPrefix, newFolderPathPrefix);
                    child.setFolderPath(newChildPath);
                    folderMapper.updateById(child);
                    log.debug("更新子目录路径: folderId={}, newPath={}", child.getFolderId(), newChildPath);
                }

                // 递归处理子目录的子目录
                updateChildFoldersRecursively(child.getFolderId(), oldFolderPathPrefix, newFolderPathPrefix);
            }

            log.info("已更新 {} 个直接子目录", childFolders.size());
        }

        /**
         * 递归更新所有文件的 physical_path（基于 folderId -> docId -> fileId 关系）
         *
         * @param folderId      目录ID
         * @param oldPathPrefix 旧物理路径前缀
         * @param newPathPrefix 新物理路径前缀
         */
        private void updateChildFilesRecursively(Long folderId, String oldPathPrefix, String newPathPrefix) {
            // 1. 更新当前目录下的所有文件
            // 通过 folderId -> KbDoc -> fileId -> KbFile 查询
            LambdaQueryWrapper<KbDoc> docQuery = Wrappers.lambdaQuery();
            docQuery.eq(KbDoc::getFolderId, folderId);
            List<KbDoc> docs = docMapper.selectList(docQuery);

            int updatedCount = 0;
            for (KbDoc doc : docs) {
                if (doc.getFileId() != null) {
                    KbFile file = fileMapper.selectById(doc.getFileId());
                    if (file != null && file.getPhysicalPath() != null) {
                        // 替换物理路径前缀
                        String newPhysicalPath = file.getPhysicalPath()
                            .replace(oldPathPrefix + "\\", newPathPrefix + "\\")
                            .replace(oldPathPrefix + "/", newPathPrefix + "/");
                        file.setPhysicalPath(newPhysicalPath);
                        fileMapper.updateById(file);
                        updatedCount++;
                        log.debug("更新文件路径: fileId={}, docId={}, newPath={}", file.getFileId(), doc.getDocId(), newPhysicalPath);
                    }
                }
            }

            // 2. 递归处理子目录下的文件
            List<KbFolder> childFolders = findChildFolders(folderId);
            for (KbFolder child : childFolders) {
                updateChildFilesRecursively(child.getFolderId(), oldPathPrefix, newPathPrefix);
            }

            log.info("目录 folderId={} 下已更新 {} 个文件的物理路径", folderId, updatedCount);
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
        private ChangeProcessResult handleDirectoryModified(Path dirPath, String dirName, KbFileWatchConfig config) {
            ChangeProcessResult result = new ChangeProcessResult();
            log.info("检测到目录修改（子文件变化）: {}", dirPath);
            try {
                Long folderId = createOrUpdateFolder(dirPath, config);
                result.setFolderId(folderId);
                log.info("目录更新成功: folderId={}", folderId);
            } catch (Exception e) {
                log.error("目录更新失败: {}", dirPath, e);
                result.setMessage("目录更新失败: " + e.getMessage());
            }
            return result;
        }

        /**
         * 处理目录删除
         * 重要：自动递归删除子目录和子文件（不触发子项事件）
         *
         * @param dirPath 目录路径
         * @param config  监控配置
         */
        private ChangeProcessResult handleDirectoryDeleted(Path dirPath, KbFileWatchConfig config) {
            ChangeProcessResult result = new ChangeProcessResult();
            log.info("检测到目录删除: {}", dirPath);
            try {
                Long kbParentFolderId = pathHelper.getKbParentFolderId(config);
                Path relativePath = pathHelper.getRelativePath(dirPath, config.getWatchPath());

                KbFolder existFolder = findFolderByRelativePath(relativePath, kbParentFolderId);
                if (existFolder != null) {
                    result.setFolderId(existFolder.getFolderId());
                    // 在事务中递归删除目录及其子目录、文档
                    transactionTemplate.executeWithoutResult(status -> {
                        deleteFolderRecursively(existFolder.getFolderId());
                    });
                    log.info("目录及子目录已删除: folderId={}, path={}", existFolder.getFolderId(), dirPath);
                } else {
                    log.warn("未找到对应的目录记录: {}", dirPath);
                }
            } catch (Exception e) {
                log.error("目录删除失败: {}", dirPath, e);
                result.setMessage("目录删除失败: " + e.getMessage());
            }
            return result;
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
        public Long createOrUpdateFolder(Path dirPath, KbFileWatchConfig config) {
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