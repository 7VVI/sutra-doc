package com.hmoob.doc.watch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hash计算引擎
 * 借鉴Git的设计，用size+mtime做快速预检，避免不必要的IO
 *
 * @author hmoob
 */
@Slf4j
@Component
public class HashEngine {

    /**
     * stat缓存：path -> StatCache(cacheKey, hash)
     */
    private final Map<String, StatCache> statCache = new ConcurrentHashMap<>();

    private record StatCache(String key, String hash) {}

    /**
     * 计算文件Hash（带stat缓存优化）
     * 快速路径：size和mtime都没变，认为文件未变化（类似Git index的stat缓存）
     *
     * @param path 文件路径
     * @return SHA256 hash
     */
    public String hashFile(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

        // 快速路径：size 和 mtime 都没变，直接返回缓存的hash
        String cacheKey = attrs.size() + "_" + attrs.lastModifiedTime().toMillis();
        StatCache cached = statCache.get(path.toString());
        if (cached != null && cached.key().equals(cacheKey)) {
            return cached.hash();
        }

        // 真正读取内容计算hash
        String hash = computeSHA256(path);
        statCache.put(path.toString(), new StatCache(cacheKey, hash));
        return hash;
    }

    /**
     * 计算目录Hash：只对直接子节点排序后做hash（不递归）
     * 子文件变化会冒泡到父目录
     */
    public String hashDirectory(Path dir, Map<String, FileNode> childNodes) {
        String childrenSignature = childNodes.entrySet().stream()
            .filter(e -> isDirectChild(dir, e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + ":" + e.getValue().contentHash())
            .collect(Collectors.joining("\n"));
        return sha256String(childrenSignature);
    }

    /**
     * 计算字符串SHA256
     */
    public String sha256String(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 计算文件内容SHA256
     */
    private String computeSHA256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) {
                    digest.update(buf, 0, n);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 判断路径是否是目录的直接子节点
     */
    private boolean isDirectChild(Path dir, String childPath) {
        Path child = Path.of(childPath);
        Path parent = child.getParent();
        return parent != null && parent.equals(dir);
    }

    /**
     * 清除指定路径的stat缓存
     */
    public void invalidateCache(String path) {
        statCache.remove(path);
    }

    /**
     * 清除所有stat缓存
     */
    public void clearCache() {
        statCache.clear();
    }
}
