package com.hmoob.doc.watch;

/**
 * 文件节点快照（类比Git的tree entry）
 *
 * @param path         文件完整路径
 * @param isDirectory  是否目录
 * @param size         文件大小（字节）
 * @param lastModified 最后修改时间戳
 * @param contentHash  快速指纹（文件用CRC32，目录用子节点签名）
 * @param sha256Hash   精确指纹（SHA256，用于重命名检测）
 * @author hmoob
 */
public record FileNode(
    String path,
    boolean isDirectory,
    long size,
    long lastModified,
    String contentHash,
    String sha256Hash
) {
    /**
     * 简化构造（contentHash 和 sha256Hash 相同）
     */
    public FileNode(String path, boolean isDirectory, long size, long lastModified, String hash) {
        this(path, isDirectory, size, lastModified, hash, hash);
    }
}