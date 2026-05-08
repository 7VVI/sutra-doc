package com.hmoob.doc.watch;

import cn.hutool.core.util.StrUtil;
import com.hmoob.doc.enums.FileChangeTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/**
 * 快照引擎（纯 Java 实现，不依赖 WatchService）
 * 采用定时快照对比方案，支持 10w+ 文件高效扫描
 *
 * 核心特性：
 * 1. Fork/Join 并发扫描
 * 2. 多层检测策略：lastModified+size 快速预判 → CRC32 快速校验 → SHA256 精确校验
 * 3. 重命名检测：DELETED + CREATED 组合，SHA256 相同
 * 4. 目录事件优化：只触发目录本身变化，子项不单独触发
 *
 * @author hmoob
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotEngine {

    private final HashEngine hashEngine;

    /** 并发扫描线程池 */
    private final ForkJoinPool scanPool = ForkJoinPool.commonPool();

    /** 并发阈值：子文件数超过此值则并行处理 */
    private static final int PARALLEL_THRESHOLD = 500;

    /**
     * 对指定目录做全量快照（并发扫描）
     *
     * @param root           根目录
     * @param recursive      是否递归
     * @param excludePattern 排除模式（逗号分隔）
     * @return 快照对象
     */
    public FileSnapshot takeSnapshot(Path root, boolean recursive, String excludePattern) throws IOException {
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            log.warn("快照根目录不存在或不是目录: {}", root);
            return FileSnapshot.empty();
        }

        // 重要：每次快照扫描前清除hash缓存，确保文件内容变更能正确检测
        hashEngine.clearCache();

        Map<String, FileNode> index = new ConcurrentHashMap<>(150000);
        Set<String> excludePatterns = parsePatterns(excludePattern);

        long start = System.currentTimeMillis();

        // Fork/Join 并发扫描
        if (recursive) {
            scanPool.invoke(new ScanTask(root, index, excludePatterns));
        } else {
            // 单层扫描
            scanSingleLevel(root, index, excludePatterns);
        }

        // 自底向上计算目录hash
        computeDirectoryHashes(index, root);

        long elapsed = System.currentTimeMillis() - start;
        log.info("快照扫描完成: 路径={}, 文件数={}, 耗时={}ms", root, index.size(), elapsed);

        return new FileSnapshot(index, System.currentTimeMillis());
    }

    /**
     * 对比两个快照，输出变化列表
     * 核心逻辑：
     * 1. 检测新增、删除、修改、重命名
     * 2. 目录变化时，子项不单独触发事件：
     *    - 目录新增：子项不触发新增事件
     *    - 目录删除：子项不触发删除事件
     *    - 目录重命名：子项不触发重命名事件
     *    - 目录修改：子项不触发修改事件
     *
     * 检测策略：
     * - 文件变化检测：先用 lastModified+size 快速预判，有变化再用 SHA256 精确确认
     * - 重命名检测：使用 SHA256 精确指纹
     *
     * @param oldSnap 旧快照
     * @param newSnap 新快照
     * @return 变化事件列表（按顺序排列）
     */
    public List<ChangeEvent> diff(FileSnapshot oldSnap, FileSnapshot newSnap) {
        List<ChangeEvent> changes = new ArrayList<>();
        Map<String, FileNode> oldIndex = oldSnap.getIndex();
        Map<String, FileNode> newIndex = newSnap.getIndex();

        Set<String> oldPaths = oldIndex.keySet();
        Set<String> newPaths = newIndex.keySet();

        // 1. 收集新增路径
        List<String> addedPaths = newPaths.stream()
            .filter(p -> !oldPaths.contains(p))
            .toList();

        // 2. 收集删除路径
        List<String> deletedPaths = oldPaths.stream()
            .filter(p -> !newPaths.contains(p))
            .toList();

        // 3. 收集修改路径（只检测文件，目录不触发 MODIFIED）
        // 目录的变化通过 CREATED/DELETED/RENAMED 事件体现，子文件变化不需要触发目录 MODIFIED
        List<String> modifiedPaths = newPaths.stream()
            .filter(oldPaths::contains)
            .filter(p -> !newIndex.get(p).isDirectory()) // 只检测文件修改
            .filter(p -> {
                FileNode oldNode = oldIndex.get(p);
                FileNode newNode = newIndex.get(p);

                // 文件：多层检测策略
                // 第1层：lastModified 或 size 变化，快速预判
                if (oldNode.lastModified() != newNode.lastModified() || oldNode.size() != newNode.size()) {
                    // 第2层：SHA256 精确确认（内容是否真的变了）
                    return !Objects.equals(oldNode.sha256Hash(), newNode.sha256Hash());
                }

                // lastModified 和 size 都没变，再用 SHA256 精确判断（防止极端 edge case）
                return !Objects.equals(oldNode.sha256Hash(), newNode.sha256Hash());
            })
            .toList();

        log.info("快照对比: 新增={}, 删除={}, 修改={}", addedPaths.size(), deletedPaths.size(), modifiedPaths.size());

        // 4. 检测重命名（分两步：先检测目录重命名，再检测文件重命名）
        // 4.1 先检测文件重命名（基于 SHA256）
        List<RenamePair> fileRenamePairs = detectFileRenames(addedPaths, deletedPaths, oldIndex, newIndex);
        Set<String> fileRenamedOldPaths = fileRenamePairs.stream().map(r -> r.oldPath).collect(Collectors.toSet());
        Set<String> fileRenamedNewPaths = fileRenamePairs.stream().map(r -> r.newPath).collect(Collectors.toSet());

        // 4.2 根据文件重命名推断目录重命名
        List<RenamePair> dirRenamePairs = inferDirectoryRenames(addedPaths, deletedPaths, fileRenamePairs, oldIndex, newIndex);
        Set<String> dirRenamedOldPaths = dirRenamePairs.stream().map(r -> r.oldPath).collect(Collectors.toSet());
        Set<String> dirRenamedNewPaths = dirRenamePairs.stream().map(r -> r.newPath).collect(Collectors.toSet());

        // 合并所有重命名
        List<RenamePair> renamePairs = new ArrayList<>();
        renamePairs.addAll(dirRenamePairs); // 目录重命名优先
        renamePairs.addAll(fileRenamePairs);
        Set<String> renamedOldPaths = renamePairs.stream().map(r -> r.oldPath).collect(Collectors.toSet());
        Set<String> renamedNewPaths = renamePairs.stream().map(r -> r.newPath).collect(Collectors.toSet());

        // 5. 收集顶级事件（用于过滤子项事件）
        // 顶级新增目录：这些目录的子项不应触发新增事件
        Set<String> topLevelCreatedDirs = addedPaths.stream()
            .filter(p -> !renamedNewPaths.contains(p))
            .filter(p -> newIndex.get(p).isDirectory())
            .filter(p -> !isChildOfAnyPath(p, addedPaths, newIndex))
            .collect(Collectors.toSet());

        // 顶级删除目录：这些目录的子项不应触发删除事件
        Set<String> topLevelDeletedDirs = deletedPaths.stream()
            .filter(p -> !renamedOldPaths.contains(p))
            .filter(p -> oldIndex.get(p).isDirectory())
            .filter(p -> !isChildOfAnyPath(p, deletedPaths, oldIndex))
            .collect(Collectors.toSet());

        // 顶级重命名目录：这些目录的子项不应触发重命名事件
        Set<String> topLevelRenamedOldDirs = dirRenamedOldPaths;

        // 6. 添加重命名事件（只添加目录重命名，文件重命名如果父目录也被重命名则跳过）
        for (RenamePair pair : dirRenamePairs) {
            FileNode newNode = newIndex.get(pair.newPath);
            changes.add(new ChangeEvent(
                FileChangeTypeEnum.RENAMED,
                pair.newPath,
                true,
                pair.oldPath,
                newNode.size(),
                newNode.sha256Hash(),
                null
            ));
            log.debug("检测到目录重命名: {} -> {}", pair.oldPath, pair.newPath);
        }

        // 文件重命名：如果父目录也被重命名，则跳过（路径变化由目录重命名处理）
        for (RenamePair pair : fileRenamePairs) {
            // 如果是目录重命名的子项，跳过
            if (isChildOfRenamedDir(pair.oldPath, topLevelRenamedOldDirs)) {
                continue;
            }
            FileNode newNode = newIndex.get(pair.newPath);
            changes.add(new ChangeEvent(
                FileChangeTypeEnum.RENAMED,
                pair.newPath,
                false,
                pair.oldPath,
                newNode.size(),
                newNode.sha256Hash(),
                null
            ));
            log.debug("检测到文件重命名: {} -> {}", pair.oldPath, pair.newPath);
        }

        // 7. 处理新增事件（排除已识别为重命名的，以及顶级目录的子项，以及目录重命名的子项）
        for (String path : addedPaths) {
            if (renamedNewPaths.contains(path)) {
                continue;
            }
            // 如果是顶级新增目录的子项，跳过
            if (isChildOfAnyCreatedDir(path, topLevelCreatedDirs)) {
                continue;
            }
            // 如果是目录重命名的子项，跳过
            if (isChildOfRenamedDir(path, dirRenamedNewPaths)) {
                continue;
            }
            FileNode node = newIndex.get(path);
            changes.add(new ChangeEvent(
                FileChangeTypeEnum.CREATED,
                path,
                node.isDirectory(),
                null,
                node.size(),
                node.sha256Hash(),
                null
            ));
            log.debug("检测到新增: {}", path);
        }

        // 8. 处理删除事件（排除已识别为重命名的，以及顶级目录的子项，以及目录重命名的子项）
        for (String path : deletedPaths) {
            if (renamedOldPaths.contains(path)) {
                continue;
            }
            // 如果是目录重命名的子项，跳过
            if (isChildOfRenamedDir(path, dirRenamedOldPaths)) {
                continue;
            }
            FileNode node = oldIndex.get(path);
            // 目录：只添加顶级删除目录
            if (node.isDirectory()) {
                if (topLevelDeletedDirs.contains(path)) {
                    changes.add(new ChangeEvent(FileChangeTypeEnum.DELETED, path, true));
                    log.debug("检测到目录删除: {}", path);
                }
            } else {
                // 文件：如果不是顶级删除目录的子项，则添加删除事件
                if (!isChildOfAnyDeletedDir(path, topLevelDeletedDirs)) {
                    changes.add(new ChangeEvent(
                        FileChangeTypeEnum.DELETED,
                        path,
                        false,
                        null,
                        node.size(),
                        node.sha256Hash(),
                        null
                    ));
                    log.debug("检测到文件删除: {}", path);
                }
            }
        }

        // 9. 处理修改事件（只有文件修改，目录不触发 MODIFIED）
        for (String path : modifiedPaths) {
            FileNode newNode = newIndex.get(path);
            FileNode oldNode = oldIndex.get(path);
            changes.add(new ChangeEvent(
                FileChangeTypeEnum.MODIFIED,
                path,
                false,
                null,
                newNode.size(),
                newNode.sha256Hash(),
                oldNode.sha256Hash()
            ));
            log.debug("检测到文件修改: {} (oldHash={}, newHash={})", path, oldNode.sha256Hash(), newNode.sha256Hash());
        }

        // 10. 按处理顺序排序事件
        sortEventsByOrder(changes);

        if (!changes.isEmpty()) {
            log.info("快照对比发现 {} 个变化事件", changes.size());
        }

        return changes;
    }

    /**
     * 检测文件重命名（基于 SHA256）
     * 只检测文件，目录的 SHA256 为空无法直接匹配
     */
    private List<RenamePair> detectFileRenames(List<String> addedPaths, List<String> deletedPaths,
                                            Map<String, FileNode> oldIndex, Map<String, FileNode> newIndex) {
        List<RenamePair> pairs = new ArrayList<>();
        Set<String> matchedDeleted = new HashSet<>();
        Set<String> matchedAdded = new HashSet<>();

        // 只检测文件重命名（排除目录）
        List<String> addedFiles = addedPaths.stream()
            .filter(p -> !newIndex.get(p).isDirectory())
            .toList();
        List<String> deletedFiles = deletedPaths.stream()
            .filter(p -> !oldIndex.get(p).isDirectory())
            .toList();

        for (String addedPath : addedFiles) {
            FileNode addedNode = newIndex.get(addedPath);

            for (String deletedPath : deletedFiles) {
                if (matchedDeleted.contains(deletedPath)) {
                    continue;
                }
                FileNode deletedNode = oldIndex.get(deletedPath);

                // SHA256 相同认为是重命名
                if (Objects.equals(addedNode.sha256Hash(), deletedNode.sha256Hash())) {
                    pairs.add(new RenamePair(deletedPath, addedPath));
                    matchedDeleted.add(deletedPath);
                    matchedAdded.add(addedPath);
                    break;
                }
            }
        }

        return pairs;
    }

    /**
     * 根据文件重命名推断目录重命名
     * 规则：如果一组文件的重命名只是父目录名变了，则推断父目录被重命名
     */
    private List<RenamePair> inferDirectoryRenames(List<String> addedPaths, List<String> deletedPaths,
                                                   List<RenamePair> fileRenamePairs,
                                                   Map<String, FileNode> oldIndex, Map<String, FileNode> newIndex) {
        List<RenamePair> dirPairs = new ArrayList<>();

        // 收集新增和删除的目录
        List<String> addedDirs = addedPaths.stream()
            .filter(p -> newIndex.get(p).isDirectory())
            .toList();
        List<String> deletedDirs = deletedPaths.stream()
            .filter(p -> oldIndex.get(p).isDirectory())
            .toList();

        // 根据文件重命名推断父目录重命名
        // 如果文件的旧路径和新路径只是父目录名不同，则父目录被重命名
        Map<String, String> potentialDirRenames = new HashMap<>();

        for (RenamePair filePair : fileRenamePairs) {
            Path oldFilePath = Path.of(filePair.oldPath);
            Path newFilePath = Path.of(filePair.newPath);

            // 文件名必须相同（否则是文件重命名，不是目录重命名）
            if (!oldFilePath.getFileName().toString().equals(newFilePath.getFileName().toString())) {
                continue;
            }

            // 检查父目录路径是否只是目录名变了
            Path oldParent = oldFilePath.getParent();
            Path newParent = newFilePath.getParent();

            if (oldParent != null && newParent != null) {
                String oldParentPath = oldParent.toAbsolutePath().normalize().toString();
                String newParentPath = newParent.toAbsolutePath().normalize().toString();

                // 检查是否是直接父目录重命名（父目录名不同，但更上级路径相同）
                Path oldGrandParent = oldParent.getParent();
                Path newGrandParent = newParent.getParent();

                if (oldGrandParent != null && newGrandParent != null &&
                    oldGrandParent.toAbsolutePath().normalize().toString()
                        .equals(newGrandParent.toAbsolutePath().normalize().toString())) {
                    potentialDirRenames.put(oldParentPath, newParentPath);
                }
            }
        }

        // 确认目录重命名：检查新增和删除的目录是否匹配推断
        for (Map.Entry<String, String> entry : potentialDirRenames.entrySet()) {
            String oldDirPath = entry.getKey();
            String newDirPath = entry.getValue();

            if (deletedDirs.contains(oldDirPath) && addedDirs.contains(newDirPath)) {
                dirPairs.add(new RenamePair(oldDirPath, newDirPath));
                log.debug("推断目录重命名: {} -> {}", oldDirPath, newDirPath);
            }
        }

        return dirPairs;
    }

    /**
     * 检查路径是否是任意路径列表的子项
     *
     * @param path      待检查的路径
     * @param parentPaths 父路径列表
     * @param index     快照索引（用于判断是否为目录）
     * @return 如果是子项则返回 true
     */
    private boolean isChildOfAnyPath(String path, List<String> parentPaths, Map<String, FileNode> index) {
        for (String parentPath : parentPaths) {
            FileNode parentNode = index.get(parentPath);
            if (parentNode != null && parentNode.isDirectory()) {
                if (isChildPath(path, parentPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查路径是否是任意新增目录的子项
     *
     * @param path            待检查的路径
     * @param createdDirPaths 新增目录路径集合
     * @return 如果是子项则返回 true
     */
    private boolean isChildOfAnyCreatedDir(String path, Set<String> createdDirPaths) {
        for (String dirPath : createdDirPaths) {
            if (isChildPath(path, dirPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查路径是否是任意删除目录的子项
     *
     * @param path            待检查的路径
     * @param deletedDirPaths 删除目录路径集合
     * @return 如果是子项则返回 true
     */
    private boolean isChildOfAnyDeletedDir(String path, Set<String> deletedDirPaths) {
        for (String dirPath : deletedDirPaths) {
            if (isChildPath(path, dirPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查路径是否是任意重命名目录的子项
     *
     * @param path              待检查的路径（旧路径）
     * @param renamedOldDirPaths 重命名目录的旧路径集合
     * @return 如果是子项则返回 true
     */
    private boolean isChildOfRenamedDir(String path, Set<String> renamedOldDirPaths) {
        for (String dirPath : renamedOldDirPaths) {
            if (isChildPath(path, dirPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断路径是否是另一路径的子项
     * 支持 Windows 和 Linux 路径分隔符
     *
     * @param childPath  子路径
     * @param parentPath 父路径
     * @return 如果是子项则返回 true
     */
    private boolean isChildPath(String childPath, String parentPath) {
        return childPath.startsWith(parentPath + "\\") || childPath.startsWith(parentPath + "/");
    }

    private void sortEventsByOrder(List<ChangeEvent> events) {
        events.sort((e1, e2) -> {
            int typeOrder1 = getTypeOrder(e1.type());
            int typeOrder2 = getTypeOrder(e2.type());

            if (typeOrder1 != typeOrder2) {
                return typeOrder1 - typeOrder2;
            }

            int depth1 = countPathDepth(e1.path());
            int depth2 = countPathDepth(e2.path());

            if (e1.type() == FileChangeTypeEnum.DELETED || e1.type() == FileChangeTypeEnum.CREATED) {
                return Integer.compare(depth1, depth2);
            } else {
                return Integer.compare(depth2, depth1);
            }
        });
    }

    private int getTypeOrder(FileChangeTypeEnum type) {
        switch (type) {
            case DELETED: return 1;
            case RENAMED: return 2;
            case CREATED: return 3;
            case MODIFIED: return 4;
            default: return 5;
        }
    }

    private int countPathDepth(String path) {
        return path.split("[\\\\/]").length;
    }

    /**
     * Fork/Join 并发扫描任务
     */
    private class ScanTask extends RecursiveAction {
        private final Path dir;
        private final Map<String, FileNode> index;
        private final Set<String> excludePatterns;

        ScanTask(Path dir, Map<String, FileNode> index, Set<String> excludePatterns) {
            this.dir = dir;
            this.index = index;
            this.excludePatterns = excludePatterns;
        }

        @Override
        protected void compute() {
            try {
                doScan();
            } catch (IOException e) {
                log.warn("扫描目录失败: {}", dir, e);
            }
        }

        private void doScan() throws IOException {
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                return;
            }

            BasicFileAttributes dirAttrs = Files.readAttributes(dir, BasicFileAttributes.class);
            FileNode dirNode = new FileNode(
                dir.toAbsolutePath().normalize().toString(),
                true,
                0,
                dirAttrs.lastModifiedTime().toMillis(),
                "",
                ""
            );
            index.put(dirNode.path(), dirNode);

            // 使用 try-with-resources 关闭 DirectoryStream，避免锁定目录
            List<Path> children;
            try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
                children = stream.toList();
            }

            List<ScanTask> subTasks = new ArrayList<>();
            List<Path> files = new ArrayList<>();

            for (Path child : children) {
                if (shouldIgnore(child, excludePatterns)) {
                    continue;
                }

                if (Files.isDirectory(child)) {
                    ScanTask task = new ScanTask(child, index, excludePatterns);
                    if (children.size() > PARALLEL_THRESHOLD) {
                        task.fork();
                        subTasks.add(task);
                    } else {
                        task.compute();
                    }
                } else {
                    files.add(child);
                }
            }

            for (Path file : files) {
                try {
                    FileNode node = buildFileNode(file);
                    if (node != null) {
                        index.put(node.path(), node);
                    }
                } catch (IOException e) {
                    log.warn("构建文件节点失败: {}", file, e);
                }
            }

            for (ScanTask task : subTasks) {
                task.join();
            }
        }
    }

    private void scanSingleLevel(Path dir, Map<String, FileNode> index, Set<String> excludePatterns) throws IOException {
        BasicFileAttributes dirAttrs = Files.readAttributes(dir, BasicFileAttributes.class);
        FileNode dirNode = new FileNode(
            dir.toAbsolutePath().normalize().toString(),
            true,
            0,
            dirAttrs.lastModifiedTime().toMillis(),
            "",
            ""
        );
        index.put(dirNode.path(), dirNode);

        // 使用 try-with-resources 关闭 DirectoryStream，避免锁定目录
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> !shouldIgnore(p, excludePatterns))
                .forEach(p -> {
                    try {
                        FileNode node = buildFileNode(p);
                        if (node != null) {
                            index.put(node.path(), node);
                        }
                    } catch (IOException e) {
                        log.warn("构建文件节点失败: {}", p, e);
                    }
                });
        }
    }

    /**
     * 构建文件节点
     * 使用多层检测策略：
     * 1. 快速预判：lastModified + size
     * 2. 精确校验：SHA256（用于变化检测和重命名检测）
     */
    private FileNode buildFileNode(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        boolean isDir = attrs.isDirectory();
        long size = isDir ? 0 : attrs.size();
        long lastModified = attrs.lastModifiedTime().toMillis();

        String contentHash;
        String sha256Hash;

        if (isDir) {
            contentHash = "";
            sha256Hash = "";
        } else {
            // 文件：使用 SHA256 作为精确指纹（用于变化检测和重命名检测）
            sha256Hash = hashEngine.calcSHA256(path);
            contentHash = sha256Hash; // 两者一致，简化逻辑
        }

        return new FileNode(
            path.toAbsolutePath().normalize().toString(),
            isDir,
            size,
            lastModified,
            contentHash,
            sha256Hash
        );
    }

    private void computeDirectoryHashes(Map<String, FileNode> index, Path root) {
        List<Map.Entry<String, FileNode>> dirEntries = index.entrySet().stream()
            .filter(e -> e.getValue().isDirectory())
            .sorted((a, b) -> b.getKey().length() - a.getKey().length())
            .toList();

        for (Map.Entry<String, FileNode> entry : dirEntries) {
            Path dirPath = Path.of(entry.getKey());
            String dirHash = hashEngine.hashDirectory(dirPath, index);
            FileNode oldNode = entry.getValue();
            index.put(entry.getKey(), new FileNode(
                oldNode.path(),
                true,
                oldNode.size(),
                oldNode.lastModified(),
                dirHash,
                dirHash
            ));
        }
    }

    private boolean shouldIgnore(Path path, Set<String> excludePatterns) {
        String fileName = path.getFileName().toString();
        for (String pattern : excludePatterns) {
            if (matchGlob(fileName, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchGlob(String fileName, String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return false;
        }
        if (pattern.startsWith("*")) {
            return fileName.endsWith(pattern.substring(1));
        } else if (pattern.endsWith("*")) {
            return fileName.startsWith(pattern.substring(0, pattern.length() - 1));
        } else {
            return fileName.equals(pattern);
        }
    }

    private Set<String> parsePatterns(String patternStr) {
        if (StrUtil.isBlank(patternStr)) {
            return Collections.emptySet();
        }
        return Arrays.stream(patternStr.split(","))
            .map(String::trim)
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.toSet());
    }

    private record RenamePair(String oldPath, String newPath) {}
}