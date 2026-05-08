package com.hmoob.doc.watch;

import com.hmoob.doc.enums.FileChangeTypeEnum;

/**
 * 文件变化事件
 *
 * @param type        变化类型
 * @param path        文件路径（新路径）
 * @param isDirectory 是否目录
 * @param oldPath     旧路径（仅重命名事件有效）
 * @param fileSize    文件大小（字节）
 * @param contentHash 文件内容hash（变化后，用于日志记录）
 * @param oldHash     旧文件内容hash（仅修改事件有效）
 * @author hmoob
 */
public record ChangeEvent(
    FileChangeTypeEnum type,
    String path,
    boolean isDirectory,
    String oldPath,
    Long fileSize,
    String contentHash,
    String oldHash
) {

    /**
     * 构造普通事件（非重命名，无hash信息）
     */
    public ChangeEvent(FileChangeTypeEnum type, String path, boolean isDirectory) {
        this(type, path, isDirectory, null, null, null, null);
    }

    /**
     * 构造重命名事件
     */
    public ChangeEvent(FileChangeTypeEnum type, String path, boolean isDirectory, String oldPath) {
        this(type, path, isDirectory, oldPath, null, null, null);
    }

    /**
     * 构造完整事件（包含hash信息）
     */
    public ChangeEvent(FileChangeTypeEnum type, String path, boolean isDirectory,
                       String oldPath, Long fileSize, String contentHash, String oldHash) {
        this.type = type;
        this.path = path;
        this.isDirectory = isDirectory;
        this.oldPath = oldPath;
        this.fileSize = fileSize;
        this.contentHash = contentHash;
        this.oldHash = oldHash;
    }
}