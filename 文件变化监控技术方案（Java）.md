## 文件变化监控技术方案（Java）

### 核心思路

借鉴 Git 的设计，用**快照 + Hash 对比**来检测变化，配合 Java NIO 的 `WatchService` 做实时监控。

------

### Git 版本控制核心思路

```
工作区文件
    ↓ hash (SHA-1/SHA-256)
  对象库 (blob)
    ↓ 构建
  树对象 (tree) → 目录结构快照
    ↓ 指向
  提交对象 (commit) → 版本节点
```

Git 不存储"差异"，而是存储每次**完整快照**，通过 Hash 比对快速判断文件是否变化。我们借用这个思路：**为每个文件/目录计算 Hash，保存快照，下次扫描时对比**。

------

### 整体架构

```
┌─────────────────────────────────────────────────────┐
│                   FileWatchService                   │
├──────────────────┬──────────────────────────────────┤
│  SnapshotEngine  │         WatchService              │
│  (全量快照扫描)    │      (实时事件监听)               │
│                  │                                   │
│  - 首次建立基线   │  - 监听 CREATE/DELETE/MODIFY      │
│  - 定时增量扫描   │  - 目录变化只报目录本身            │
│  - Hash 对比      │  - 防抖去重                       │
└──────────────────┴──────────────────────────────────┘
         ↓                        ↓
    ChangeDetector（统一变化事件聚合 & 去重）
         ↓
    ChangeListener（业务回调）
```

------

### 关键数据结构

java

```java
// 文件节点快照（类比 Git 的 tree entry）
public record FileNode(
    String path,
    boolean isDirectory,
    long size,
    long lastModified,
    String contentHash   // 目录为子节点列表的 hash，文件为内容 hash
) {}

// 快照（类比 Git 的 tree 对象）
public class Snapshot {
    private final Map<String, FileNode> index; // path -> FileNode
    private final long timestamp;
}

// 变化事件
public record ChangeEvent(
    ChangeType type,      // CREATED / DELETED / MODIFIED
    String path,
    boolean isDirectory
) {}
```

------

### 核心实现

#### 1. Hash 计算策略（性能关键）

java

```java
public class HashEngine {

    // 文件 Hash：用 size+mtime 做快速预检，避免不必要的 IO
    public String hashFile(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        
        // 快速路径：size 和 mtime 都没变，认为文件未变化（类似 Git index 的 stat 缓存）
        String cacheKey = attrs.size() + "_" + attrs.lastModifiedTime().toMillis();
        if (statCache.containsKey(path.toString())) {
            StatCache cached = statCache.get(path.toString());
            if (cached.key().equals(cacheKey)) {
                return cached.hash();
            }
        }

        // 真正读取内容计算 hash
        String hash = computeSHA256(path);
        statCache.put(path.toString(), new StatCache(cacheKey, hash));
        return hash;
    }

    // 目录 Hash：只对直接子节点排序后做 hash（不递归）
    // 这样目录下的文件变化会冒泡到父目录
    public String hashDirectory(Path dir, Map<String, FileNode> childNodes) {
        String childrenSignature = childNodes.entrySet().stream()
            .filter(e -> isDirectChild(dir, e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + ":" + e.getValue().contentHash())
            .collect(Collectors.joining("\n"));
        return sha256(childrenSignature);
    }

    private String computeSHA256(Path path) throws IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) digest.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
```

#### 2. 快照引擎（类比 Git index）

java

```java
@Service
public class SnapshotEngine {

    private volatile Snapshot currentSnapshot = Snapshot.empty();

    // 并行扫描 + 分批处理，支持 10w+ 文件
    public Snapshot takeSnapshot(Path root) throws IOException {
        Map<String, FileNode> index = new ConcurrentHashMap<>();

        // 并行遍历（ForkJoinPool）
        try (Stream<Path> stream = Files.walk(root)) {
            stream.parallel()
                  .filter(p -> !shouldIgnore(p))
                  .forEach(p -> {
                      try {
                          FileNode node = buildNode(p);
                          index.put(p.toString(), node);
                      } catch (IOException e) {
                          log.warn("Skip unreadable path: {}", p);
                      }
                  });
        }

        // 自底向上计算目录 hash
        computeDirectoryHashes(index, root);

        return new Snapshot(index, System.currentTimeMillis());
    }

    // 对比两个快照，输出变化列表
    public List<ChangeEvent> diff(Snapshot oldSnap, Snapshot newSnap) {
        List<ChangeEvent> changes = new ArrayList<>();
        Set<String> oldPaths = oldSnap.index().keySet();
        Set<String> newPaths = newSnap.index().keySet();

        // 新增
        Sets.difference(newPaths, oldPaths).forEach(p ->
            changes.add(new ChangeEvent(CREATED, p, newSnap.get(p).isDirectory())));

        // 删除
        Sets.difference(oldPaths, newPaths).forEach(p ->
            changes.add(new ChangeEvent(DELETED, p, oldSnap.get(p).isDirectory())));

        // 修改（Hash 不同）
        Sets.intersection(oldPaths, newPaths).forEach(p -> {
            FileNode o = oldSnap.get(p), n = newSnap.get(p);
            if (!o.contentHash().equals(n.contentHash())) {
                changes.add(new ChangeEvent(MODIFIED, p, n.isDirectory()));
            }
        });

        // ⚡ 关键：目录下文件变化时，只上报目录本身（过滤子项）
        return filterDirectoryChildren(changes);
    }

    // 如果父目录已经在变化列表中，子文件/子目录不重复上报
    private List<ChangeEvent> filterDirectoryChildren(List<ChangeEvent> raw) {
        Set<String> dirChanges = raw.stream()
            .filter(ChangeEvent::isDirectory)
            .map(ChangeEvent::path)
            .collect(Collectors.toSet());

        return raw.stream()
            .filter(e -> {
                // 检查是否有父目录已经变化了
                Path p = Path.of(e.path());
                for (Path parent = p.getParent(); parent != null; parent = parent.getParent()) {
                    if (dirChanges.contains(parent.toString())) return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }
}
```

#### 3. WatchService 实时监听

java

```java
@Component
public class RealtimeWatcher {

    private final WatchService watchService = FileSystems.getDefault().newWatchService();
    private final Map<WatchKey, Path> keyPathMap = new ConcurrentHashMap<>();
    
    // 防抖：同一路径 500ms 内的重复事件合并
    private final Map<String, ScheduledFuture<?>> debounceMap = new ConcurrentHashMap<>();

    public void register(Path dir) throws IOException {
        WatchKey key = dir.register(watchService,
            ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keyPathMap.put(key, dir);

        // 递归注册子目录
        Files.walk(dir)
             .filter(Files::isDirectory)
             .forEach(sub -> registerSilently(sub));
    }

    public void startWatching(Consumer<ChangeEvent> handler) {
        Thread.ofVirtual().start(() -> {  // Java 21 虚拟线程
            while (!Thread.interrupted()) {
                WatchKey key = watchService.take();
                Path watchedDir = keyPathMap.get(key);

                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = watchedDir.resolve((Path) event.context());
                    ChangeType type = toChangeType(event.kind());

                    // 新目录创建时，自动注册监听
                    if (event.kind() == ENTRY_CREATE && Files.isDirectory(changed)) {
                        register(changed);
                    }

                    // 防抖处理
                    debounce(changed.toString(), () -> {
                        boolean isDir = Files.isDirectory(changed) 
                                     || event.kind() == ENTRY_DELETE; // 删除时无法判断，需查快照
                        handler.accept(new ChangeEvent(type, changed.toString(), isDir));
                    });
                }
                key.reset();
            }
        });
    }

    private void debounce(String path, Runnable action) {
        ScheduledFuture<?> existing = debounceMap.get(path);
        if (existing != null) existing.cancel(false);
        debounceMap.put(path, scheduler.schedule(() -> {
            debounceMap.remove(path);
            action.run();
        }, 500, TimeUnit.MILLISECONDS));
    }
}
```

#### 4. 对外服务门面

java

```java
@Service
public class FileWatchService {

    public void start(Path root, ChangeListener listener) {
        // 1. 建立初始快照基线
        Snapshot baseline = snapshotEngine.takeSnapshot(root);

        // 2. 启动实时 WatchService
        realtimeWatcher.register(root);
        realtimeWatcher.startWatching(event -> {
            // 实时事件触发增量快照对比
            Snapshot newSnap = snapshotEngine.takeSnapshot(root);
            List<ChangeEvent> changes = snapshotEngine.diff(currentSnapshot, newSnap);
            currentSnapshot = newSnap;
            changes.forEach(listener::onChange);
        });

        // 3. 兜底：定时全量扫描（防止 WatchService 漏报）
        scheduler.scheduleAtFixedRate(() -> {
            Snapshot newSnap = snapshotEngine.takeSnapshot(root);
            List<ChangeEvent> changes = snapshotEngine.diff(currentSnapshot, newSnap);
            currentSnapshot = newSnap;
            if (!changes.isEmpty()) changes.forEach(listener::onChange);
        }, 5, 5, TimeUnit.MINUTES);
    }
}
```

------

### 性能优化清单（面向 10w+ 文件）

| 优化点             | 方案                                           |
| ------------------ | ---------------------------------------------- |
| **避免全量 IO**    | `stat` 缓存（size + mtime），变了再读内容      |
| **并行扫描**       | `Files.walk().parallel()` + ForkJoinPool       |
| **快照持久化**     | 序列化到本地（Kryo/Protobuf），重启不用重建    |
| **虚拟线程**       | Java 21 `Thread.ofVirtual()`，IO 等待零成本    |
| **事件防抖**       | 500ms 内同路径事件合并，避免编辑器临时文件风暴 |
| **增量对比**       | 只对比 mtime/size 变化的节点，不全量 hash      |
| **目录 hash 冒泡** | 子文件变化 → 父目录 hash 变化，只上报目录      |

### 两种模式对比

|          | WatchService 实时模式    | 快照轮询模式 |
| -------- | ------------------------ | ------------ |
| 延迟     | 毫秒级                   | 秒~分钟级    |
| 可靠性   | 系统负载高时可能漏报     | 不漏报       |
| 跨平台   | Linux inotify 有队列上限 | 完全一致     |
| **建议** | 主要监听手段             | 兜底补偿     |

**推荐双轨制**：WatchService 负责实时响应，定时快照扫描兜底，两者共享同一个 `diff` 逻辑，结果去重后推给业务层。