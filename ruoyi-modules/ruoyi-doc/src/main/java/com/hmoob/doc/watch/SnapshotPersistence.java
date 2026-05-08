package com.hmoob.doc.watch;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 快照持久化服务
 * 将快照保存为JSON文件，便于下次启动快速加载
 *
 * 快照文件命名规则：snapshot_{configId}.json
 * 存储位置：{user.home}/.kb-watch/snapshots/
 *
 * @author hmoob
 */
@Slf4j
@Component
public class SnapshotPersistence {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 快照存储目录 */
    private static final String SNAPSHOT_DIR = System.getProperty("user.home") + "/.kb-watch/snapshots/";

    /**
     * 保存快照到JSON文件
     *
     * @param snapshot 快照对象
     * @param configId 监控配置ID
     * @return 是否成功
     */
    public boolean save(FileSnapshot snapshot, Long configId) {
        if (snapshot == null || configId == null) {
            return false;
        }

        String filePath = getSnapshotFilePath(configId);
        try {
            // 确保目录存在
            File dir = new File(SNAPSHOT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 转换为可序列化的DTO
            SnapshotDto dto = toDto(snapshot);

            // 写入JSON文件
            objectMapper.writeValue(new File(filePath), dto);

            log.info("快照已保存: configId={}, nodes={}, file={}", configId, snapshot.size(), filePath);
            return true;
        } catch (IOException e) {
            log.error("保存快照失败: configId={}, file={}", configId, filePath, e);
            return false;
        }
    }

    /**
     * 从JSON文件加载快照
     *
     * @param configId 监控配置ID
     * @return 快照对象，如果加载失败返回null
     */
    public FileSnapshot load(Long configId) {
        if (configId == null) {
            return null;
        }

        String filePath = getSnapshotFilePath(configId);
        File file = new File(filePath);

        if (!file.exists()) {
            log.info("快照文件不存在: configId={}, file={}", configId, filePath);
            return null;
        }

        try {
            SnapshotDto dto = objectMapper.readValue(file, SnapshotDto.class);
            FileSnapshot snapshot = fromDto(dto);

            log.info("快照已加载: configId={}, nodes={}, timestamp={}", configId, snapshot.size(), snapshot.getTimestamp());
            return snapshot;
        } catch (IOException e) {
            log.error("加载快照失败: configId={}, file={}", configId, filePath, e);
            return null;
        }
    }

    /**
     * 删除快照文件
     *
     * @param configId 监控配置ID
     * @return 是否成功
     */
    public boolean delete(Long configId) {
        if (configId == null) {
            return false;
        }

        String filePath = getSnapshotFilePath(configId);
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            log.info("快照文件已删除: configId={}, file={}", configId, filePath);
            return true;
        } catch (Exception e) {
            log.error("删除快照失败: configId={}, file={}", configId, filePath, e);
            return false;
        }
    }

    /**
     * 获取快照文件路径
     */
    private String getSnapshotFilePath(Long configId) {
        return SNAPSHOT_DIR + "snapshot_" + configId + ".json";
    }

    /**
     * 转换快照为DTO
     */
    private SnapshotDto toDto(FileSnapshot snapshot) {
        SnapshotDto dto = new SnapshotDto();
        dto.setTimestamp(snapshot.getTimestamp());

        List<FileNodeDto> nodes = snapshot.getIndex().values().stream()
            .map(this::toNodeDto)
            .toList();
        dto.setNodes(nodes);

        return dto;
    }

    /**
     * 转换FileNode为DTO
     */
    private FileNodeDto toNodeDto(FileNode node) {
        FileNodeDto dto = new FileNodeDto();
        dto.setPath(node.path());
        dto.setDirectory(node.isDirectory());
        dto.setSize(node.size());
        dto.setLastModified(node.lastModified());
        dto.setContentHash(node.contentHash());
        dto.setSha256Hash(node.sha256Hash());
        return dto;
    }

    /**
     * 从DTO转换快照
     */
    private FileSnapshot fromDto(SnapshotDto dto) {
        Map<String, FileNode> index = new HashMap<>();

        for (FileNodeDto nodeDto : dto.getNodes()) {
            FileNode node = fromNodeDto(nodeDto);
            index.put(node.path(), node);
        }

        return new FileSnapshot(index, dto.getTimestamp());
    }

    /**
     * 从DTO转换FileNode
     */
    private FileNode fromNodeDto(FileNodeDto dto) {
        return new FileNode(
            dto.getPath(),
            dto.isDirectory(),
            dto.getSize(),
            dto.getLastModified(),
            dto.getContentHash(),
            dto.getSha256Hash()
        );
    }

    /**
     * 快照DTO（用于JSON序列化）
     */
    public static class SnapshotDto {
        private long timestamp;
        private List<FileNodeDto> nodes;

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public List<FileNodeDto> getNodes() { return nodes; }
        public void setNodes(List<FileNodeDto> nodes) { this.nodes = nodes; }
    }

    /**
     * 文件节点DTO（用于JSON序列化）
     */
    public static class FileNodeDto {
        private String path;
        private boolean directory;
        private long size;
        private long lastModified;
        private String contentHash;
        private String sha256Hash;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isDirectory() { return directory; }
        public void setDirectory(boolean directory) { this.directory = directory; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
        public String getContentHash() { return contentHash; }
        public void setContentHash(String contentHash) { this.contentHash = contentHash; }
        public String getSha256Hash() { return sha256Hash; }
        public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    }
}