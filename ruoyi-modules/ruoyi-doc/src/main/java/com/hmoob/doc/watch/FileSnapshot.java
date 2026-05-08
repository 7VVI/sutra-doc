package com.hmoob.doc.watch;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件快照（类比Git的tree对象）
 * 存储文件系统的完整状态，用于差异对比
 *
 * @author hmoob
 */
public class FileSnapshot {

    /** 文件索引：path -> FileNode */
    private final Map<String, FileNode> index;

    /** 快照时间戳 */
    private final long timestamp;

    public FileSnapshot(Map<String, FileNode> index, long timestamp) {
        this.index = index;
        this.timestamp = timestamp;
    }

    /**
     * 创建空快照
     */
    public static FileSnapshot empty() {
        return new FileSnapshot(new ConcurrentHashMap<>(), System.currentTimeMillis());
    }

    /**
     * 获取不可变的索引视图
     */
    public Map<String, FileNode> getIndex() {
        return Collections.unmodifiableMap(index);
    }

    /**
     * 获取指定路径的节点
     */
    public FileNode get(String path) {
        return index.get(path);
    }

    /**
     * 获取快照时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 获取快照中节点数量
     */
    public int size() {
        return index.size();
    }

    /**
     * 判断快照是否为空
     */
    public boolean isEmpty() {
        return index.isEmpty();
    }

    /**
     * 判断路径是否存在于快照中
     */
    public boolean contains(String path) {
        return index.containsKey(path);
    }
}