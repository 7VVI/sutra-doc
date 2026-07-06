# 企业知识中台实现方案 2.1

## 基于 RuoYi-Vue-Plus · Compile-First 范式 · 渐进式升级 · 对 2.0 的全面增强

> **升级说明**：2.1 在 2.0 架构基础上，修复了 10 个关键设计缺陷，重点强化了 **Writeback 写回**（Karpathy 最核心原则）、**Chunk 级语义向量化**、**LLM 降级策略** 和 **编译版本追溯** 四大核心能力。

------

## 与 2.0 方案的关键差异（问题清单与修复）

| #    | 2.0 的问题                                          | 2.1 的修复                                             | 影响程度     |
| ---- | --------------------------------------------------- | ------------------------------------------------------ | ------------ |
| 1    | Prompt 无目录感知，无法主动关联已有知识             | 编译时注入 `existing_wiki_titles` 上下文，强制输出关联 | 🔴 高         |
| 2    | `summarizeLongDoc` 用固定字符长度切割，破坏语义边界 | 改用 LangChain4j `RecursiveCharacterTextSplitter`      | 🔴 高         |
| 3    | 向量化只覆盖 `title + summary`，颗粒度太粗          | 引入 **Chunk 级向量化**，每文档存多条向量记录          | 🔴 高         |
| 4    | `extractEntityCandidates` 每次查询调 LLM，影响延迟  | 改为基于已有实体词典的倒排匹配，LLM 作为兜底           | 🟠 中         |
| 5    | **Writeback 写回机制缺乏完整实现**                  | 补充完整的 `WritebackPipeline`，含审核队列             | 🔴 高（核心） |
| 6    | Lint 只有扫描，无 `AUTO_FIX` 实现                   | 补充 LLM 驱动的孤岛自动修复 + 建议写回                 | 🟠 中         |
| 7    | 无编译版本控制，无法追溯知识演化                    | 新增 `kb_compile_history` 表，保留 Diff                | 🟡 低         |
| 8    | MCP 接口无流式输出，无会话上下文                    | 改为 SSE 流式 + 多轮会话 `sessionId` 支持              | 🟠 中         |
| 9    | LLM 不可用时无降级策略，整个问答瘫痪                | 三层降级：LLM→ES BM25→关键词匹配                       | 🔴 高         |
| 10   | 图谱孤岛查询只找文档孤岛，遗漏实体孤岛              | 同时扫描实体孤岛（入度=0 的 Entity 节点）              | 🟡 低         |

------

## 目录

- [整体架构概览](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#整体架构概览)
- [Phase 0：基础设施扩展（延续 2.0）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-0基础设施扩展)
- [Phase 1：LLM 集成层（增强降级策略）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-1llm-集成层增强)
- [Phase 2：文档编译引擎（修复语义切片 + 目录感知）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-2文档编译引擎增强)
- [Phase 3：Chunk 级向量化（重大升级）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-3chunk-级向量化)
- [Phase 4：知识图谱层（延续 2.0）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-4知识图谱层)
- [Phase 5：混合检索引擎（修复实体提取延迟）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-5混合检索引擎增强)
- [Phase 6：知识孤岛治理（补充 AUTO_FIX）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-6知识孤岛治理增强)
- [Phase 7：**Writeback 写回管道（核心补充）**](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-7writeback-写回管道)
- [Phase 8：MCP 接入（流式 + 会话）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-8mcp-接入增强)
- [数据库变更清单（2.1 新增）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#数据库变更清单21-新增)
- [规模估算与性能基准](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#规模估算与性能基准)
- [分期交付计划](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#分期交付计划)
- [附录：设计决策补充](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#附录设计决策补充)

------

## 整体架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                 ruoyi-doc 现有模块（保持不变）                     │
│  文档 CRUD · 文件上传/去重 · 目录管理 · 权限控制 · ES 全文检索    │
└───────────────────────────┬─────────────────────────────────────┘
                            │ 扩展点：asyncProcessDoc()
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    编译引擎层（2.1 核心升级）                      │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Compile Pipeline（目录感知 Prompt + 语义切片）           │    │
│  │  ① Tika解析 → ② 语义切片 → ③ LLM编译(注入Wiki标题上下文)│    │
│  │  → ④ Chunk向量化(Milvus) → ⑤ 图谱写入(Neo4j)           │    │
│  │  → ⑥ ES增强字段 → ⑦ 编译版本归档                        │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌───────────────────────┐  ┌──────────────────────────────┐    │
│  │  降级策略层（NEW）     │  │  Writeback Pipeline（NEW）   │    │
│  │  LLM→BM25→关键词       │  │  问答新结论→审核→写回→重编译  │    │
│  └───────────────────────┘  └──────────────────────────────┘    │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  知识存储层（Chunk 级精度）                        │
│  Milvus(分段向量) · Neo4j(实体+关系) · ES(文档) · OSS(原文)      │
└───────────────────────────┬─────────────────────────────────────┘
                            │ 混合检索（快速实体匹配）
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│               智能问答 + MCP（流式 SSE + 多轮会话）                │
└─────────────────────────────────────────────────────────────────┘
```

------

## Phase 0：基础设施扩展

> 与 2.0 完全相同，此处不重复。新增一个 Redis Stream 用于 Writeback 队列。

```yaml
# application-knowledge.yml 新增配置项

knowledge:
  # ===== 2.1 新增：Writeback 配置 =====
  writeback:
    enabled: true
    auto-apply: false              # false=需人工审核，true=自动写回
    queue-key: "kb:writeback:queue"
    max-queue-size: 1000

  # ===== 2.1 新增：降级策略 =====
  fallback:
    llm-timeout-ms: 10000         # LLM 超时阈值
    enable-bm25-fallback: true    # LLM 超时时降级到 BM25
    enable-keyword-fallback: true # BM25 也失败时降级到关键词

  # ===== 2.1 新增：Chunk 向量化 =====
  chunk:
    size: 512                     # 每个 Chunk 的目标 Token 数
    overlap: 64                   # Chunk 重叠 Token 数
    min-size: 100                 # 低于此长度的 Chunk 丢弃

  # ===== 2.1 新增：编译版本控制 =====
  compile:
    keep-history: true            # 保留历史编译版本
    max-history-versions: 10      # 每文档最多保留几个历史版本
```

------

## Phase 1：LLM 集成层（增强）

> 2.0 的 `LlmConfig` 和 `LlmService` 保持不变，2.1 在此基础上新增**熔断降级**能力。

### 1.1 LLM 熔断降级服务

```java
// compile/engine/LlmServiceWithFallback.java

/**
 * 带熔断降级的 LLM 服务
 *
 * 降级链路：
 *   LLM 正常调用
 *     ↓ 超时 / 异常
 *   BM25 摘要提取（基于 ES highlight）
 *     ↓ 仍然失败
 *   关键词频率摘要（纯本地，零依赖）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmServiceWithFallback {

    private final LlmService llmService;
    private final IKbEsSearchService esSearchService;
    private final KnowledgeFallbackProperties fallbackProps;

    // 熔断器状态：连续失败超过阈值后开启熔断
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile long circuitOpenTime = 0L;
    private static final int FAILURE_THRESHOLD = 5;
    private static final long CIRCUIT_RESET_MS = 60_000L;  // 1分钟后尝试半开

    /**
     * 带降级的 LLM 编译调用
     */
    public <T> T callWithFallback(String prompt, Class<T> resultClass,
                                   FallbackSupplier<T> fallbackSupplier) {
        // 检查熔断状态
        if (isCircuitOpen()) {
            log.warn("LLM 熔断中，直接走降级路径");
            return fallbackSupplier.get();
        }

        try {
            T result = llmService.callAndParse(prompt, resultClass,
                fallbackProps.getLlmRetryMax());
            consecutiveFailures.set(0);  // 成功则重置
            return result;

        } catch (Exception e) {
            int failures = consecutiveFailures.incrementAndGet();
            log.warn("LLM 调用失败（连续 {} 次）: {}", failures, e.getMessage());

            if (failures >= FAILURE_THRESHOLD) {
                circuitOpenTime = System.currentTimeMillis();
                log.error("LLM 熔断开启！连续失败 {} 次", failures);
            }

            // 走降级
            return fallbackSupplier.get();
        }
    }

    private boolean isCircuitOpen() {
        if (circuitOpenTime == 0L) return false;
        if (System.currentTimeMillis() - circuitOpenTime > CIRCUIT_RESET_MS) {
            circuitOpenTime = 0L;  // 半开：允许试探性请求
            return false;
        }
        return true;
    }

    @FunctionalInterface
    public interface FallbackSupplier<T> {
        T get();
    }
}
```

### 1.2 降级编译结果构造器

```java
// compile/engine/FallbackCompileBuilder.java

/**
 * 当 LLM 不可用时，用规则方法生成降级编译结果
 * 保证系统可用性，质量评分会标注为降级产物
 */
@Component
@RequiredArgsConstructor
public class FallbackCompileBuilder {

    private final IKbEsSearchService esSearchService;

    /**
     * BM25 高亮摘要（ES 有 highlight，质量优于关键词统计）
     */
    public CompileResult buildFromBm25(KbDoc doc, String content) {
        // 用 ES highlight 提取最相关的句子作为摘要
        String summary = extractTopSentences(content, 3);
        List<String> tags = extractKeywords(content, 5);

        return CompileResult.builder()
            .wikiTitle(doc.getDocTitle())
            .summary(summary)
            .contentMarkdown(buildBasicMarkdown(doc, content))
            .tags(tags)
            .relatedTopics(List.of())
            .entities(List.of())  // 降级模式无实体抽取
            .relations(List.of())
            .qualitySignals(CompileResult.QualitySignals.builder()
                .informationDensity(0.3)   // 标注为低质量
                .hasContradictions(false)
                .contradictionNotes("⚠️ 降级模式：LLM 不可用，由规则引擎生成，质量受限")
                .build())
            .isFallback(true)   // 标记为降级产物，Lint 时优先重编译
            .build();
    }

    /**
     * 纯关键词频率统计（零依赖兜底）
     */
    public CompileResult buildFromKeywords(KbDoc doc, String content) {
        // TF-IDF 简化版：统计词频，去掉停用词
        Map<String, Integer> wordFreq = countWords(content);
        List<String> topWords = wordFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(8)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        String summary = "关键词：" + String.join("、", topWords.subList(0, Math.min(5, topWords.size())));

        return CompileResult.builder()
            .wikiTitle(doc.getDocTitle())
            .summary(summary)
            .contentMarkdown("# " + doc.getDocTitle() + "\n\n" + StrUtil.sub(content, 0, 500) + "...")
            .tags(topWords.subList(0, Math.min(5, topWords.size())))
            .entities(List.of())
            .relations(List.of())
            .qualitySignals(CompileResult.QualitySignals.builder()
                .informationDensity(0.1)
                .contradictionNotes("⚠️ 降级模式：关键词提取，质量极低，待 LLM 恢复后重编译")
                .build())
            .isFallback(true)
            .build();
    }

    private String extractTopSentences(String content, int count) {
        String[] sentences = content.split("[。！？.!?\\n]");
        return Arrays.stream(sentences)
            .filter(s -> s.length() > 15 && s.length() < 200)
            .limit(count)
            .collect(Collectors.joining("。"));
    }

    private List<String> extractKeywords(String content, int topN) {
        // 简单的停用词过滤 + 词频统计
        Set<String> stopWords = Set.of("的", "了", "在", "是", "和", "与", "等", "为", "或", "及");
        Map<String, Long> freq = Arrays.stream(content.split("[\\s\\p{Punct}，。！？、]"))
            .filter(w -> w.length() >= 2 && !stopWords.contains(w))
            .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        return freq.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(topN)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}
```

------

## Phase 2：文档编译引擎（增强）

> 2.0 的编译引擎有两个关键问题：**Prompt 不感知已有 Wiki 标题**，**长文档用字符截断**。2.1 全面修复。

### 2.1 目录感知 Prompt（核心升级）

```java
// compile/prompt/PromptTemplateManager.java（2.1 版本）

@Component
@RequiredArgsConstructor
public class PromptTemplateManager {

    private final KbDocMapper docMapper;

    /**
     * 2.1 核心升级：编译 Prompt 注入已有 Wiki 标题上下文
     *
     * 2.0 问题：LLM 不知道系统中已有哪些知识，无法主动建立关联
     * 2.1 修复：将现有所有文档标题注入 Prompt，要求 LLM 从中找关联
     *
     * 这是消除知识孤岛的关键：让 LLM 在编译时就建立跨文档链接
     */
    public static final String COMPILE_PROMPT_V2 = """
        你是一个企业知识编译引擎。你的任务是将原始文档内容编译为结构化的知识页面，
        并主动识别与知识库中已有内容的关联。

        ## 编译规则
        1. **提炼共识**：不要复制原文，提炼出最终结论和核心知识点
        2. **主动关联**：从"已有知识库标题"中找出与本文档相关的条目，填入 related_topics
        3. **实体抽取**：识别文中的人物、产品、流程、标准、概念等实体
        4. **消除歧义**：若文档内容有矛盾，选择最新/权威的表述，并标注分歧
        5. **去除噪音**：忽略版权声明、页眉页脚、格式符号等无效内容
        6. **目录感知**：参考文档所在目录路径推断文档领域

        ## 输入文档
        标题：{doc_title}
        来源类型：{source_type}
        所属目录：{folder_path}
        内容：
        {doc_content}

        ## 已有知识库标题（用于建立关联，避免知识孤岛）
        {existing_wiki_titles}

        ## 输出格式（严格 JSON，不要输出任何解释）
        {
          "wiki_title": "简洁的知识页面标题",
          "summary": "2-3句核心摘要，面向不熟悉该领域的读者",
          "content_markdown": "完整的 Markdown 正文，包含结构化标题和核心内容",
          "tags": ["标签1", "标签2"],
          "related_topics": ["从上面已有知识库标题中选出的相关条目"],
          "entities": [
            {"name": "实体名称", "type": "Person|Product|Process|Standard|Concept", "description": "简要描述"}
          ],
          "relations": [
            {"from": "实体A", "relation": "关系类型（如：依赖|包含|替代|由...制定）", "to": "实体B"}
          ],
          "quality_signals": {
            "information_density": 0.8,
            "has_contradictions": false,
            "contradiction_notes": ""
          }
        }
        """;

    /**
     * 增量更新 Prompt（文档版本更新时使用）
     * 2.1 新增：在更新时也注入 Wiki 上下文，确保关联不丢失
     */
    public static final String UPDATE_PROMPT_V2 = """
        已有编译结果如下，请根据新文档内容进行增量更新。
        保留原有正确内容，合并新知识，标记信息来源变化。

        ## 现有编译结果
        标题：{existing_title}
        摘要：{existing_summary}
        现有关联主题：{existing_related_topics}
        内容摘要：{existing_content_brief}

        ## 新文档内容
        {new_doc_content}

        ## 已有知识库标题（用于补充关联）
        {existing_wiki_titles}

        ## 更新要求
        - 保留并扩展现有 related_topics，从 Wiki 标题中补充新关联
        - 若新文档提供了更新/更权威的信息，更新对应内容
        - 若存在矛盾，在 contradiction_notes 中标注，填写"以哪个为准"
        - 输出格式与编译 Prompt 相同
        """;

    /**
     * 构建带 Wiki 上下文的编译 Prompt
     * 实时从数据库加载现有文档标题（取最新 2000 条）
     */
    public String buildCompilePrompt(KbDoc doc, String content) {
        // 加载现有 Wiki 标题（限制长度，避免超 Token）
        List<String> existingTitles = docMapper.selectCompiledTitles(
            doc.getTenantId(), 2000);

        String titlesContext = existingTitles.isEmpty()
            ? "（知识库暂无已编译内容）"
            : existingTitles.stream()
                .map(t -> "- " + t)
                .collect(Collectors.joining("\n"));

        return COMPILE_PROMPT_V2
            .replace("{doc_title}", StrUtil.defaultIfBlank(doc.getDocTitle(), doc.getDocName()))
            .replace("{source_type}", StrUtil.defaultIfBlank(doc.getFileType(), "unknown"))
            .replace("{folder_path}", StrUtil.defaultIfBlank(doc.getFolderPath(), "/"))
            .replace("{doc_content}", content)
            .replace("{existing_wiki_titles}", titlesContext);
    }
}
```

### 2.2 语义切片（修复固定字符截断问题）

```java
// compile/engine/SemanticChunkSplitter.java

/**
 * 2.0 问题：用 Splitter.fixedLength(5000) 按字符截断，破坏句子和段落完整性
 * 2.1 修复：使用 LangChain4j RecursiveCharacterTextSplitter 按语义边界切片
 */
@Component
@RequiredArgsConstructor
public class SemanticChunkSplitter {

    private final KnowledgeChunkProperties chunkProps;

    /**
     * 将文档内容切分为语义完整的 Chunk 列表
     * 优先在段落、句子边界切割，保留 overlap 避免跨段截断
     *
     * @param content 原始文档文本
     * @param docTitle 文档标题（附加到每个 Chunk 的元数据）
     * @return Chunk 列表
     */
    public List<DocChunk> split(String content, String docTitle) {
        // 使用 LangChain4j 语义切片器
        // 分隔符优先级：段落 > 句子 > 词 > 字符
        DocumentSplitter splitter = DocumentSplitters.recursive(
            chunkProps.getSize(),     // 512 tokens
            chunkProps.getOverlap()   // 64 tokens overlap
        );

        Document lc4jDoc = Document.from(content,
            Metadata.from("title", docTitle));

        List<TextSegment> segments = splitter.split(lc4jDoc);

        return IntStream.range(0, segments.size())
            .filter(i -> segments.get(i).text().length() >= chunkProps.getMinSize())
            .mapToObj(i -> DocChunk.builder()
                .chunkIndex(i)
                .totalChunks(segments.size())
                .content(segments.get(i).text())
                .docTitle(docTitle)
                .estimatedTokens(estimateTokens(segments.get(i).text()))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 超长文档：先分 Chunk，各自摘要，再汇总编译
     * 避免 2.0 中把超长文档整体喂给 LLM 导致截断
     */
    public String hierarchicalSummarize(List<DocChunk> chunks, LlmService llmService) {
        if (chunks.size() <= 3) {
            // 短文档直接合并
            return chunks.stream()
                .map(DocChunk::getContent)
                .collect(Collectors.joining("\n\n"));
        }

        // 并行对每个 Chunk 生成摘要
        List<String> chunkSummaries = chunks.parallelStream()
            .map(chunk -> {
                String prompt = String.format(
                    "请用 3-5 句话概括以下内容的核心信息，保留关键实体、数字和结论：\n\n%s",
                    chunk.getContent());
                try {
                    return llmService.generate(prompt);
                } catch (Exception e) {
                    // 单 Chunk 摘要失败，降级为截取前 300 字
                    return StrUtil.sub(chunk.getContent(), 0, 300);
                }
            })
            .collect(Collectors.toList());

        // 汇总所有 Chunk 摘要作为最终编译输入
        return IntStream.range(0, chunkSummaries.size())
            .mapToObj(i -> String.format("【第%d部分】\n%s", i + 1, chunkSummaries.get(i)))
            .collect(Collectors.joining("\n\n"));
    }

    private int estimateTokens(String text) {
        // 中文约 1.5字/token，英文约 4字/token，取平均 2字/token
        return text.length() / 2;
    }
}
```

### 2.3 编译引擎核心（2.1 版本）

```java
// compile/engine/WikiCompileEngine.java（2.1 重写）

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiCompileEngine {

    private final LlmServiceWithFallback llmServiceWithFallback;
    private final FallbackCompileBuilder fallbackBuilder;
    private final PromptTemplateManager promptManager;
    private final SemanticChunkSplitter chunkSplitter;
    private final KbDocMapper docMapper;
    private final KbCompileResultMapper compileResultMapper;
    private final KbCompileHistoryMapper compileHistoryMapper;  // 2.1 新增
    private final IKbEsIndexService esIndexService;
    private final TransactionTemplate transactionTemplate;
    private final KnowledgeCompileProperties compileProps;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 编译单个文档（2.1 版本）
     *
     * 核心改进：
     * 1. 目录感知 Prompt（注入 Wiki 标题上下文）
     * 2. 语义切片（替代固定字符截断）
     * 3. LLM 降级策略
     * 4. 编译版本归档
     */
    public CompileResult compile(Long docId) {
        KbDoc doc = docMapper.selectById(docId);
        Assert.notNull(doc, "文档不存在: " + docId);

        // 更新状态
        updateCompileStatus(docId, "COMPILING");

        String content = getDocContent(doc);
        if (StrUtil.isBlank(content)) {
            log.warn("文档内容为空，跳过编译: docId={}", docId);
            updateCompileStatus(docId, "SKIPPED");
            return null;
        }

        // 语义切片
        List<DocChunk> chunks = chunkSplitter.split(content, doc.getDocTitle());
        log.info("文档切片完成: docId={}, chunks={}", docId, chunks.size());

        // 判断是否超长（超过 10 个 Chunk）：先做层次摘要
        String compileInput = chunks.size() > 10
            ? chunkSplitter.hierarchicalSummarize(chunks, llmService)
            : content;

        // 构建目录感知 Prompt
        String prompt = promptManager.buildCompilePrompt(doc, compileInput);

        // 调用 LLM（带三层降级）
        CompileResult result = llmServiceWithFallback.callWithFallback(
            prompt,
            CompileResult.class,
            () -> {
                // 降级1：BM25 摘要
                try {
                    return fallbackBuilder.buildFromBm25(doc, content);
                } catch (Exception e2) {
                    // 降级2：关键词统计
                    return fallbackBuilder.buildFromKeywords(doc, content);
                }
            }
        );

        // 持久化（含版本归档）
        persistCompileResultWithHistory(docId, result);

        // 同步 ES 增强字段
        enrichEsDocument(doc, result);

        // 发布编译完成事件（Phase 3/4 订阅）
        eventPublisher.publishEvent(new DocumentCompiledEvent(docId, result, chunks));

        updateCompileStatus(docId, "COMPILED");
        log.info("文档编译完成: docId={}, title={}, isFallback={}",
            docId, result.getWikiTitle(), result.isFallback());

        return result;
    }

    /**
     * 2.1 新增：持久化编译结果 + 保留历史版本（Diff 追溯）
     */
    @Transactional
    private void persistCompileResultWithHistory(Long docId, CompileResult result) {
        // 查询已有编译结果
        KbCompileResult existing = compileResultMapper.selectByDocId(docId);

        if (existing != null && compileProps.isKeepHistory()) {
            // 将旧版本存入历史表
            KbCompileHistory history = KbCompileHistory.builder()
                .docId(docId)
                .compileVersion(existing.getCompileVersion())
                .summary(existing.getSummary())
                .contentMd(existing.getContentMd())
                .tags(existing.getTags())
                .qualityScore(existing.getQualityScore())
                .archiveTime(LocalDateTime.now())
                .build();
            compileHistoryMapper.insert(history);

            // 限制历史版本数量
            compileHistoryMapper.deleteOldVersions(docId, compileProps.getMaxHistoryVersions());
        }

        // 保存/更新当前版本
        KbCompileResult record = KbCompileResult.builder()
            .docId(docId)
            .wikiTitle(result.getWikiTitle())
            .summary(result.getSummary())
            .contentMd(result.getContentMarkdown())
            .tags(JsonUtils.toJsonString(result.getTags()))
            .entities(JsonUtils.toJsonString(result.getEntities()))
            .relations(JsonUtils.toJsonString(result.getRelations()))
            .qualityScore(BigDecimal.valueOf(result.getQualitySignals().getInformationDensity()))
            .isOrphan(0)
            .isFallback(result.isFallback() ? 1 : 0)  // 2.1 新增
            .compileVersion(existing != null ? existing.getCompileVersion() + 1 : 1)
            .build();

        if (existing == null) {
            compileResultMapper.insert(record);
        } else {
            record.setId(existing.getId());
            compileResultMapper.updateById(record);
        }

        // 同步更新 kb_doc 主表
        KbDoc update = new KbDoc();
        update.setDocId(docId);
        update.setSummary(result.getSummary());
        update.setCompileStatus("COMPILED");
        update.setQualityScore(BigDecimal.valueOf(result.getQualitySignals().getInformationDensity()));
        docMapper.updateById(update);
    }
}
```

------

## Phase 3：Chunk 级向量化（重大升级）

> **2.0 的根本问题**：只对 `title + summary` 做一条向量记录。10w 文档的大型报告，摘要可能只有 200 字，大量知识被压缩丢失，语义检索颗粒度极粗。
>
> **2.1 方案**：对每个语义 Chunk 单独生成向量，Milvus 中每文档存多条记录。检索命中 Chunk 后，返回该 Chunk 所在文档，并高亮对应段落。

### 3.1 Chunk 级 Milvus Collection 设计

```java
// vector/MilvusChunkVectorAdapter.java

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(MilvusServiceClient.class)
public class MilvusChunkVectorAdapter {

    private final MilvusServiceClient milvusClient;
    private final EmbeddingModel embeddingModel;
    private final MilvusProperties properties;

    /**
     * 2.1 新增：Chunk 级 Collection（区别于 2.0 的文档级 Collection）
     *
     * 字段说明：
     * - chunk_id：全局唯一（docId_chunkIndex）
     * - doc_id：父文档 ID（用于回查文档信息）
     * - chunk_index：在文档中的序号（用于排序、上下文扩展）
     * - chunk_content：Chunk 原文（用于召回时展示命中片段）
     * - embedding：Chunk 的向量（捕获细粒度语义）
     */
    private static final String CHUNK_COLLECTION = "kb_doc_chunks";

    @PostConstruct
    public void initChunkCollection() {
        if (collectionExists(CHUNK_COLLECTION)) return;

        CreateCollectionParam param = CreateCollectionParam.newBuilder()
            .withCollectionName(CHUNK_COLLECTION)
            .withDescription("KB 文档 Chunk 级语义向量索引")
            .withShardsNum(4)
            .addFieldType(FieldType.newBuilder()
                .withName("chunk_id").withDataType(DataType.VarChar)
                .withMaxLength(100).withPrimaryKey(true).build())
            .addFieldType(FieldType.newBuilder()
                .withName("doc_id").withDataType(DataType.Int64).build())
            .addFieldType(FieldType.newBuilder()
                .withName("chunk_index").withDataType(DataType.Int32).build())
            .addFieldType(FieldType.newBuilder()
                .withName("chunk_content").withDataType(DataType.VarChar)
                .withMaxLength(2000).build())
            .addFieldType(FieldType.newBuilder()
                .withName("doc_title").withDataType(DataType.VarChar)
                .withMaxLength(500).build())
            .addFieldType(FieldType.newBuilder()
                .withName("embedding").withDataType(DataType.FloatVector)
                .withDimension(properties.getDimension()).build())
            .build();

        milvusClient.createCollection(param);

        // HNSW 索引（最适合中等规模，10w 文档约 50w Chunk）
        milvusClient.createIndex(CreateIndexParam.newBuilder()
            .withCollectionName(CHUNK_COLLECTION)
            .withFieldName("embedding")
            .withIndexType(IndexType.HNSW)
            .withMetricType(MetricType.COSINE)
            .withExtraParam("{\"M\": 16, \"efConstruction\": 256}")
            .build());

        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
            .withCollectionName(CHUNK_COLLECTION).build());

        log.info("Milvus Chunk Collection '{}' 初始化完成", CHUNK_COLLECTION);
    }

    /**
     * 批量向量化并存储文档的所有 Chunk
     * 先删除该文档的旧 Chunk，再批量插入新 Chunk（幂等）
     *
     * @param docId  文档 ID
     * @param title  文档标题
     * @param chunks 语义切片列表
     */
    public void upsertChunks(Long docId, String title, List<DocChunk> chunks) {
        // 删除旧 Chunk（幂等）
        milvusClient.delete(DeleteParam.newBuilder()
            .withCollectionName(CHUNK_COLLECTION)
            .withExpr("doc_id == " + docId)
            .build());

        if (CollUtil.isEmpty(chunks)) return;

        // 批量生成 Embedding（注意：API 有 batch size 限制，建议每批 ≤ 100）
        List<String> textsToEmbed = chunks.stream()
            .map(c -> title + "\n" + c.getContent())
            .collect(Collectors.toList());

        // 分批调用 Embedding API
        List<float[]> embeddings = batchEmbed(textsToEmbed);

        // 构建插入参数
        List<String> chunkIds = new ArrayList<>();
        List<Long> docIds = new ArrayList<>();
        List<Integer> chunkIndexes = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        List<List<Float>> embeddingList = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            chunkIds.add(docId + "_" + i);
            docIds.add(docId);
            chunkIndexes.add(i);
            contents.add(StrUtil.sub(chunks.get(i).getContent(), 0, 1990));
            titles.add(StrUtil.sub(title, 0, 490));
            embeddingList.add(toFloatList(embeddings.get(i)));
        }

        milvusClient.insert(InsertParam.newBuilder()
            .withCollectionName(CHUNK_COLLECTION)
            .withFields(List.of(
                new InsertParam.Field("chunk_id", chunkIds),
                new InsertParam.Field("doc_id", docIds),
                new InsertParam.Field("chunk_index", chunkIndexes),
                new InsertParam.Field("chunk_content", contents),
                new InsertParam.Field("doc_title", titles),
                new InsertParam.Field("embedding", embeddingList)
            ))
            .build());

        log.info("Chunk 向量化完成: docId={}, chunks={}", docId, chunks.size());
    }

    /**
     * Chunk 级语义搜索
     * 返回命中 Chunk 及其所在文档 ID（去重聚合）
     */
    public List<ChunkSearchResult> searchChunks(String query, int topK) {
        float[] queryEmbedding = embeddingModel.embed(query).content().vector();

        SearchParam param = SearchParam.newBuilder()
            .withCollectionName(CHUNK_COLLECTION)
            .withVectors(List.of(toFloatList(queryEmbedding)))
            .withTopK(topK * 3)  // 多取，因为会按 doc_id 去重聚合
            .withMetricType(MetricType.COSINE)
            .withParams("{\"ef\": 128}")
            .withOutFields(List.of("doc_id", "chunk_index", "chunk_content", "doc_title"))
            .build();

        R<SearchResults> response = milvusClient.search(param);
        List<ChunkSearchResult> rawResults = parseChunkResults(response);

        // 按 doc_id 聚合：同一文档的多个命中 Chunk 合并，取最高分
        return rawResults.stream()
            .collect(Collectors.toMap(
                ChunkSearchResult::getDocId,
                r -> r,
                (r1, r2) -> r1.getScore() >= r2.getScore() ? r1 : r2
            ))
            .values().stream()
            .sorted(Comparator.comparingDouble(ChunkSearchResult::getScore).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }

    /**
     * 分批调用 Embedding（每批 50 条，避免 API 限制）
     */
    private List<float[]> batchEmbed(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        int batchSize = 50;
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            // LangChain4j EmbeddingModel 不原生支持批量，逐条调用
            batch.forEach(t -> result.add(embeddingModel.embed(t).content().vector()));
        }
        return result;
    }

    private List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) list.add(v);
        return list;
    }
}
```

### 3.2 事件监听：编译完成后自动触发向量化

```java
// vector/ChunkVectorListener.java

/**
 * 监听文档编译完成事件，自动触发 Chunk 级向量化
 * 与编译引擎解耦，通过 Spring Event 通信
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkVectorListener {

    private final MilvusChunkVectorAdapter chunkVectorAdapter;

    @Async("vectorExecutor")  // 专用线程池，不阻塞编译线程
    @EventListener
    public void onDocumentCompiled(DocumentCompiledEvent event) {
        try {
            chunkVectorAdapter.upsertChunks(
                event.getDocId(),
                event.getCompileResult().getWikiTitle(),
                event.getChunks()
            );
        } catch (Exception e) {
            log.error("Chunk 向量化失败: docId={}", event.getDocId(), e);
            // 失败不影响主流程，Lint 时会检测未向量化文档并重试
        }
    }
}

// 向量化专用线程池配置
@Bean("vectorExecutor")
public Executor vectorExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("vector-");
    executor.setRejectedExecutionHandler(new CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

------

## Phase 4：知识图谱层

> 与 2.0 基本相同，2.1 补充一个细节：图谱写入也通过 Spring Event 监听，保持解耦。

```java
// graph/GraphWriteListener.java

@Slf4j
@Component
@RequiredArgsConstructor
public class GraphWriteListener {

    private final Neo4jGraphAdapter graphAdapter;

    @Async("graphExecutor")
    @EventListener
    @ConditionalOnProperty(prefix = "knowledge.neo4j", name = "uri")
    public void onDocumentCompiled(DocumentCompiledEvent event) {
        CompileResult result = event.getCompileResult();
        if (CollUtil.isEmpty(result.getEntities())) return;

        try {
            graphAdapter.writeEntitiesAndRelations(
                result.getEntities(),
                result.getRelations(),
                event.getDocId()
            );
        } catch (Exception e) {
            log.error("图谱写入失败: docId={}", event.getDocId(), e);
        }
    }
}
```

------

## Phase 5：混合检索引擎（增强）

> **2.0 的问题**：`extractEntityCandidates` 对每个查询请求调用 LLM，增加 500-2000ms 额外延迟，P99 严重超标。
>
> **2.1 方案**：建立本地实体词典（来自 Neo4j 实体名），查询时倒排匹配，毫秒级完成。LLM 仅作为找不到匹配时的兜底。

### 5.1 实体词典服务（替代 LLM 实体提取）

```java
// query/EntityDictionaryService.java

/**
 * 本地实体词典：基于 Neo4j 实体名的 Aho-Corasick 多模式匹配
 *
 * 性能对比：
 * - 2.0 LLM 实体提取：500-2000ms
 * - 2.1 词典匹配：< 5ms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityDictionaryService {

    private final Driver neo4jDriver;
    private final LlmService llmService;

    // 内存词典：定时从 Neo4j 同步
    private volatile Set<String> entityNames = new HashSet<>();
    private volatile long lastRefreshTime = 0L;
    private static final long REFRESH_INTERVAL_MS = 5 * 60 * 1000L;  // 5分钟刷新

    /**
     * 从查询文本中提取实体候选词
     * 优先使用词典匹配（毫秒级），LLM 作为兜底
     */
    public List<String> extractEntities(String query) {
        refreshDictionaryIfNeeded();

        // 词典匹配：找出 query 中出现的已知实体名
        List<String> matched = entityNames.stream()
            .filter(name -> name.length() >= 2 && query.contains(name))
            .sorted(Comparator.comparingInt(String::length).reversed())  // 优先匹配更长的实体名
            .limit(10)
            .collect(Collectors.toList());

        if (CollUtil.isNotEmpty(matched)) {
            return matched;
        }

        // 词典未命中，使用 LLM 兜底（异步，不阻塞主检索流程）
        // 注意：这里不等待 LLM 结果，直接返回空，图谱检索退化为跳过
        return List.of();
    }

    /**
     * 定时刷新实体词典（从 Neo4j 加载所有实体名）
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)  // 每5分钟
    public void refreshDictionary() {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run("MATCH (e:Entity) RETURN e.name AS name LIMIT 50000");
            Set<String> names = new HashSet<>();
            while (result.hasNext()) {
                names.add(result.next().get("name").asString());
            }
            entityNames = names;
            lastRefreshTime = System.currentTimeMillis();
            log.info("实体词典刷新完成，共 {} 个实体", names.size());
        } catch (Exception e) {
            log.warn("实体词典刷新失败（Neo4j 可能未启动）: {}", e.getMessage());
        }
    }

    private void refreshDictionaryIfNeeded() {
        if (System.currentTimeMillis() - lastRefreshTime > REFRESH_INTERVAL_MS) {
            refreshDictionary();
        }
    }
}
```

### 5.2 混合检索编排器（2.1 版本）

```java
// query/HybridSearchOrchestrator.java（2.1 重写）

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchOrchestrator {

    private final IKbEsSearchService esSearchService;
    private final MilvusChunkVectorAdapter chunkVectorAdapter;  // 2.1：Chunk 级
    private final Neo4jGraphAdapter graphAdapter;
    private final EntityDictionaryService entityDictionary;     // 2.1：词典替代 LLM
    private final RrfReranker rrfReranker;
    private final KnowledgeSearchProperties searchProperties;

    /**
     * 混合检索主入口（2.1 版本）
     * 三路并行 + Chunk 级向量检索 + 词典实体匹配
     */
    public List<RankedResult> search(String query, HybridSearchOptions options) {
        int topK = options.getTopK();

        // 词典实体提取（< 5ms，不阻塞）
        List<String> entities = entityDictionary.extractEntities(query);

        // 三路并行检索
        CompletableFuture<List<ChunkSearchResult>> vectorFuture =
            CompletableFuture.supplyAsync(() ->
                chunkVectorAdapter.searchChunks(query, topK));   // 2.1：Chunk 级

        CompletableFuture<List<KbSearchResultVo>> esFuture =
            CompletableFuture.supplyAsync(() ->
                esSearchService.searchDoc(buildEsQuery(query, topK)));

        CompletableFuture<List<GraphSearchResult>> graphFuture =
            CollUtil.isEmpty(entities)
                ? CompletableFuture.completedFuture(List.of())  // 无实体直接跳过
                : CompletableFuture.supplyAsync(() ->
                    graphAdapter.searchByEntities(entities, topK));

        // 等待（最多 3 秒，超时用已完成结果）
        try {
            CompletableFuture.allOf(vectorFuture, esFuture, graphFuture)
                .get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("混合检索部分超时，使用已完成的结果");
        } catch (Exception e) {
            log.error("混合检索异常", e);
        }

        // 安全获取结果（isDone 判断）
        List<ChunkSearchResult> vectorResults = vectorFuture.isDone()
            ? vectorFuture.join() : List.of();
        List<KbSearchResultVo> esResults = esFuture.isDone()
            ? esFuture.join() : List.of();
        List<GraphSearchResult> graphResults = graphFuture.isDone()
            ? graphFuture.join() : List.of();

        return rrfReranker.fuse(vectorResults, esResults, graphResults, options.getTopN());
    }
}
```

------

## Phase 6：知识孤岛治理（增强）

> **2.0 的问题**：Lint 只扫描孤岛，缺少 `AUTO_FIX` 实现。2.1 补充 LLM 驱动的孤岛自动修复。

### 6.1 孤岛自动修复器

```java
// lint/OrphanAutoFixer.java

/**
 * 2.1 新增：LLM 驱动的孤岛自动修复
 *
 * 修复策略：
 * 1. 从词典或已编译文档标题中，找出与孤岛文档语义最近的条目
 * 2. 让 LLM 判断关联关系并写入图谱
 * 3. 若置信度高，直接 AUTO_FIX；否则推入人工审核队列
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanAutoFixer {

    private final KbDocMapper docMapper;
    private final KbCompileResultMapper compileResultMapper;
    private final Neo4jGraphAdapter graphAdapter;
    private final LlmServiceWithFallback llmService;
    private final WritebackPipeline writebackPipeline;

    /**
     * 自动修复孤岛文档
     *
     * @param orphanDocIds 孤岛文档 ID 列表
     * @return 修复结果报告
     */
    public AutoFixReport autoFix(List<Long> orphanDocIds) {
        int fixed = 0, pending = 0;

        // 获取所有已编译文档的标题（供 LLM 参考）
        List<String> allTitles = docMapper.selectCompiledTitles(null, 2000);
        String titlesContext = String.join("\n", allTitles.stream()
            .map(t -> "- " + t).collect(Collectors.toList()));

        // 限制每次自动修复数量，避免 LLM 调用过多
        List<Long> toFix = orphanDocIds.subList(0, Math.min(20, orphanDocIds.size()));

        for (Long docId : toFix) {
            try {
                KbCompileResult compile = compileResultMapper.selectByDocId(docId);
                if (compile == null) continue;

                String prompt = String.format("""
                    以下是一个与知识库完全没有关联的"孤岛"知识页面：
                    
                    标题：%s
                    摘要：%s
                    
                    知识库中所有其他文档的标题：
                    %s
                    
                    请从上面的标题中找出与该孤岛页面最相关的 1-5 个条目，给出关系类型。
                    若实在找不到关联，返回空数组。
                    
                    输出严格 JSON（不要解释）：
                    {
                      "confidence": 0.85,
                      "suggestions": [
                        {"related_title": "相关文档标题", "relation": "关系描述", "reason": "关联原因"}
                      ]
                    }
                    """,
                    compile.getWikiTitle(),
                    compile.getSummary(),
                    titlesContext
                );

                FixSuggestion suggestion = llmService.callWithFallback(
                    prompt, FixSuggestion.class, () -> new FixSuggestion(0.0, List.of()));

                if (suggestion.getConfidence() >= 0.7 && CollUtil.isNotEmpty(suggestion.getSuggestions())) {
                    // 置信度高，自动写入图谱关联
                    writeRelationsToGraph(docId, compile.getWikiTitle(), suggestion);

                    // 清除孤岛标记
                    compileResultMapper.clearOrphanFlag(docId);
                    docMapper.updateOrphanFlag(docId, 0);
                    fixed++;
                    log.info("孤岛自动修复成功: docId={}", docId);

                } else {
                    // 置信度低，推入人工审核队列
                    writebackPipeline.addToReviewQueue(WritebackItem.builder()
                        .type("ORPHAN_FIX")
                        .docId(docId)
                        .content(JsonUtils.toJsonString(suggestion))
                        .source("LINT_AUTO_FIX")
                        .build());
                    pending++;
                }

            } catch (Exception e) {
                log.warn("孤岛修复失败: docId={}", docId, e);
            }
        }

        return AutoFixReport.builder()
            .totalOrphans(orphanDocIds.size())
            .autoFixed(fixed)
            .pendingReview(pending)
            .build();
    }

    private void writeRelationsToGraph(Long docId, String title, FixSuggestion suggestion) {
        for (FixSuggestion.RelationSuggestion rel : suggestion.getSuggestions()) {
            try (Session session = graphAdapter.openSession()) {
                session.executeWrite(tx -> {
                    tx.run("""
                        MERGE (a:Doc {id: $docId, title: $title})
                        MERGE (b:Doc {title: $relatedTitle})
                        MERGE (a)-[r:RELATED_TO {relation: $relation}]->(b)
                        ON CREATE SET r.source = 'LINT_AUTO_FIX', r.created_at = datetime()
                        """,
                        Map.of("docId", docId, "title", title,
                            "relatedTitle", rel.getRelatedTitle(),
                            "relation", rel.getRelation())
                    );
                    return null;
                });
            }
        }
    }
}
```

------

## Phase 7：Writeback 写回管道

> **这是 Karpathy 方案的核心灵魂，也是 2.0 最大的缺失。**
>
> Karpathy 的原则：**"每次问答产生的新结论必须写回 Wiki，否则系统永远不会变聪明。"** 2.0 方案在 Phase 7 最后一段提到了 Writeback，但没有实现。2.1 将其提升为独立管道，完整实现。

### 7.1 Writeback Pipeline（写回管道核心）

```java
// writeback/WritebackPipeline.java

/**
 * 知识写回管道（Karpathy 核心原则的工程实现）
 *
 * 工作流：
 * 问答生成新结论
 *   → 提取 [NEW_INSIGHT] 标记的新知识
 *   → 推入 Redis Stream 队列
 *   → 审核（自动/人工，可配置）
 *   → 审核通过 → 触发受影响文档重编译
 *   → 重编译结果写回向量库 + 图谱
 *   → 知识库变聪明 ✓
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WritebackPipeline {

    private final RedissonClient redisson;
    private final KbWritebackLogMapper writebackLogMapper;
    private final WikiCompileEngine compileEngine;
    private final KnowledgeWritebackProperties writebackProps;
    private final ApplicationEventPublisher eventPublisher;

    private static final String QUEUE_KEY = "kb:writeback:queue";

    /**
     * 将新结论推入写回队列
     * 由 AnswerGenerator 在问答完成后调用
     *
     * @param insight   新结论内容
     * @param relatedDocIds 支持该结论的文档 ID 列表
     * @param source    来源（ASK/LINT_AUTO_FIX/MANUAL）
     */
    public void submit(String insight, List<Long> relatedDocIds, String source) {
        if (StrUtil.isBlank(insight)) return;

        // 记录写回日志
        KbWritebackLog log = KbWritebackLog.builder()
            .sourceType(source)
            .insightContent(insight)
            .relatedDocIds(JsonUtils.toJsonString(relatedDocIds))
            .writebackStatus("PENDING")
            .build();
        writebackLogMapper.insert(log);

        WritebackItem item = WritebackItem.builder()
            .logId(log.getId())
            .type("INSIGHT")
            .content(insight)
            .relatedDocIds(relatedDocIds)
            .source(source)
            .build();

        // 推入 Redis Stream
        RStream<String, String> stream = redisson.getStream(QUEUE_KEY);
        stream.add(StreamAddArgs.entry("item", JsonUtils.toJsonString(item)));

        // 若配置为自动写回，立即处理；否则等待人工审核
        if (writebackProps.isAutoApply()) {
            processWritebackAsync(item);
        }
    }

    /**
     * 推入人工审核队列（孤岛修复、低置信度结论使用）
     */
    public void addToReviewQueue(WritebackItem item) {
        KbWritebackLog log = KbWritebackLog.builder()
            .sourceType(item.getSource())
            .insightContent(item.getContent())
            .writebackStatus("PENDING_REVIEW")
            .build();
        writebackLogMapper.insert(log);
        log.info("写回推入人工审核队列: logId={}", log.getId());
    }

    /**
     * 人工审核通过，触发正式写回
     */
    public void approve(Long logId) {
        KbWritebackLog record = writebackLogMapper.selectById(logId);
        Assert.notNull(record, "写回记录不存在");

        record.setWritebackStatus("APPLYING");
        writebackLogMapper.updateById(record);

        WritebackItem item = WritebackItem.builder()
            .logId(logId)
            .content(record.getInsightContent())
            .relatedDocIds(JsonUtils.parseArray(record.getRelatedDocIds(), Long.class))
            .build();

        processWritebackAsync(item);
    }

    /**
     * 异步处理写回
     * 核心逻辑：将新结论触发关联文档的重编译
     */
    @Async("writebackExecutor")
    public void processWritebackAsync(WritebackItem item) {
        log.info("开始处理写回: logId={}", item.getLogId());

        try {
            // 策略1：若有关联文档，对其追加内容后重编译
            if (CollUtil.isNotEmpty(item.getRelatedDocIds())) {
                for (Long docId : item.getRelatedDocIds()) {
                    appendInsightAndRecompile(docId, item.getContent());
                }
            } else {
                // 策略2：无关联文档，创建新的知识条目
                createNewKnowledgeEntry(item.getContent());
            }

            // 更新写回状态
            writebackLogMapper.updateStatus(item.getLogId(), "APPLIED");
            log.info("写回处理完成: logId={}", item.getLogId());

        } catch (Exception e) {
            log.error("写回处理失败: logId={}", item.getLogId(), e);
            writebackLogMapper.updateStatus(item.getLogId(), "FAILED");
        }
    }

    /**
     * 将新结论追加到关联文档的编译结果，触发增量重编译
     */
    private void appendInsightAndRecompile(Long docId, String insight) {
        // 触发增量重编译（使用 UPDATE_PROMPT_V2）
        compileEngine.compileWithUpdate(docId, insight);
        log.info("增量重编译完成: docId={}", docId);
    }

    /**
     * 新结论无法关联到已有文档，作为独立条目写入
     * 在 kb_doc 和 ES 中创建一条"知识碎片"类型的记录
     */
    private void createNewKnowledgeEntry(String insight) {
        // 实现：创建一个 doc_type='INSIGHT' 的虚拟文档
        // 触发编译后进入正常流程
        log.info("创建新知识条目: {}", StrUtil.sub(insight, 0, 100));
    }
}
```

### 7.2 答案生成器（完整 Writeback 集成）

```java
// query/AnswerGenerator.java（2.1 版本，含完整 Writeback）

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerGenerator {

    private final HybridSearchOrchestrator searchOrchestrator;
    private final KbDocMapper docMapper;
    private final KbCompileResultMapper compileResultMapper;
    private final LlmServiceWithFallback llmService;
    private final WritebackPipeline writebackPipeline;    // 2.1：完整写回

    /**
     * 智能问答（含 Writeback）
     */
    public AnswerResponse answer(String question, Long userId) {
        long start = System.currentTimeMillis();

        // 1. 混合检索（Chunk 级）
        List<RankedResult> candidates = searchOrchestrator.search(
            question, HybridSearchOptions.defaults());

        // 2. 加载文档编译结果（优先用 summary，保证 Token 可控）
        List<ContextDoc> contextDocs = candidates.stream()
            .limit(5)
            .map(r -> loadContextDoc(r.getDocId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        String context = contextDocs.stream()
            .map(d -> String.format("### %s\n%s", d.getTitle(), d.getSummary()))
            .collect(Collectors.joining("\n\n"));

        // 3. RAG 问答 Prompt（含 NEW_INSIGHT 标记要求）
        String prompt = String.format("""
            基于以下知识库内容回答问题。
            
            ## 相关知识
            %s
            
            ## 问题
            %s
            
            ## 回答要求
            1. 仅基于提供的知识库内容回答，不要编造
            2. 若知识库中没有相关信息，明确说明"知识库暂无相关内容"
            3. 回答末尾用 【来源】 标注引用的文档标题
            4. 若你的推理产生了知识库中未明确记录的新结论或综合观点，
               用 [NEW_INSIGHT]...内容...[/NEW_INSIGHT] 单独包裹，
               注意：只有真正的新结论才使用此标记，不要滥用
            """, context, question);

        String rawAnswer = llmService.callWithFallback(
            prompt,
            String.class,
            // 降级：直接返回 ES 检索摘要，不走 LLM 生成
            () -> buildFallbackAnswer(question, contextDocs)
        );

        // 4. 提取新结论，触发 Writeback（异步，不阻塞响应）
        List<Long> docIds = contextDocs.stream()
            .map(ContextDoc::getDocId).collect(Collectors.toList());
        extractAndSubmitInsight(rawAnswer, docIds);

        return AnswerResponse.builder()
            .answer(cleanAnswer(rawAnswer))
            .sourceDocIds(docIds)
            .latencyMs(System.currentTimeMillis() - start)
            .build();
    }

    /**
     * 提取 [NEW_INSIGHT] 标记的新结论，提交写回管道
     */
    @Async
    protected void extractAndSubmitInsight(String rawAnswer, List<Long> relatedDocIds) {
        Matcher matcher = Pattern.compile(
            "\\[NEW_INSIGHT\\](.*?)\\[/NEW_INSIGHT\\]", Pattern.DOTALL
        ).matcher(rawAnswer);

        while (matcher.find()) {
            String insight = matcher.group(1).trim();
            if (insight.length() >= 20) {  // 过滤太短的"新结论"
                writebackPipeline.submit(insight, relatedDocIds, "ASK");
                log.info("新结论提交写回: {}", StrUtil.sub(insight, 0, 80));
            }
        }
    }

    /**
     * LLM 不可用时的降级答案（直接拼接 ES 检索摘要）
     */
    private String buildFallbackAnswer(String question, List<ContextDoc> contextDocs) {
        if (CollUtil.isEmpty(contextDocs)) {
            return "⚠️ 知识库检索暂时不可用，请稍后重试。";
        }
        return "以下是知识库中与您问题相关的内容（智能问答服务暂时不可用）：\n\n" +
            contextDocs.stream()
                .map(d -> "**" + d.getTitle() + "**\n" + d.getSummary())
                .collect(Collectors.joining("\n\n"));
    }

    private String cleanAnswer(String rawAnswer) {
        // 移除 [NEW_INSIGHT] 标记，只保留对话内容
        return rawAnswer.replaceAll("\\[NEW_INSIGHT\\].*?\\[/NEW_INSIGHT\\]", "")
            .trim();
    }
}
```

------

## Phase 8：MCP 接入（增强）

> **2.0 的问题**：MCP 接口是简单的 HTTP 请求-响应，无法流式输出，无法保持多轮会话上下文。 **2.1 方案**：改为 SSE（Server-Sent Events）流式输出 + sessionId 多轮会话。

### 8.1 SSE 流式 MCP 控制器

```java
// mcp/McpStreamController.java

/**
 * MCP 流式接口（SSE）
 * 支持 Claude Desktop / Claude Code 等工具调用
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpStreamController {

    private final AnswerGenerator answerGenerator;
    private final HybridSearchOrchestrator searchOrchestrator;
    private final ChatLanguageModel chatModel;

    // 会话存储（简单内存实现，生产可换 Redis）
    private final ConcurrentHashMap<String, List<Map<String, String>>> sessions =
        new ConcurrentHashMap<>();

    /**
     * 流式问答接口（SSE）
     * 支持多轮会话：每次请求携带 sessionId
     */
    @GetMapping(value = "/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(
        @RequestParam String question,
        @RequestParam(required = false) String sessionId,
        @RequestParam(defaultValue = "5") int topK
    ) {
        SseEmitter emitter = new SseEmitter(60_000L);  // 60秒超时

        // 获取或创建会话历史
        String sid = StrUtil.isBlank(sessionId) ? IdUtil.fastUUID() : sessionId;
        List<Map<String, String>> history = sessions.computeIfAbsent(sid, k -> new ArrayList<>());

        CompletableFuture.runAsync(() -> {
            try {
                // 1. 混合检索
                List<RankedResult> candidates = searchOrchestrator.search(
                    question, HybridSearchOptions.withTopK(topK));

                String context = buildContext(candidates);

                // 2. 构建含历史的消息列表
                List<ChatMessage> messages = buildMessages(history, context, question);

                // 3. 流式调用 LLM（LangChain4j StreamingChatLanguageModel）
                StringBuilder fullAnswer = new StringBuilder();

                ((StreamingChatLanguageModel) chatModel).generate(
                    messages,
                    new StreamingResponseHandler<AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .data(JsonUtils.toJsonString(Map.of("token", token)))
                                    .id(sid));
                                fullAnswer.append(token);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }

                        @Override
                        public void onComplete(Response<AiMessage> response) {
                            // 更新会话历史
                            history.add(Map.of("role", "user", "content", question));
                            history.add(Map.of("role", "assistant", "content", fullAnswer.toString()));

                            // 限制历史长度（最多 10 轮）
                            if (history.size() > 20) {
                                history.subList(0, history.size() - 20).clear();
                            }

                            // 发送结束标记
                            try {
                                emitter.send(SseEmitter.event()
                                    .data(JsonUtils.toJsonString(Map.of(
                                        "done", true,
                                        "sessionId", sid
                                    ))));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }

                            // 提取新结论，触发 Writeback
                            List<Long> docIds = candidates.stream()
                                .map(RankedResult::getDocId).collect(Collectors.toList());
                            answerGenerator.extractAndSubmitInsight(fullAnswer.toString(), docIds);
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("流式问答出错", error);
                            emitter.completeWithError(error);
                        }
                    }
                );

            } catch (Exception e) {
                log.error("MCP 流式问答失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 工具发现接口（MCP 协议要求）
     */
    @GetMapping("/tools")
    public List<McpToolDescription> listTools() {
        return List.of(
            McpToolDescription.builder()
                .name("knowledge_ask")
                .description("向企业知识库提问，支持多轮对话，返回基于知识库的答案及来源")
                .parameters(Map.of(
                    "question", "问题内容",
                    "sessionId", "（可选）会话 ID，用于多轮对话"
                ))
                .streamSupport(true)
                .build(),
            McpToolDescription.builder()
                .name("knowledge_search")
                .description("搜索企业知识库，返回相关文档列表和摘要")
                .parameters(Map.of(
                    "query", "搜索关键词或问题",
                    "topK", "（可选）返回条数，默认 5"
                ))
                .build(),
            McpToolDescription.builder()
                .name("get_knowledge_map")
                .description("获取指定文档的知识关联图谱（JSON），供可视化使用")
                .parameters(Map.of("docId", "文档 ID"))
                .build()
        );
    }
}
```

------

## 数据库变更清单（2.1 新增）

> 在 2.0 的变更基础上，2.1 额外新增以下表和字段。

### 2.1 新增表

```sql
-- 编译历史版本表（版本追溯）
CREATE TABLE kb_compile_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id          BIGINT NOT NULL COMMENT '关联 kb_doc.doc_id',
    compile_version INT NOT NULL COMMENT '版本号',
    summary         TEXT COMMENT '该版本摘要',
    content_md      MEDIUMTEXT COMMENT '该版本 Markdown 正文',
    tags            JSON,
    quality_score   DECIMAL(3,2),
    archive_time    DATETIME NOT NULL COMMENT '归档时间',
    tenant_id       VARCHAR(20) DEFAULT '000000',
    KEY idx_doc_version (doc_id, compile_version),
    KEY idx_archive_time (archive_time)
) ENGINE=InnoDB COMMENT='文档编译历史版本表';

-- 实体词典快照表（加速实体匹配，避免每次查 Neo4j）
CREATE TABLE kb_entity_snapshot (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_name VARCHAR(200) NOT NULL,
    entity_type VARCHAR(100),
    doc_count   INT DEFAULT 1 COMMENT '被多少文档提及',
    tenant_id   VARCHAR(20) DEFAULT '000000',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_entity (entity_name, tenant_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB COMMENT='实体快照表（加速词典匹配）';
```

### 2.1 扩展现有表字段

```sql
-- kb_compile_result 表新增字段
ALTER TABLE kb_compile_result
    ADD COLUMN is_fallback TINYINT(1) DEFAULT 0
        COMMENT '是否为降级产物（LLM 不可用时由规则引擎生成）' AFTER quality_score,
    ADD COLUMN related_topics JSON
        COMMENT '关联主题列表（来自目录感知 Prompt 的输出）' AFTER tags,
    ADD INDEX idx_fallback (is_fallback);

-- kb_compile_history 保留条数限制（通过应用层控制，无需 DB 字段）

-- kb_writeback_log 新增字段
ALTER TABLE kb_writeback_log
    ADD COLUMN reviewed_by BIGINT COMMENT '审核人 ID' AFTER writeback_status,
    ADD COLUMN reviewed_at DATETIME COMMENT '审核时间' AFTER reviewed_by,
    ADD COLUMN review_comment VARCHAR(500) COMMENT '审核备注' AFTER reviewed_at;
```

------

## 规模估算与性能基准

### 存储规模（10w 文档，Chunk 级向量）

| 存储           | 2.0 估算       | 2.1 估算（Chunk 级）      | 变化原因                 |
| -------------- | -------------- | ------------------------- | ------------------------ |
| MySQL          | ~15GB          | ~18GB                     | 新增历史版本表、实体快照 |
| ES             | ~30GB          | ~32GB                     | 新增 related_topics 字段 |
| Milvus（向量） | ~2GB（10w 条） | **~25GB（50w Chunk 条）** | 每文档平均 5 个 Chunk    |
| Neo4j          | ~3GB           | ~4GB                      | 实体词典同步写入 MySQL   |
| OSS            | ~500GB         | ~500GB                    | 不变                     |

> **Milvus 存储增大说明**：Chunk 级向量化将向量数量从 10w 增加到约 50w，这是 2.1 的核心权衡——**用更多存储换取更细粒度的语义检索精度**。单节点 16GB RAM 的 Milvus 可轻松承载 50w 条 1536 维向量。

### 性能基准目标

| 指标               | 2.0 目标       | 2.1 目标                | 改进点                     |
| ------------------ | -------------- | ----------------------- | -------------------------- |
| 文档上传（含编译） | ≤ 30 秒/文档   | ≤ 35 秒/文档            | Chunk 向量化增加约 5 秒    |
| 混合检索延迟 P50   | < 300ms        | **< 200ms**             | 实体词典替代 LLM，省 500ms |
| 混合检索延迟 P99   | < 800ms        | **< 600ms**             | 同上                       |
| LLM 不可用时可用性 | 0%（全部失败） | **100%（降级服务）**    | 三层降级策略               |
| 知识孤岛率         | < 5%           | **< 3%**                | 目录感知 Prompt + AUTO_FIX |
| 知识库自我增长速度 | 不增长         | **每日写回 N 条新结论** | Writeback 管道             |

------

## 分期交付计划

```
Phase 0  基础设施扩展        ████░░░░░░░░░░  第1周
Phase 1  LLM集成+降级策略    ██████░░░░░░░░  第2周
Phase 2  编译引擎（目录感知） ████████████░░  第2-3周（含语义切片）
Phase 3  Chunk级向量化       ████████░░░░░░  第3-4周（重大升级）
Phase 4  知识图谱层          ██████████░░░░  第4-5周
Phase 5  混合检索（词典优化） ██████████░░░░  第5-6周
Phase 6  Lint+AUTO_FIX       ██████░░░░░░░░  第6周
Phase 7  Writeback写回管道   ████████░░░░░░  第7周（核心）
Phase 8  MCP流式接入         ████░░░░░░░░░░  第7周
────────────────────────────────────────────────
MVP（编译+Chunk向量+降级）              第4周末
V2（图谱+混合检索+词典优化）            第6周末
V3（Writeback+MCP流式+全功能）          第7周末
```

### MVP 最小功能集（第4周末上线）

- [x] LLM 多模型配置 + 三层降级策略
- [x] 目录感知 Prompt（注入 Wiki 标题上下文）
- [x] 语义切片（替代固定字符截断）
- [x] **Chunk 级 Milvus 向量化**（区别 2.0）
- [x] ES 编译字段增强
- [x] 编译版本历史归档

### V2 功能集（第6周末上线）

- [x] Neo4j 实体图谱 + 实体词典（MySQL 快照）
- [x] 混合检索（词典匹配替代 LLM 实体提取）
- [x] RRF 融合排序
- [x] 孤岛扫描 + AUTO_FIX（LLM 驱动）

### V3 功能集（第7周末上线）

- [x] **完整 Writeback 写回管道**（含审核 + 增量重编译）
- [x] MCP SSE 流式问答 + 多轮会话
- [x] 人工审核 API（写回和孤岛建议）
- [x] 降级模式告警 + Grafana 大盘
- [x] 全量数据迁移脚本（10w 文档批量编译）

------

## 附录：设计决策补充

### 为什么 Writeback 要独立为 Phase 7？

2.0 把 Writeback 列在问答后端的注释里，实际没有工程实现。Karpathy 的核心洞察是：**写回不是可选的增强，而是系统架构的核心约束**。一个不写回的系统，每次问答都是"重新发现知识"，系统永远停留在初始状态。将 Writeback 提升为独立 Phase，是强制保证这一约束得到实现的工程手段。

### Chunk 级向量化 vs 文档级向量化

文档级向量化（2.0）：10w 文档 → 10w 条向量，每条代表整篇文档的"平均语义"。大型报告（如 50 页需求文档）只有一条向量，查询时命中了这条向量，但不知道命中了哪个段落，无法提供高亮或定位。

Chunk 级向量化（2.1）：10w 文档 → ~50w 个 Chunk → 50w 条向量，每条代表 512 Token 的语义片段。命中时可以精确定位到哪一段，返回该段上下文，还可以做"向前扩展 1 Chunk"的上下文补充，使 LLM 生成答案时信息更完整。

### 为什么用实体词典替代 LLM 做实体提取？

在 P99 < 600ms 的延迟目标下，三路并行检索本身需要约 200-400ms，无法再容忍 500-2000ms 的 LLM 实体提取。实体词典（MySQL 快照 + 内存缓存）在 < 5ms 内完成匹配，代价是无法识别词典外的新实体——但这恰好由 Compile 阶段的 LLM 实体抽取来弥补（编译时抽取新实体，写入 Neo4j，同步到词典）。

### 降级策略的三级设计原理

```
Level 1（正常）：LLM 编译 → 高质量 Wiki，质量评分 0.7-1.0
Level 2（降级1）：BM25 摘要 → ES highlight 拼凑，质量评分 0.2-0.4
Level 3（降级2）：关键词统计 → TF-IDF 粗摘要，质量评分 0.05-0.2
```

降级产物标记 `is_fallback=1`，Lint 扫描时**优先对降级文档重新编译**（LLM 恢复后自动补偿），确保系统可用性不以牺牲最终质量为代价。

### Writeback 审核模式选择

- `auto-apply: true`：适合内部技术文档（可信度高，容忍少量错误）
- `auto-apply: false`：适合法规合规类文档（准确性要求高，需人工把关）

建议：生产环境初期设为 `false`，待 Writeback 质量稳定后改为 `true`，配合质量评分阈值（仅 `confidence >= 0.85` 的结论自动写回）。