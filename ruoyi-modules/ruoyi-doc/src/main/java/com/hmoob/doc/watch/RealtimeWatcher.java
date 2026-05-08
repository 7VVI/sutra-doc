package com.hmoob.doc.watch;

import cn.hutool.crypto.digest.DigestUtil;
import com.hmoob.doc.enums.FileChangeTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 实时文件监听器（基于WatchService）
 * 采用全局队列批量处理机制，确保所有事件按顺序处理
 * <p>
 * 核心功能：
 * 1. 全局事件队列：所有事件收集到同一队列，定时批量处理
 * 2. 重命名识别：将 DELETED + CREATED 组合识别为 RENAMED 事件
 * 3. 目录删除检测：父目录删除时，子文件 MODIFIED 转换为 DELETED
 * 4. 重复事件合并：粘贴文件时 CREATED + 多个 MODIFIED 合并为单个 CREATED
 * 5. 目录事件过滤：过滤掉目录本身的 MODIFIED 事件（子文件变化导致）
 *
 * @author hmoob
 */
@Slf4j
public class RealtimeWatcher {

    private final WatchService watchService;
    private final Map<WatchKey, Path> keyPathMap = new ConcurrentHashMap<>();
    /** 全局事件队列 */
    private final List<PendingEvent> globalEventQueue = new ArrayList<>();
    /** 全局防抖调度器 */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final int debounceMs;
    private volatile boolean running = false;
    private Thread watchThread;
    private ScheduledFuture<?> batchProcessTask;
    private final Object queueLock = new Object();

    public RealtimeWatcher(int debounceMs) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.debounceMs = debounceMs;
    }

    /**
     * 注册目录监听
     *
     * @param dir       目录
     * @param recursive 是否递归
     */
    public void register(Path dir, boolean recursive) throws IOException {
        registerDirectory(dir);
        if (recursive) {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path subDir, BasicFileAttributes attrs) {
                    try {
                        registerDirectory(subDir);
                    } catch (IOException e) {
                        log.warn("注册目录监听失败: {}", subDir, e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keyPathMap.put(key, dir);
    }

    /**
     * 开始监听
     *
     * @param handler 变化事件处理器
     */
    public void startWatching(Consumer<ChangeEvent> handler) {
        if (running) {
            return;
        }
        running = true;

        // 启动定时批量处理任务
        batchProcessTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                processBatchEvents(handler);
            } catch (Exception e) {
                log.error("批量处理事件失败", e);
            }
        }, debounceMs, debounceMs, TimeUnit.MILLISECONDS);

        // 启动事件收集线程
        watchThread = new Thread(() -> {
            log.info("文件实时监听线程启动");
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key == null) {
                        continue;
                    }

                    Path watchedDir = keyPathMap.get(key);
                    if (watchedDir == null) {
                        key.reset();
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == OVERFLOW) {
                            continue;
                        }

                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path changed = watchedDir.resolve(pathEvent.context());
                        String changedPath = changed.toAbsolutePath().normalize().toString();

                        // 新目录创建时，自动注册监听
                        if (kind == ENTRY_CREATE && Files.isDirectory(changed)) {
                            try {
                                register(changed, true);
                            } catch (IOException e) {
                                log.warn("注册新目录监听失败: {}", changed, e);
                            }
                        }

                        // 收集事件到全局队列
                        PendingEvent pending = new PendingEvent(
                            toChangeType(kind),
                            changedPath,
                            Files.isDirectory(changed)
                        );

                        synchronized (queueLock) {
                            globalEventQueue.add(pending);
                        }
                    }
                    key.reset();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ClosedWatchServiceException e) {
                    break;
                }
            }
            log.info("文件实时监听线程停止");
        });
        watchThread.setName("kb-file-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    /**
     * 停止监听
     */
    public void stopWatching() {
        running = false;
        try {
            watchService.close();
        } catch (IOException e) {
            log.warn("关闭WatchService异常", e);
        }
        if (watchThread != null) {
            watchThread.interrupt();
        }
        if (batchProcessTask != null) {
            batchProcessTask.cancel(false);
        }
        scheduler.shutdownNow();
        synchronized (queueLock) {
            globalEventQueue.clear();
        }
        keyPathMap.clear();
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * 批量处理事件队列
     * 核心逻辑：从队列取出所有事件，进行合并和优化处理
     */
    private void processBatchEvents(Consumer<ChangeEvent> handler) {
        List<PendingEvent> batchEvents;
        synchronized (queueLock) {
            if (globalEventQueue.isEmpty()) {
                return;
            }
            batchEvents = new ArrayList<>(globalEventQueue);
            globalEventQueue.clear();
        }

        if (batchEvents.isEmpty()) {
            return;
        }

        log.debug("批量处理 {} 个事件", batchEvents.size());

        // 1. 修正 DELETED 事件的 isDirectory 标志
        // 当目录删除时，Files.isDirectory 返回 false（文件已不存在）
        // 需要通过路径关系推断：如果某个路径是其他删除事件的父路径，则它是目录
        inferDeletedDirectories(batchEvents);

        // 2. 按文件路径分组，去除重复事件
        Map<String, List<PendingEvent>> eventsByPath = new LinkedHashMap<>();
        for (PendingEvent event : batchEvents) {
            eventsByPath.computeIfAbsent(event.path, k -> new ArrayList<>()).add(event);
        }

        // 3. 分离文件和目录事件
        List<PendingEvent> fileEvents = new ArrayList<>();
        List<PendingEvent> dirEvents = new ArrayList<>();
        for (PendingEvent event : batchEvents) {
            if (event.isDirectory) {
                dirEvents.add(event);
            } else {
                fileEvents.add(event);
            }
        }

        // 4. 收集被删除的目录路径（用于检测子文件是否被删除）
        Set<String> deletedDirs = new HashSet<>();
        for (PendingEvent dirEvent : dirEvents) {
            if (dirEvent.type == FileChangeTypeEnum.DELETED) {
                deletedDirs.add(dirEvent.path);
            }
        }

        // 5. 检测文件事件合并（重命名、重复事件、目录删除导致的文件删除）
        List<MergedEvent> mergedFileEvents = mergeFileEvents(fileEvents, deletedDirs);

        // 6. 处理目录事件（检测重命名、过滤掉目录本身的 MODIFIED 事件）
        List<ChangeEvent> finalDirEvents = mergeDirectoryEvents(dirEvents);

        // 7. 按路径排序发送事件（保证处理顺序）
        List<ChangeEvent> allEvents = new ArrayList<>();

        // 先添加目录事件（父目录删除应该在子文件删除之前）
        for (ChangeEvent dirEvent : finalDirEvents) {
            allEvents.add(dirEvent);
        }

        // 再添加文件事件
        for (MergedEvent merged : mergedFileEvents) {
            allEvents.add(merged.toChangeEvent());
        }

        // 按路径深度和类型排序：DELETED优先，然后按路径深度
        sortEventsByPriority(allEvents);

        // 发送事件
        for (ChangeEvent event : allEvents) {
            log.debug("发送事件: type={}, path={}, oldPath={}", event.type(), event.path(), event.oldPath());
            handler.accept(event);
        }
    }

    /**
     * 推断 DELETED 事件是否是目录删除
     * 条件：如果某个 DELETED 路径是其他删除事件的父路径，则它是目录
     */
    private void inferDeletedDirectories(List<PendingEvent> batchEvents) {
        // 收集所有 DELETED 事件路径
        List<PendingEvent> deletedEvents = batchEvents.stream()
            .filter(e -> e.type == FileChangeTypeEnum.DELETED)
            .toList();

        // 检查每个 DELETED 路径是否是其他删除事件的父路径
        for (PendingEvent deleted : deletedEvents) {
            if (deleted.isDirectory) {
                continue; // 已经是目录，跳过
            }

            // 检查是否有其他删除事件的路径以当前路径为前缀
            for (PendingEvent other : deletedEvents) {
                if (other.path.equals(deleted.path)) {
                    continue; // 同一路径，跳过
                }

                // 如果其他删除路径是当前路径的子路径，则当前路径是目录
                if (other.path.startsWith(deleted.path + "\\") || other.path.startsWith(deleted.path + "/")) {
                    deleted.isDirectory = true;
                    log.debug("推断目录删除: {} (因为有子路径删除: {})", deleted.path, other.path);
                    break;
                }
            }

            // 检查是否有 CREATED 事件与 DELETED 事件路径对应（目录重命名场景）
            // 如果存在同层级的 CREATED 事件，也可能是目录重命名
            for (PendingEvent created : batchEvents.stream().filter(e -> e.type == FileChangeTypeEnum.CREATED && e.isDirectory).toList()) {
                // 同一父目录下，旧名删除、新名创建
                Path deletedPath = Path.of(deleted.path);
                Path createdPath = Path.of(created.path);
                if (deletedPath.getParent().equals(createdPath.getParent())) {
                    deleted.isDirectory = true;
                    log.debug("推断目录删除（重命名场景）: {} -> {}", deleted.path, created.path);
                    break;
                }
            }
        }
    }

    /**
     * 合并目录事件
     * 检测目录重命名（DELETED + CREATED 组合）
     */
    private List<ChangeEvent> mergeDirectoryEvents(List<PendingEvent> dirEvents) {
        List<ChangeEvent> mergedEvents = new ArrayList<>();

        // 分离 DELETED、CREATED、MODIFIED 事件
        List<PendingEvent> deletedEvents = new ArrayList<>();
        List<PendingEvent> createdEvents = new ArrayList<>();
        List<PendingEvent> modifiedEvents = new ArrayList<>();

        for (PendingEvent event : dirEvents) {
            switch (event.type) {
                case DELETED -> deletedEvents.add(event);
                case CREATED -> createdEvents.add(event);
                case MODIFIED -> modifiedEvents.add(event);
                default -> log.warn("未知的目录事件类型: {}", event.type);
            }
        }

        // 1. 检测目录重命名（DELETED + CREATED 组合）
        Set<PendingEvent> matchedDeleted = new HashSet<>();
        Set<PendingEvent> matchedCreated = new HashSet<>();

        for (PendingEvent deleted : deletedEvents) {
            for (PendingEvent created : createdEvents) {
                if (matchedCreated.contains(created)) {
                    continue;
                }

                // 检查是否可能是目录重命名
                if (isLikelyDirRename(deleted.path, created.path)) {
                    matchedDeleted.add(deleted);
                    matchedCreated.add(created);
                    mergedEvents.add(new ChangeEvent(FileChangeTypeEnum.RENAMED, created.path, true, deleted.path));
                    log.info("检测到目录重命名: {} -> {}", deleted.path, created.path);
                    break;
                }
            }
        }

        // 2. 未匹配的 DELETED 事件
        for (PendingEvent deleted : deletedEvents) {
            if (!matchedDeleted.contains(deleted)) {
                mergedEvents.add(new ChangeEvent(FileChangeTypeEnum.DELETED, deleted.path, true));
            }
        }

        // 3. 未匹配的 CREATED 事件
        for (PendingEvent created : createdEvents) {
            if (!matchedCreated.contains(created)) {
                mergedEvents.add(new ChangeEvent(FileChangeTypeEnum.CREATED, created.path, true));
            }
        }

        // 4. MODIFIED 事件过滤（子文件变化导致的目录MODIFIED，忽略）
        // 目录 MODIFIED 通常是因为子文件变化，不是目录本身的变化
        log.debug("忽略目录 MODIFIED 事件，共 {} 个", modifiedEvents.size());

        return mergedEvents;
    }

    /**
     * 判断是否可能是目录重命名
     * 条件：旧目录不存在，新目录存在
     */
    private boolean isLikelyDirRename(String oldPath, String newPath) {
        Path oldDir = Path.of(oldPath);
        Path newDir = Path.of(newPath);

        // 新目录必须存在
        if (!Files.isDirectory(newDir)) {
            return false;
        }

        // 旧目录必须不存在
        if (Files.exists(oldDir)) {
            return false;
        }

        // 在同一批次事件中，旧目录删除、新目录创建，认为是重命名
        log.debug("目录重命名检测：oldPath不存在，newPath存在");
        return true;
    }

    /**
     * 合并文件事件
     * 处理：重命名、重复事件、目录删除导致的文件删除
     */
    private List<MergedEvent> mergeFileEvents(List<PendingEvent> fileEvents, Set<String> deletedDirs) {
        List<MergedEvent> mergedEvents = new ArrayList<>();

        // 按路径分组
        Map<String, List<PendingEvent>> eventsByPath = new LinkedHashMap<>();
        for (PendingEvent event : fileEvents) {
            eventsByPath.computeIfAbsent(event.path, k -> new ArrayList<>()).add(event);
        }

        // 收集所有 DELETED 和 CREATED 路径（用于检测重命名）
        Set<String> deletedPaths = new HashSet<>();
        Set<String> createdPaths = new HashSet<>();
        Map<String, PendingEvent> firstEventByPath = new LinkedHashMap<>();

        for (PendingEvent event : fileEvents) {
            // 记录每个路径的第一个事件类型
            if (!firstEventByPath.containsKey(event.path)) {
                firstEventByPath.put(event.path, event);
            }

            if (event.type == FileChangeTypeEnum.DELETED) {
                deletedPaths.add(event.path);
            } else if (event.type == FileChangeTypeEnum.CREATED) {
                createdPaths.add(event.path);
            }
        }

        // 1. 检测重命名组合（DELETED + CREATED，文件大小/SHA256相同）
        Set<String> matchedDeletedPaths = new HashSet<>();
        Set<String> matchedCreatedPaths = new HashSet<>();

        for (String deletedPath : deletedPaths) {
            for (String createdPath : createdPaths) {
                if (matchedCreatedPaths.contains(createdPath)) {
                    continue;
                }

                if (isLikelyRename(deletedPath, createdPath)) {
                    matchedDeletedPaths.add(deletedPath);
                    matchedCreatedPaths.add(createdPath);
                    mergedEvents.add(new MergedEvent(FileChangeTypeEnum.RENAMED, createdPath, deletedPath));
                    log.info("检测到文件重命名: {} -> {}", deletedPath, createdPath);
                    break;
                }
            }
        }

        // 2. 处理其他文件事件
        for (Map.Entry<String, PendingEvent> entry : firstEventByPath.entrySet()) {
            String filePath = entry.getKey();
            PendingEvent firstEvent = entry.getValue();

            // 已匹配为重命名的路径跳过
            if (matchedDeletedPaths.contains(filePath) || matchedCreatedPaths.contains(filePath)) {
                continue;
            }

            // 检查该路径的所有事件类型
            List<PendingEvent> pathEvents = eventsByPath.get(filePath);
            boolean hasCreated = pathEvents.stream().anyMatch(e -> e.type == FileChangeTypeEnum.CREATED);
            boolean hasDeleted = pathEvents.stream().anyMatch(e -> e.type == FileChangeTypeEnum.DELETED);
            boolean hasModified = pathEvents.stream().anyMatch(e -> e.type == FileChangeTypeEnum.MODIFIED);

            // 情况A：有 CREATED 事件，合并为 CREATED（忽略后续 MODIFIED）
            if (hasCreated) {
                mergedEvents.add(new MergedEvent(FileChangeTypeEnum.CREATED, filePath, null));
                continue;
            }

            // 情况B：有 DELETED 事件
            if (hasDeleted) {
                mergedEvents.add(new MergedEvent(FileChangeTypeEnum.DELETED, filePath, null));
                continue;
            }

            // 情况C：只有 MODIFIED 事件，检查是否实际被删除（父目录删除）
            if (hasModified && isFileActuallyDeleted(filePath, deletedDirs)) {
                log.info("检测到文件实际被删除（MODIFIED转换为DELETED）: {}", filePath);
                mergedEvents.add(new MergedEvent(FileChangeTypeEnum.DELETED, filePath, null));
                continue;
            }

            // 情况D：正常的 MODIFIED 事件
            if (hasModified) {
                mergedEvents.add(new MergedEvent(FileChangeTypeEnum.MODIFIED, filePath, null));
            }
        }

        return mergedEvents;
    }

    /**
     * 判断两个文件是否可能是重命名关系
     */
    private boolean isLikelyRename(String oldPath, String newPath) {
        Path oldFile = Path.of(oldPath);
        Path newFile = Path.of(newPath);

        // 新文件必须存在
        if (!Files.exists(newFile)) {
            return false;
        }

        // 旧文件必须不存在（真正的删除）
        if (Files.exists(oldFile)) {
            return false;
        }

        try {
            // 文件大小相同是重命名的强信号
            long newSize = Files.size(newFile);
            // 由于旧文件已删除，无法直接比较大小
            // 但可以检查新文件的 SHA256 是否在数据库中已存在（说明是重命名而非新增）
            String newSha256 = DigestUtil.sha256Hex(Files.newInputStream(newFile));

            // 如果新文件的 SHA256 已存在，说明是重命名或复制
            // 这里简化判断：只要旧文件不存在、新文件存在，且在同一批次事件中，认为是重命名
            log.debug("重命名检测：oldPath不存在，newPath存在，size={}, sha256={}", newSize, newSha256);
            return true;

        } catch (IOException e) {
            log.debug("重命名检测失败: {}", e.getMessage());
        }

        return false;
    }

    /**
     * 检查文件是否实际被删除
     */
    private boolean isFileActuallyDeleted(String filePath, Set<String> deletedDirs) {
        Path file = Path.of(filePath);

        // 文件本身不存在
        if (!Files.exists(file)) {
            return true;
        }

        // 检查父目录是否被删除
        Path parentDir = file.getParent();
        if (parentDir != null) {
            String parentPath = parentDir.toAbsolutePath().normalize().toString();
            for (String deletedDir : deletedDirs) {
                if (parentPath.equals(deletedDir) || parentPath.startsWith(deletedDir + "\\") || parentPath.startsWith(deletedDir + "/")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 按优先级排序事件
     * DELETED 事件优先处理（确保删除顺序正确：父目录先删除，子文件后处理）
     */
    private void sortEventsByPriority(List<ChangeEvent> events) {
        events.sort((e1, e2) -> {
            // DELETED 事件优先
            if (e1.type() == FileChangeTypeEnum.DELETED && e2.type() != FileChangeTypeEnum.DELETED) {
                return -1;
            }
            if (e1.type() != FileChangeTypeEnum.DELETED && e2.type() == FileChangeTypeEnum.DELETED) {
                return 1;
            }

            // 同类型按路径深度排序（浅路径优先，即父目录优先）
            int depth1 = countPathDepth(e1.path());
            int depth2 = countPathDepth(e2.path());
            return Integer.compare(depth1, depth2);
        });
    }

    /**
     * 计算路径深度
     */
    private int countPathDepth(String path) {
        return path.split("[\\\\/]").length;
    }

    /**
     * WatchEvent.Kind转换为FileChangeTypeEnum
     */
    private FileChangeTypeEnum toChangeType(WatchEvent.Kind<?> kind) {
        if (kind == ENTRY_CREATE) {
            return FileChangeTypeEnum.CREATED;
        } else if (kind == ENTRY_DELETE) {
            return FileChangeTypeEnum.DELETED;
        } else {
            return FileChangeTypeEnum.MODIFIED;
        }
    }

    // ========== 内部数据结构 ==========

    /**
     * 待处理事件（使用普通类以支持字段修改）
     */
    private static class PendingEvent {
        FileChangeTypeEnum type;
        String path;
        boolean isDirectory;

        PendingEvent(FileChangeTypeEnum type, String path, boolean isDirectory) {
            this.type = type;
            this.path = path;
            this.isDirectory = isDirectory;
        }
    }

    /**
     * 合并后事件
     */
    private record MergedEvent(
        FileChangeTypeEnum type,
        String path,
        String oldPath
    ) {
        ChangeEvent toChangeEvent() {
            return new ChangeEvent(type, path, Files.isDirectory(Path.of(path)), oldPath);
        }
    }
}