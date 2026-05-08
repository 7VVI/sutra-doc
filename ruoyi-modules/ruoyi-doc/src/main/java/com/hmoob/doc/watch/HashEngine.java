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
import java.util.zip.CRC32;

/**
 * Hash计算引擎
 * 采用多层检测策略，平衡速度和精度：
 * 1. lastModified + size 快速预判（无需读取文件内容）
 * 2. CRC32 快速校验（适合大文件场景）
 * 3. SHA256 精确校验（用于去重、重命名检测）
 *
 * @author hmoob
 */
@Slf4j
@Component
public class HashEngine {

    /**
     * stat缓存：绝对路径 -> StatCache
     * 缓存 lastModified + size + crc32 + sha256
     */
    private final Map<String, FileFingerprintCache> fingerprintCache = new ConcurrentHashMap<>();

    /**
     * 文件指纹缓存记录
     */
    private record FileFingerprintCache(
        long size,
        long lastModified,
        long crc32,      // CRC32 快速校验
        String sha256    // SHA256 精确校验
    ) {}

    /**
     * 计算文件 CRC32（快速校验）
     * 适合大文件，计算速度比 SHA256 快很多
     *
     * @param path 文件路径
     * @return CRC32 值
     */
    public long calcCRC32(Path path) throws IOException {
        CRC32 crc = new CRC32();
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buf = new byte[65536]; // 64KB 缓冲区
            int n;
            while ((n = is.read(buf)) != -1) {
                crc.update(buf, 0, n);
            }
        }
        return crc.getValue();
    }

    /**
     * 计算文件 SHA256（精确校验）
     *
     * @param path 文件路径
     * @return SHA256 hash（十六进制字符串）
     */
    public String calcSHA256(Path path) throws IOException {
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
     * 获取文件快速指纹（用于变化检测）
     * 策略：lastModified + size + CRC32
     *
     * @param path 文件路径（绝对路径）
     * @return 快速指纹字符串
     */
    public String getQuickFingerprint(Path path) throws IOException {
        String absolutePath = path.toAbsolutePath().normalize().toString();
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        long size = attrs.size();
        long lastModified = attrs.lastModifiedTime().toMillis();

        // 检查缓存是否可用（size 和 lastModified 未变）
        FileFingerprintCache cached = fingerprintCache.get(absolutePath);
        if (cached != null && cached.size() == size && cached.lastModified() == lastModified) {
            // 使用缓存的 CRC32
            return size + "_" + lastModified + "_" + cached.crc32();
        }

        // 计算新的 CRC32
        long crc32 = calcCRC32(path);

        // 更新缓存（暂不计算 SHA256）
        fingerprintCache.put(absolutePath, new FileFingerprintCache(size, lastModified, crc32, null));

        return size + "_" + lastModified + "_" + crc32;
    }

    /**
     * 获取文件精确指纹（用于去重、重命名检测）
     * 策略：SHA256
     *
     * @param path 文件路径（绝对路径）
     * @return SHA256 hash
     */
    public String getExactFingerprint(Path path) throws IOException {
        String absolutePath = path.toAbsolutePath().normalize().toString();
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        long size = attrs.size();
        long lastModified = attrs.lastModifiedTime().toMillis();

        // 检查缓存是否有 SHA256
        FileFingerprintCache cached = fingerprintCache.get(absolutePath);
        if (cached != null && cached.size() == size && cached.lastModified() == lastModified && cached.sha256() != null) {
            return cached.sha256();
        }

        // 计算新的 SHA256
        String sha256 = calcSHA256(path);

        // 更新缓存
        if (cached != null) {
            fingerprintCache.put(absolutePath, new FileFingerprintCache(size, lastModified, cached.crc32(), sha256));
        } else {
            long crc32 = calcCRC32(path);
            fingerprintCache.put(absolutePath, new FileFingerprintCache(size, lastModified, crc32, sha256));
        }

        return sha256;
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
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hashBytes);
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
        if (parent == null) {
            return false;
        }
        String dirAbsPath = dir.toAbsolutePath().normalize().toString();
        String parentAbsPath = parent.toAbsolutePath().normalize().toString();
        return parentAbsPath.equals(dirAbsPath);
    }

    /**
     * 清除指定路径的缓存
     */
    public void invalidateCache(String path) {
        fingerprintCache.remove(path);
    }

    /**
     * 清除所有缓存
     * 重要：每次快照扫描开始前调用
     */
    public void clearCache() {
        fingerprintCache.clear();
        log.debug("文件指纹缓存已清除");
    }
}