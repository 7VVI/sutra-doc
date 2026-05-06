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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 快照引擎（类比Git index）
 * 负责文件系统扫描和快照对比
 *
 * @author hmoob
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotEngine {

    private final HashEngine hashEngine;

    /**
     * 对指定目录做全量快照
     *
     * @param root           根目录
     * @param recursive      是否递归
     * @param excludePattern 排除模式（逗号分隔）
     * @return 快照对象
     */
    public FileSnapshot takeSnapshot(Path root, boolean recursive, String excludePattern) throws IOException {
        Map<String, FileNode> index = new ConcurrentHashMap<>();
        Set<String> excludePatterns = parsePatterns(excludePattern);

        int maxDepth = recursive ? Integer.MAX_VALUE : 1;

        try (Stream<Path> stream = Files.walk(root, maxDepth)) {
            stream.filter(p -> !p.equals(root))
                .filter(p -> !shouldIgnore(p, excludePatterns))
                .forEach(p -> {
                    try {
                        FileNode node = buildNode(p);
                        index.put(p.toAbsolutePath().normalize().toString(), node);
                    } catch (IOException e) {
                        log.warn("跳过无法读取的路径: {}", p);
                    }
                });
        }

        // 自底向上计算目录hash
        computeDirectoryHashes(index, root);

        return new FileSnapshot(index, System.currentTimeMillis());
    }

    /**
     * 对比两个快照，输出变化列表
     */
    public List<ChangeEvent> diff(FileSnapshot oldSnap, FileSnapshot newSnap) {
        List<ChangeEvent> changes = new ArrayList<>();
        Set<String> oldPaths = oldSnap.getIndex().keySet();
        Set<String> newPaths = newSnap.getIndex().keySet();

        // 新增：在new中但不在old中
        for (String path : newPaths) {
            if (!oldPaths.contains(path)) {
                FileNode node = newSnap.get(path);
                changes.add(new ChangeEvent(FileChangeTypeEnum.CREATED, path, node.isDirectory()));
            }
        }

        // 删除：在old中但不在new中
        for (String path : oldPaths) {
            if (!newPaths.contains(path)) {
                FileNode node = oldSnap.get(path);
                changes.add(new ChangeEvent(FileChangeTypeEnum.DELETED, path, node.isDirectory()));
            }
        }

        // 修改：hash不同
        for (String path : newPaths) {
            if (oldPaths.contains(path)) {
                FileNode oldNode = oldSnap.get(path);
                FileNode newNode = newSnap.get(path);
                if (!Objects.equals(oldNode.contentHash(), newNode.contentHash())) {
                    changes.add(new ChangeEvent(FileChangeTypeEnum.MODIFIED, path, newNode.isDirectory()));
                }
            }
        }

        return changes;
    }

    /**
     * 构建文件节点
     */
    private FileNode buildNode(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        boolean isDir = attrs.isDirectory();
        long size = isDir ? 0 : attrs.size();
        long lastModified = attrs.lastModifiedTime().toMillis();

        String hash;
        if (isDir) {
            // 目录hash在后面统一计算
            hash = "";
        } else {
            hash = hashEngine.hashFile(path);
        }

        return new FileNode(
            path.toAbsolutePath().normalize().toString(),
            isDir,
            size,
            lastModified,
            hash
        );
    }

    /**
     * 自底向上计算目录hash
     */
    private void computeDirectoryHashes(Map<String, FileNode> index, Path root) {
        // 找出所有目录节点，按路径长度倒序排列（先子后父）
        List<Map.Entry<String, FileNode>> dirEntries = index.entrySet().stream()
            .filter(e -> e.getValue().isDirectory())
            .sorted((a, b) -> b.getKey().length() - a.getKey().length())
            .toList();

        for (Map.Entry<String, FileNode> entry : dirEntries) {
            Path dirPath = Path.of(entry.getKey());
            String dirHash = hashEngine.hashDirectory(dirPath, index);
            FileNode oldNode = entry.getValue();
            // 用计算出的hash替换空hash
            index.put(entry.getKey(), new FileNode(
                oldNode.path(),
                true,
                oldNode.size(),
                oldNode.lastModified(),
                dirHash
            ));
        }
    }

    /**
     * 判断路径是否应该忽略
     */
    private boolean shouldIgnore(Path path, Set<String> excludePatterns) {
        String fileName = path.getFileName().toString();
        for (String pattern : excludePatterns) {
            if (matchGlob(fileName, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简单的glob匹配
     */
    private boolean matchGlob(String fileName, String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return false;
        }
        // 支持 *.ext 和 前缀* 两种模式
        if (pattern.startsWith("*")) {
            return fileName.endsWith(pattern.substring(1));
        } else if (pattern.endsWith("*")) {
            return fileName.startsWith(pattern.substring(0, pattern.length() - 1));
        } else {
            return fileName.equals(pattern);
        }
    }

    /**
     * 解析排除模式字符串
     */
    private Set<String> parsePatterns(String patternStr) {
        if (StrUtil.isBlank(patternStr)) {
            return Collections.emptySet();
        }
        return Arrays.stream(patternStr.split(","))
            .map(String::trim)
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.toSet());
    }
}
