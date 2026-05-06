package com.hmoob.doc.watch;

/**
 * 文件节点快照（类比Git的tree entry）
 *
 * @param path         文件完整路径
 * @param isDirectory  是否目录
 * @param size         文件大小（字节）
 * @param lastModified 最后修改时间戳
 * @param contentHash  内容哈希（文件为内容hash，目录为子节点列表hash）
 * @author hmoob
 */
public record FileNode(
    String path,
    boolean isDirectory,
    long size,
    long lastModified,
    String contentHash
) {
}
