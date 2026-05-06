package com.hmoob.doc.watch;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件快照（类比Git的tree对象）
 *
 * @author hmoob
 */
public class FileSnapshot {

    private final Map<String, FileNode> index;
    private final long timestamp;

    public FileSnapshot(Map<String, FileNode> index, long timestamp) {
        this.index = index;
        this.timestamp = timestamp;
    }

    public static FileSnapshot empty() {
        return new FileSnapshot(new ConcurrentHashMap<>(), System.currentTimeMillis());
    }

    public Map<String, FileNode> getIndex() {
        return Collections.unmodifiableMap(index);
    }

    public FileNode get(String path) {
        return index.get(path);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int size() {
        return index.size();
    }
}
