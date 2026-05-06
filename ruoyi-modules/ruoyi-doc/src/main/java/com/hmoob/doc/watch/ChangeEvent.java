package com.hmoob.doc.watch;

import com.hmoob.doc.enums.FileChangeTypeEnum;

/**
 * 文件变化事件
 *
 * @param type        变化类型
 * @param path        文件路径（新路径）
 * @param isDirectory 是否目录
 * @param oldPath     旧路径（仅重命名事件有效）
 * @author hmoob
 */
public record ChangeEvent(
    FileChangeTypeEnum type,
    String path,
    boolean isDirectory,
    String oldPath
) {

    /**
     * 构造普通事件（非重命名）
     */
    public ChangeEvent(FileChangeTypeEnum type, String path, boolean isDirectory) {
        this(type, path, isDirectory, null);
    }
}
