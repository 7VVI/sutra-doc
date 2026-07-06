1. 企业知识中台实现方案

   ## 基于 Karpathy Compile-First 范式 · Java 全栈 · 支持 10w+ 文档

   > **核心思想**：LLM 的正确用法不是 Q&A，而是**编译（Compile）**。原始文档是源码，Wiki 是编译产物，查询时只读结构化知识，永不重复处理原始文档，主动消除知识孤岛。

   ------

   ## 目录

   - [整体架构概览](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#整体架构概览)
   - [Phase 0：基础设施准备](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-0基础设施准备)
   - [Phase 1：文档摄取管道（Ingest）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-1文档摄取管道ingest)
   - [Phase 2：LLM 编译引擎（Compile）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-2llm-编译引擎compile)
   - [Phase 3：知识存储层](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-3知识存储层)
   - [Phase 4：知识孤岛治理（Lint）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-4知识孤岛治理lint)
   - [Phase 5：混合检索引擎（Query）](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-5混合检索引擎query)
   - [Phase 6：应用与接入层](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-6应用与接入层)
   - [Phase 7：运维与可观测性](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#phase-7运维与可观测性)
   - [规模估算与性能基准](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#规模估算与性能基准)
   - [分期交付计划](https://claude.ai/chat/7872e39e-634b-4355-a65d-568ac4139bfb#分期交付计划)

   ------

   ## 整体架构概览

   ```
   ┌─────────────────────────────────────────────────────────┐
   │                     原始层 raw/（不可变）                  │
   │  PDF · Word · 网页 · 数据库 · 邮件 · 代码 · 图片/视频     │
   └──────────────────────────┬──────────────────────────────┘
                              │ Ingest Pipeline
                              ▼
   ┌─────────────────────────────────────────────────────────┐
   │                  编译引擎层（核心）                        │
   │    Ingest 摄取 → Compile 编译蒸馏 → Lint 质检去孤岛       │
   └──────────────────────────┬──────────────────────────────┘
                              │ Writeback（写回强制）
                              ▼
   ┌─────────────────────────────────────────────────────────┐
   │                  知识存储层 wiki/                          │
   │  Milvus(向量) · Neo4j(图谱) · ES(全文) · MinIO(文件)     │
   └──────────────────────────┬──────────────────────────────┘
                              │ Hybrid Query
                              ▼
   ┌─────────────────────────────────────────────────────────┐
   │               混合检索引擎（RRF 融合排序）                  │
   │          语义向量检索 + 图谱推理 + 全文关键词               │
   └──────────────────────────┬──────────────────────────────┘
                              │ API
                              ▼
   ┌─────────────────────────────────────────────────────────┐
   │                     应用接入层                            │
   │   问答 Bot · 知识地图 · REST/GraphQL · MCP · SSO         │
   └─────────────────────────────────────────────────────────┘
   ```

   ### 技术栈全景

   | 层次       | 技术选型                       | 版本要求     |
   | ---------- | ------------------------------ | ------------ |
   | 主框架     | Spring Boot 3.x + Spring Batch | JDK 21+      |
   | LLM 集成   | LangChain4j + Spring AI        | 最新稳定版   |
   | 文档解析   | Apache Tika 2.x                | 2.9+         |
   | 消息队列   | Apache Kafka                   | 3.6+         |
   | 向量数据库 | Milvus 2.x 或 pgvector         | Milvus 2.4+  |
   | 图数据库   | Neo4j                          | 5.x          |
   | 全文检索   | Elasticsearch                  | 8.x          |
   | 对象存储   | MinIO                          | RELEASE.2024 |
   | 关系数据库 | PostgreSQL                     | 16+          |
   | 缓存       | Redis                          | 7.x          |
   | 定时任务   | Quartz                         | 2.3+         |
   | 监控       | Prometheus + Grafana           | 最新稳定版   |
   | 容器化     | Docker + Kubernetes            | K8s 1.28+    |

   ------

   ## Phase 0：基础设施准备

   **目标**：搭建所有依赖中间件，验证联通性。
    **周期**：第 1 周
    **产出**：可运行的 `docker-compose.yml`，环境健康检查通过

   ### 0.1 Docker Compose 基础环境

   ```yaml
   # docker-compose.yml
   version: '3.9'
   
   services:
     postgres:
       image: pgvector/pgvector:pg16
       environment:
         POSTGRES_DB: knowledge_hub
         POSTGRES_USER: khub
         POSTGRES_PASSWORD: ${DB_PASSWORD}
       volumes:
         - pgdata:/var/lib/postgresql/data
       ports:
         - "5432:5432"
       healthcheck:
         test: ["CMD-SHELL", "pg_isready -U khub"]
         interval: 10s
         timeout: 5s
         retries: 5
   
     redis:
       image: redis:7-alpine
       command: redis-server --requirepass ${REDIS_PASSWORD}
       ports:
         - "6379:6379"
   
     kafka:
       image: confluentinc/cp-kafka:7.6.0
       environment:
         KAFKA_NODE_ID: 1
         KAFKA_PROCESS_ROLES: broker,controller
         KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
         KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
         KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
         KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
         CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
       ports:
         - "9092:9092"
   
     elasticsearch:
       image: elasticsearch:8.13.0
       environment:
         - discovery.type=single-node
         - xpack.security.enabled=false
         - ES_JAVA_OPTS=-Xms2g -Xmx2g
       volumes:
         - esdata:/usr/share/elasticsearch/data
       ports:
         - "9200:9200"
   
     neo4j:
       image: neo4j:5.19-community
       environment:
         NEO4J_AUTH: neo4j/${NEO4J_PASSWORD}
         NEO4J_PLUGINS: '["apoc", "graph-data-science"]'
       volumes:
         - neo4jdata:/data
       ports:
         - "7474:7474"
         - "7687:7687"
   
     minio:
       image: minio/minio:latest
       command: server /data --console-address ":9001"
       environment:
         MINIO_ROOT_USER: ${MINIO_USER}
         MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD}
       volumes:
         - miniodata:/data
       ports:
         - "9000:9000"
         - "9001:9001"
   
     milvus:
       image: milvusdb/milvus:v2.4.5
       command: milvus run standalone
       environment:
         ETCD_ENDPOINTS: etcd:2379
         MINIO_ADDRESS: minio:9000
       depends_on:
         - etcd
         - minio
       ports:
         - "19530:19530"
   
     etcd:
       image: quay.io/coreos/etcd:v3.5.5
       command: etcd -advertise-client-urls=http://127.0.0.1:2379 -listen-client-urls http://0.0.0.0:2379 --data-dir /etcd
   
   volumes:
     pgdata:
     esdata:
     neo4jdata:
     miniodata:
   ```

   ### 0.2 数据库初始化脚本

   ```sql
   -- init.sql（PostgreSQL）
   
   -- 启用向量扩展（pgvector 备用方案）
   CREATE EXTENSION IF NOT EXISTS vector;
   CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
   
   -- 文档元数据表
   CREATE TABLE raw_documents (
       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
       source_path     TEXT NOT NULL,
       source_type     VARCHAR(50) NOT NULL,  -- pdf/word/html/email/code
       file_hash       VARCHAR(64) NOT NULL UNIQUE,
       file_size       BIGINT,
       title           TEXT,
       author          TEXT,
       created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
       updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
       ingest_status   VARCHAR(20) DEFAULT 'PENDING',  -- PENDING/PROCESSING/DONE/FAILED
       compile_status  VARCHAR(20) DEFAULT 'PENDING',
       metadata        JSONB DEFAULT '{}'
   );
   
   -- Wiki 页面表
   CREATE TABLE wiki_pages (
       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
       slug            VARCHAR(500) NOT NULL UNIQUE,
       title           TEXT NOT NULL,
       content         TEXT NOT NULL,           -- Markdown 正文
       summary         TEXT,                    -- LLM 生成的摘要
       tags            TEXT[],
       version         INT DEFAULT 1,
       quality_score   FLOAT DEFAULT 0.0,       -- Lint 质量评分
       is_orphan       BOOLEAN DEFAULT FALSE,   -- 是否为知识孤岛
       source_doc_ids  UUID[],                  -- 来源文档 ID 列表
       created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
       updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
       embedding       vector(1536)             -- pgvector 备用
   );
   
   -- 编译任务日志
   CREATE TABLE compile_jobs (
       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
       job_type        VARCHAR(50) NOT NULL,    -- INGEST/COMPILE/LINT
       status          VARCHAR(20) NOT NULL,
       doc_count       INT DEFAULT 0,
       success_count   INT DEFAULT 0,
       error_count     INT DEFAULT 0,
       started_at      TIMESTAMP WITH TIME ZONE,
       finished_at     TIMESTAMP WITH TIME ZONE,
       error_details   JSONB DEFAULT '[]'
   );
   
   -- 知识实体表
   CREATE TABLE knowledge_entities (
       id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
       name        TEXT NOT NULL,
       type        VARCHAR(100),   -- Person/Concept/Product/Process/Standard
       wiki_page_id UUID REFERENCES wiki_pages(id),
       properties  JSONB DEFAULT '{}'
   );
   
   -- 索引
   CREATE INDEX idx_raw_docs_status ON raw_documents(ingest_status, compile_status);
   CREATE INDEX idx_raw_docs_hash ON raw_documents(file_hash);
   CREATE INDEX idx_wiki_pages_orphan ON wiki_pages(is_orphan) WHERE is_orphan = TRUE;
   CREATE INDEX idx_wiki_pages_quality ON wiki_pages(quality_score);
   CREATE INDEX idx_wiki_embedding ON wiki_pages USING ivfflat (embedding vector_cosine_ops);
   ```

   ### 0.3 Kafka Topic 初始化

   ```bash
   #!/bin/bash
   # init-kafka-topics.sh
   
   KAFKA_BIN="/opt/kafka/bin"
   
   topics=(
     "raw-document-ingest"       # 新文档待摄取
     "document-compile-queue"    # 待编译文档
     "wiki-page-writeback"       # Wiki 写回
     "lint-scan-trigger"         # Lint 触发
     "knowledge-graph-update"    # 图谱更新
   )
   
   for topic in "${topics[@]}"; do
     $KAFKA_BIN/kafka-topics.sh \
       --create \
       --topic "$topic" \
       --partitions 8 \
       --replication-factor 1 \
       --bootstrap-server localhost:9092 \
       --if-not-exists
     echo "Created topic: $topic"
   done
   ```

   ### 0.4 项目模块结构

   ```
   knowledge-hub/
   ├── hub-common/                    # 公共模型、工具类
   │   ├── model/                     # 领域模型（Document、WikiPage、Entity）
   │   ├── exception/                 # 统一异常
   │   └── util/                      # 哈希、文本处理工具
   ├── hub-ingest/                    # Phase 1：摄取模块
   │   ├── parser/                    # Tika 解析器
   │   ├── splitter/                  # 文档切片
   │   ├── batch/                     # Spring Batch Job
   │   └── connector/                 # 各数据源连接器
   ├── hub-compile/                   # Phase 2：编译模块
   │   ├── engine/                    # LLM 编译引擎
   │   ├── prompt/                    # Prompt 模板管理
   │   ├── extractor/                 # 实体关系抽取
   │   └── writer/                    # Wiki 写回器
   ├── hub-storage/                   # Phase 3：存储适配器
   │   ├── vector/                    # Milvus 向量操作
   │   ├── graph/                     # Neo4j 图操作
   │   ├── search/                    # ES 操作
   │   └── object/                    # MinIO 操作
   ├── hub-lint/                      # Phase 4：质检模块
   │   ├── scanner/                   # 孤岛扫描
   │   ├── conflict/                  # 冲突检测
   │   └── scheduler/                 # Quartz 定时任务
   ├── hub-query/                     # Phase 5：检索模块
   │   ├── hybrid/                    # 混合检索
   │   ├── rerank/                    # RRF 重排
   │   └── answer/                    # LLM 答案生成
   └── hub-api/                       # Phase 6：API 网关
       ├── rest/                      # REST 控制器
       ├── graphql/                   # GraphQL Schema
       ├── mcp/                       # MCP Tool 接口
       └── security/                  # Spring Security
   ```

   ------

   ## Phase 1：文档摄取管道（Ingest）

   **目标**：将任意格式原始文档解析、去重、切片，推入编译队列。
    **周期**：第 2-3 周
    **产出**：支持 PDF/Word/HTML/数据库/邮件/代码 的统一摄取管道，处理速度 ≥ 500 文档/分钟

   ### 1.1 核心依赖

   ```xml
   <!-- hub-ingest/pom.xml -->
   <dependencies>
       <!-- 文档解析 -->
       <dependency>
           <groupId>org.apache.tika</groupId>
           <artifactId>tika-core</artifactId>
           <version>2.9.1</version>
       </dependency>
       <dependency>
           <groupId>org.apache.tika</groupId>
           <artifactId>tika-parsers-standard-package</artifactId>
           <version>2.9.1</version>
       </dependency>
   
       <!-- Spring Batch -->
       <dependency>
           <groupId>org.springframework.boot</groupId>
           <artifactId>spring-boot-starter-batch</artifactId>
       </dependency>
   
       <!-- LangChain4j -->
       <dependency>
           <groupId>dev.langchain4j</groupId>
           <artifactId>langchain4j</artifactId>
           <version>0.32.0</version>
       </dependency>
   
       <!-- Kafka -->
       <dependency>
           <groupId>org.springframework.kafka</groupId>
           <artifactId>spring-kafka</artifactId>
       </dependency>
   
       <!-- MinIO -->
       <dependency>
           <groupId>io.minio</groupId>
           <artifactId>minio</artifactId>
           <version>8.5.9</version>
       </dependency>
   </dependencies>
   ```

   ### 1.2 统一文档解析器

   ```java
   // hub-ingest/parser/UniversalDocumentParser.java
   
   @Component
   @Slf4j
   public class UniversalDocumentParser {
   
       private final Tika tika;
       private final MinioStorageAdapter minioAdapter;
   
       public UniversalDocumentParser(MinioStorageAdapter minioAdapter) {
           this.tika = new Tika();
           this.minioAdapter = minioAdapter;
       }
   
       /**
        * 解析任意格式文档，返回统一的 ParsedDocument
        */
       public ParsedDocument parse(RawDocumentSource source) {
           try {
               byte[] rawBytes = source.readBytes();
               String contentHash = Sha256Util.hash(rawBytes);
   
               // 媒体类型自动检测
               MediaType mediaType = tika.detect(rawBytes);
               log.info("Parsing document: {} detected as {}", source.getName(), mediaType);
   
               // 使用 Tika 提取文本和元数据
               Metadata metadata = new Metadata();
               metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, source.getName());
               ParseContext context = new ParseContext();
   
               StringWriter textWriter = new StringWriter();
               AutoDetectParser parser = new AutoDetectParser(tika);
               parser.parse(
                   new ByteArrayInputStream(rawBytes),
                   new BodyContentHandler(textWriter),
                   metadata,
                   context
               );
   
               String rawText = textWriter.toString();
   
               // 将原始文件存入 MinIO
               String storageKey = minioAdapter.storeRaw(rawBytes, contentHash, mediaType.toString());
   
               return ParsedDocument.builder()
                   .contentHash(contentHash)
                   .rawText(rawText)
                   .title(extractTitle(metadata, source.getName()))
                   .author(metadata.get(TikaCoreProperties.CREATOR))
                   .mediaType(mediaType.toString())
                   .sourceType(detectSourceType(mediaType))
                   .charCount(rawText.length())
                   .storageKey(storageKey)
                   .metadataMap(metadataToMap(metadata))
                   .build();
   
           } catch (Exception e) {
               log.error("Failed to parse document: {}", source.getName(), e);
               throw new DocumentParseException("Parse failed: " + source.getName(), e);
           }
       }
   
       private String extractTitle(Metadata metadata, String fileName) {
           String title = metadata.get(TikaCoreProperties.TITLE);
           return StringUtils.hasText(title) ? title : FilenameUtils.getBaseName(fileName);
       }
   
       private SourceType detectSourceType(MediaType mediaType) {
           String type = mediaType.toString().toLowerCase();
           if (type.contains("pdf"))       return SourceType.PDF;
           if (type.contains("word") || type.contains("docx")) return SourceType.WORD;
           if (type.contains("html"))      return SourceType.HTML;
           if (type.contains("text"))      return SourceType.TEXT;
           if (type.contains("email"))     return SourceType.EMAIL;
           return SourceType.OTHER;
       }
   }
   ```

   ### 1.3 语义切片策略

   ```java
   // hub-ingest/splitter/SemanticDocumentSplitter.java
   
   @Component
   public class SemanticDocumentSplitter {
   
       // 每个 Chunk 的目标 Token 数（适配 LLM 上下文）
       private static final int TARGET_CHUNK_TOKENS = 512;
       private static final int OVERLAP_TOKENS = 64;
       private static final int MAX_CHUNK_TOKENS = 800;
   
       private final LangChain4jSplitter langChainSplitter;
   
       public List<DocumentChunk> split(ParsedDocument doc) {
           // 优先按语义段落切割（标题、段落、表格边界）
           DocumentSplitter splitter = DocumentSplitters.recursive(
               TARGET_CHUNK_TOKENS,
               OVERLAP_TOKENS,
               new OpenAiTokenizer()
           );
   
           Document lc4jDoc = Document.from(doc.getRawText(),
               Metadata.from("title", doc.getTitle())
                       .add("source_type", doc.getSourceType().name())
                       .add("doc_hash", doc.getContentHash())
           );
   
           List<TextSegment> segments = splitter.split(lc4jDoc);
   
           return IntStream.range(0, segments.size())
               .mapToObj(i -> DocumentChunk.builder()
                   .chunkIndex(i)
                   .totalChunks(segments.size())
                   .content(segments.get(i).text())
                   .docHash(doc.getContentHash())
                   .docTitle(doc.getTitle())
                   .estimatedTokens(segments.get(i).text().length() / 4) // 粗估
                   .build())
               .collect(Collectors.toList());
       }
   }
   ```

   ### 1.4 Spring Batch 摄取 Job

   ```java
   // hub-ingest/batch/IngestJobConfig.java
   
   @Configuration
   @EnableBatchProcessing
   public class IngestJobConfig {
   
       @Bean
       public Job documentIngestJob(
           JobRepository jobRepository,
           Step scanSourceStep,
           Step parseAndDeduplicateStep,
           Step splitAndQueueStep
       ) {
           return new JobBuilder("documentIngestJob", jobRepository)
               .incrementer(new RunIdIncrementer())
               .start(scanSourceStep)
               .next(parseAndDeduplicateStep)
               .next(splitAndQueueStep)
               .build();
       }
   
       // Step 1: 扫描文档源
       @Bean
       public Step scanSourceStep(JobRepository repo, PlatformTransactionManager tm,
                                  DocumentSourceScanner scanner) {
           return new StepBuilder("scanSource", repo)
               .<RawDocumentSource, RawDocumentSource>chunk(100, tm)
               .reader(scanner.sourceReader())
               .writer(scanner.pendingDocWriter())
               .build();
       }
   
       // Step 2: 解析 + 去重
       @Bean
       public Step parseAndDeduplicateStep(JobRepository repo, PlatformTransactionManager tm,
                                           UniversalDocumentParser parser,
                                           DeduplicationService dedup,
                                           RawDocumentRepository docRepo) {
           return new StepBuilder("parseAndDeduplicate", repo)
               .<RawDocumentSource, ParsedDocument>chunk(20, tm)
               .reader(new PendingDocumentReader(docRepo))
               .processor(new CompositeItemProcessor<>(
                   new ParseProcessor(parser),
                   new DeduplicateProcessor(dedup)  // 基于 SHA256 跳过重复
               ))
               .writer(new ParsedDocumentWriter(docRepo))
               .faultTolerant()
               .skip(DocumentParseException.class)
               .skipLimit(100)
               .build();
       }
   
       // Step 3: 切片 + 推送 Kafka
       @Bean
       public Step splitAndQueueStep(JobRepository repo, PlatformTransactionManager tm,
                                     SemanticDocumentSplitter splitter,
                                     KafkaTemplate<String, CompileTask> kafkaTemplate) {
           return new StepBuilder("splitAndQueue", repo)
               .<ParsedDocument, List<CompileTask>>chunk(10, tm)
               .reader(new ParsedDocumentReader(docRepo))
               .processor(doc -> {
                   List<DocumentChunk> chunks = splitter.split(doc);
                   // 每批 chunks 组成一个 CompileTask
                   return CompileTask.from(doc, chunks);
               })
               .writer(tasks -> tasks.getItems().forEach(task ->
                   kafkaTemplate.send("document-compile-queue", task.getDocHash(), task)
               ))
               .build();
       }
   }
   ```

   ### 1.5 多数据源连接器

   ```java
   // hub-ingest/connector/ConnectorRegistry.java
   
   /**
    * 数据源连接器注册中心
    * 通过 SPI 机制动态扩展，新增连接器只需实现 DocumentSourceConnector 接口
    */
   @Component
   public class ConnectorRegistry {
   
       private final Map<ConnectorType, DocumentSourceConnector> connectors;
   
       public ConnectorRegistry(List<DocumentSourceConnector> connectorList) {
           this.connectors = connectorList.stream()
               .collect(Collectors.toMap(DocumentSourceConnector::getType, c -> c));
       }
   
       public DocumentSourceConnector get(ConnectorType type) {
           DocumentSourceConnector connector = connectors.get(type);
           if (connector == null) throw new UnsupportedConnectorException(type);
           return connector;
       }
   }
   
   // 接口定义
   public interface DocumentSourceConnector {
       ConnectorType getType();
       Flux<RawDocumentSource> stream(ConnectorConfig config);  // 响应式流
       boolean supports(ConnectorConfig config);
   }
   
   // 示例：数据库连接器
   @Component
   public class JdbcDocumentConnector implements DocumentSourceConnector {
   
       @Override
       public ConnectorType getType() { return ConnectorType.DATABASE; }
   
       @Override
       public Flux<RawDocumentSource> stream(ConnectorConfig config) {
           JdbcConnectorConfig jdbcConfig = (JdbcConnectorConfig) config;
           return Flux.fromIterable(
               jdbcTemplate.query(jdbcConfig.getSql(), (rs, row) ->
                   RawDocumentSource.fromDatabase(
                       rs.getString(jdbcConfig.getIdColumn()),
                       rs.getString(jdbcConfig.getContentColumn()),
                       rs.getTimestamp(jdbcConfig.getUpdatedAtColumn())
                   )
               )
           );
       }
   }
   ```

   ------

   ## Phase 2：LLM 编译引擎（Compile）

   **目标**：将摄取的文档 Chunks 通过 LLM 蒸馏为结构化 Wiki 页面，提取实体关系，强制写回。
    **周期**：第 3-5 周
    **产出**：自动生成结构化 Wiki 页面，实体关系写入 Neo4j，编译吞吐量 ≥ 200 文档/小时

   ### 2.1 Prompt 模板管理

   ```java
   // hub-compile/prompt/PromptTemplateRegistry.java
   
   @Component
   public class PromptTemplateRegistry {
   
       // Wiki 页面编译 Prompt（核心）
       public static final String WIKI_COMPILE_PROMPT = """
           你是一个企业知识编译引擎。你的任务是将原始文档内容编译为结构化的 Wiki 知识页面。
           
           ## 编译规则
           1. **提炼共识**：不要复制原文，提炼出最终结论和核心知识点
           2. **建立关联**：识别与其他主题的关联，在 related_topics 中列出
           3. **实体抽取**：识别文中的人物、产品、流程、标准、概念等实体
           4. **消除歧义**：若文档内容有矛盾，选择最新/权威的表述，并标注分歧
           5. **去除噪音**：忽略版权声明、页眉页脚、格式符号等无效内容
           
           ## 输入文档
           标题：{doc_title}
           来源类型：{source_type}
           内容：
           {doc_content}
           
           ## 输出格式（严格 JSON）
           {
             "wiki_title": "简洁的 Wiki 页面标题",
             "summary": "2-3句核心摘要",
             "content_markdown": "完整的 Markdown 正文，包含结构化标题",
             "tags": ["标签1", "标签2"],
             "related_topics": ["相关主题1", "相关主题2"],
             "entities": [
               {"name": "实体名称", "type": "Person|Product|Process|Standard|Concept", "description": "简要描述"}
             ],
             "relations": [
               {"from": "实体A", "relation": "关系类型", "to": "实体B"}
             ],
             "quality_signals": {
               "information_density": 0.8,
               "has_contradictions": false,
               "contradiction_notes": ""
             }
           }
           """;
   
       // 增量更新 Prompt（已有 Wiki 页面时）
       public static final String WIKI_UPDATE_PROMPT = """
           已有 Wiki 页面如下，请根据新文档内容进行增量更新。
           保留原有正确内容，合并新知识，标记信息来源变化。
           
           ## 现有 Wiki 内容
           {existing_wiki}
           
           ## 新文档内容
           {new_doc_content}
           
           ## 更新要求
           - 若新文档提供了更新/更权威的信息，更新对应内容
           - 若新文档补充了新的知识点，追加到合适位置
           - 若存在矛盾，在内容末尾的"⚠️ 知识冲突"区块中标注
           - 输出格式与编译 Prompt 相同
           """;
   }
   ```

   ### 2.2 编译引擎核心

   ```java
   // hub-compile/engine/WikiCompileEngine.java
   
   @Service
   @Slf4j
   public class WikiCompileEngine {
   
       private final ChatLanguageModel llm;
       private final WikiPageRepository wikiRepo;
       private final KnowledgeGraphWriter graphWriter;
       private final VectorStoreAdapter vectorStore;
       private final MeterRegistry metrics;
   
       @KafkaListener(
           topics = "document-compile-queue",
           groupId = "compile-engine",
           concurrency = "8"  // 8 个并发消费者
       )
       public void onCompileTask(CompileTask task) {
           Timer.Sample sample = Timer.start(metrics);
           try {
               log.info("Compiling doc: {} ({} chunks)", task.getDocTitle(), task.getChunks().size());
   
               // 1. 检查是否已有相关 Wiki 页面（增量模式）
               Optional<WikiPage> existingPage = wikiRepo.findByRelatedDocHash(task.getDocHash());
   
               // 2. 构造编译上下文
               String mergedContent = mergeChunks(task.getChunks());
               String prompt = existingPage.isPresent()
                   ? buildUpdatePrompt(existingPage.get(), mergedContent)
                   : buildCompilePrompt(task, mergedContent);
   
               // 3. 调用 LLM 编译（带重试）
               CompileResult result = callLlmWithRetry(prompt, task);
   
               // 4. 强制写回（Writeback is mandatory）
               WikiPage wikiPage = persistWikiPage(result, task, existingPage);
   
               // 5. 写入知识图谱
               graphWriter.writeEntitiesAndRelations(result.getEntities(), result.getRelations(), wikiPage.getId());
   
               // 6. 写入向量存储
               vectorStore.upsert(wikiPage);
   
               // 7. 更新文档编译状态
               updateDocCompileStatus(task.getDocHash(), CompileStatus.DONE);
   
               sample.stop(metrics.timer("compile.success"));
               log.info("Compiled wiki page: {}", wikiPage.getSlug());
   
           } catch (Exception e) {
               log.error("Compile failed for doc: {}", task.getDocHash(), e);
               updateDocCompileStatus(task.getDocHash(), CompileStatus.FAILED);
               metrics.counter("compile.failure").increment();
               // 推送到死信队列
               kafkaTemplate.send("document-compile-queue.DLT", task);
           }
       }
   
       private CompileResult callLlmWithRetry(String prompt, CompileTask task) {
           int maxRetries = 3;
           for (int attempt = 1; attempt <= maxRetries; attempt++) {
               try {
                   String response = llm.generate(prompt);
                   return parseCompileResult(response);
               } catch (JsonParseException e) {
                   log.warn("LLM returned invalid JSON on attempt {}, retrying...", attempt);
                   if (attempt == maxRetries) throw new CompileException("LLM JSON parse failed after retries");
                   // 等待后重试
                   try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
               }
           }
           throw new CompileException("Unreachable");
       }
   
       @Transactional
       private WikiPage persistWikiPage(CompileResult result, CompileTask task, Optional<WikiPage> existing) {
           WikiPage page = existing.orElseGet(WikiPage::new);
           page.setTitle(result.getWikiTitle());
           page.setSlug(SlugUtil.generate(result.getWikiTitle()));
           page.setContent(result.getContentMarkdown());
           page.setSummary(result.getSummary());
           page.setTags(result.getTags());
           page.setQualityScore(result.getQualitySignals().getInformationDensity());
           page.setVersion(page.getVersion() + 1);
           page.setSourceDocIds(appendDocId(page.getSourceDocIds(), task.getDocHash()));
   
           WikiPage saved = wikiRepo.save(page);
   
           // 同步写入 MinIO（Markdown 文件归档）
           minioAdapter.putWikiPage(saved.getSlug(), saved.getContent());
   
           return saved;
       }
   
       private String mergeChunks(List<DocumentChunk> chunks) {
           // 按顺序合并 Chunks，超过 token 限制时进行分层摘要
           int totalTokens = chunks.stream().mapToInt(DocumentChunk::getEstimatedTokens).sum();
           if (totalTokens <= 6000) {
               return chunks.stream().map(DocumentChunk::getContent).collect(Collectors.joining("\n\n"));
           }
           // 超长文档：先对每个 Chunk 摘要，再合并摘要
           return summarizeChunksHierarchically(chunks);
       }
   }
   ```

   ### 2.3 LLM 配置（多模型支持）

   ```java
   // hub-compile/config/LlmConfig.java
   
   @Configuration
   public class LlmConfig {
   
       @Bean
       @ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
       public ChatLanguageModel openAiModel(@Value("${llm.openai.api-key}") String apiKey,
                                             @Value("${llm.openai.model}") String model) {
           return OpenAiChatModel.builder()
               .apiKey(apiKey)
               .modelName(model)           // gpt-4o 或 gpt-4o-mini
               .temperature(0.1)           // 低温度保证一致性
               .maxTokens(4096)
               .responseFormat(ResponseFormat.JSON)   // 强制 JSON 输出
               .timeout(Duration.ofSeconds(120))
               .build();
       }
   
       @Bean
       @ConditionalOnProperty(name = "llm.provider", havingValue = "anthropic")
       public ChatLanguageModel anthropicModel(@Value("${llm.anthropic.api-key}") String apiKey) {
           return AnthropicChatModel.builder()
               .apiKey(apiKey)
               .modelName("claude-3-5-sonnet-20241022")
               .temperature(0.1)
               .maxTokens(4096)
               .build();
       }
   
       @Bean
       @ConditionalOnProperty(name = "llm.provider", havingValue = "local")
       public ChatLanguageModel ollamaModel(@Value("${llm.ollama.base-url}") String baseUrl) {
           // 本地部署（Ollama + Qwen2.5/Llama3.1）
           return OllamaChatModel.builder()
               .baseUrl(baseUrl)
               .modelName("qwen2.5:14b")
               .temperature(0.1)
               .build();
       }
   
       // 嵌入模型（向量化）
       @Bean
       public EmbeddingModel embeddingModel(@Value("${llm.provider}") String provider) {
           return switch (provider) {
               case "openai" -> OpenAiEmbeddingModel.builder()
                   .apiKey(openAiApiKey)
                   .modelName("text-embedding-3-small")
                   .build();
               case "local" -> OllamaEmbeddingModel.builder()
                   .baseUrl(ollamaBaseUrl)
                   .modelName("nomic-embed-text")
                   .build();
               default -> throw new IllegalArgumentException("Unknown provider: " + provider);
           };
       }
   }
   ```

   ### 2.4 知识图谱写入器

   ```java
   // hub-compile/extractor/KnowledgeGraphWriter.java
   
   @Component
   @Slf4j
   public class KnowledgeGraphWriter {
   
       private final Driver neo4jDriver;
   
       /**
        * 将编译结果的实体和关系写入 Neo4j
        */
       @Transactional
       public void writeEntitiesAndRelations(
           List<EntityDto> entities,
           List<RelationDto> relations,
           UUID wikiPageId
       ) {
           try (Session session = neo4jDriver.session()) {
               // 批量 MERGE 实体节点（幂等）
               session.executeWrite(tx -> {
                   for (EntityDto entity : entities) {
                       tx.run("""
                           MERGE (e:Entity {name: $name})
                           ON CREATE SET e.type = $type, e.created_at = datetime()
                           ON MATCH  SET e.type = $type, e.updated_at = datetime()
                           WITH e
                           MATCH (w:WikiPage {id: $wikiPageId})
                           MERGE (e)-[:MENTIONED_IN]->(w)
                           """,
                           Map.of("name", entity.getName(), "type", entity.getType(),
                                  "wikiPageId", wikiPageId.toString())
                       );
                   }
                   return null;
               });
   
               // 批量 MERGE 关系边
               session.executeWrite(tx -> {
                   for (RelationDto rel : relations) {
                       String cypher = String.format("""
                           MERGE (a:Entity {name: $from})
                           MERGE (b:Entity {name: $to})
                           MERGE (a)-[r:%s]->(b)
                           ON CREATE SET r.source_wiki = $wikiPageId, r.created_at = datetime()
                           """, sanitizeRelationType(rel.getRelation()));
                       tx.run(cypher, Map.of(
                           "from", rel.getFrom(),
                           "to", rel.getTo(),
                           "wikiPageId", wikiPageId.toString()
                       ));
                   }
                   return null;
               });
   
               log.info("Written {} entities, {} relations to Neo4j for wiki {}", 
                   entities.size(), relations.size(), wikiPageId);
           }
       }
   
       // 关系类型白名单（防止 Cypher 注入）
       private String sanitizeRelationType(String relation) {
           return relation.toUpperCase()
               .replaceAll("[^A-Z_]", "_")
               .replaceAll("_+", "_");
       }
   }
   ```

   ------

   ## Phase 3：知识存储层

   **目标**：四库协同存储，各司其职，为混合检索奠定基础。
    **周期**：第 3-4 周（与 Phase 2 并行）
    **产出**：向量库、图库、全文索引、文件存储均稳定运行，数据一致性保障

   ### 3.1 Milvus 向量存储适配器

   ```java
   // hub-storage/vector/MilvusVectorAdapter.java
   
   @Component
   @Slf4j
   public class MilvusVectorAdapter implements VectorStoreAdapter {
   
       private static final String COLLECTION_NAME = "wiki_pages";
       private static final int DIMENSION = 1536;         // text-embedding-3-small 维度
   
       private final MilvusServiceClient milvusClient;
       private final EmbeddingModel embeddingModel;
   
       @PostConstruct
       public void initCollection() {
           // 创建 Collection（若不存在）
           if (!collectionExists(COLLECTION_NAME)) {
               CreateCollectionParam param = CreateCollectionParam.newBuilder()
                   .withCollectionName(COLLECTION_NAME)
                   .withDescription("Wiki page vector index")
                   .withShardsNum(4)
                   .addFieldType(FieldType.newBuilder()
                       .withName("id").withDataType(DataType.VarChar).withMaxLength(64).withPrimaryKey(true).build())
                   .addFieldType(FieldType.newBuilder()
                       .withName("wiki_slug").withDataType(DataType.VarChar).withMaxLength(500).build())
                   .addFieldType(FieldType.newBuilder()
                       .withName("title").withDataType(DataType.VarChar).withMaxLength(500).build())
                   .addFieldType(FieldType.newBuilder()
                       .withName("summary").withDataType(DataType.VarChar).withMaxLength(2000).build())
                   .addFieldType(FieldType.newBuilder()
                       .withName("tags").withDataType(DataType.Array)
                       .withElementType(DataType.VarChar).withMaxCapacity(20).build())
                   .addFieldType(FieldType.newBuilder()
                       .withName("embedding").withDataType(DataType.FloatVector).withDimension(DIMENSION).build())
                   .build();
   
               milvusClient.createCollection(param);
   
               // 创建 HNSW 索引（高效 ANN 检索）
               milvusClient.createIndex(CreateIndexParam.newBuilder()
                   .withCollectionName(COLLECTION_NAME)
                   .withFieldName("embedding")
                   .withIndexType(IndexType.HNSW)
                   .withMetricType(MetricType.COSINE)
                   .withExtraParam("{\"M\": 16, \"efConstruction\": 200}")
                   .build());
   
               log.info("Milvus collection '{}' initialized", COLLECTION_NAME);
           }
       }
   
       @Override
       public void upsert(WikiPage page) {
           // 生成 Embedding
           float[] embedding = embeddingModel.embed(page.getSummary() + "\n" + page.getTitle())
               .content().vector();
   
           // 删除旧数据（通过 slug 幂等）
           milvusClient.delete(DeleteParam.newBuilder()
               .withCollectionName(COLLECTION_NAME)
               .withExpr(String.format("wiki_slug == '%s'", page.getSlug()))
               .build());
   
           // 插入新向量
           InsertParam insertParam = InsertParam.newBuilder()
               .withCollectionName(COLLECTION_NAME)
               .withFields(List.of(
                   new InsertParam.Field("id", List.of(page.getId().toString())),
                   new InsertParam.Field("wiki_slug", List.of(page.getSlug())),
                   new InsertParam.Field("title", List.of(page.getTitle())),
                   new InsertParam.Field("summary", List.of(
                       StringUtils.truncate(page.getSummary(), 1990))),
                   new InsertParam.Field("embedding", List.of(toFloatList(embedding)))
               ))
               .build();
   
           milvusClient.insert(insertParam);
       }
   
       @Override
       public List<VectorSearchResult> search(float[] queryEmbedding, int topK, Map<String, Object> filters) {
           SearchParam param = SearchParam.newBuilder()
               .withCollectionName(COLLECTION_NAME)
               .withVectors(List.of(toFloatList(queryEmbedding)))
               .withTopK(topK)
               .withMetricType(MetricType.COSINE)
               .withParams("{\"ef\": 100}")
               .withOutFields(List.of("wiki_slug", "title", "summary"))
               .build();
   
           SearchResults results = milvusClient.search(param).getData();
           return parseSearchResults(results);
       }
   }
   ```

   ### 3.2 Elasticsearch 全文索引

   ```java
   // hub-storage/search/ElasticsearchAdapter.java
   
   @Component
   public class ElasticsearchAdapter {
   
       private static final String INDEX_NAME = "wiki_pages";
       private final ElasticsearchClient esClient;
   
       @PostConstruct
       public void createIndex() throws IOException {
           if (!esClient.indices().exists(e -> e.index(INDEX_NAME)).value()) {
               esClient.indices().create(c -> c
                   .index(INDEX_NAME)
                   .settings(s -> s
                       .numberOfShards("3")
                       .numberOfReplicas("1")
                       .analysis(a -> a
                           .analyzer("ik_smart_analyzer", an -> an
                               .custom(cu -> cu
                                   .tokenizer("ik_smart")  // 中文分词
                                   .filter(List.of("lowercase", "stop"))
                               )
                           )
                       )
                   )
                   .mappings(m -> m
                       .properties("title", p -> p.text(t -> t.analyzer("ik_smart_analyzer").boost(3.0)))
                       .properties("summary", p -> p.text(t -> t.analyzer("ik_smart_analyzer").boost(2.0)))
                       .properties("content", p -> p.text(t -> t.analyzer("ik_smart_analyzer")))
                       .properties("tags", p -> p.keyword(k -> k))
                       .properties("slug", p -> p.keyword(k -> k))
                       .properties("quality_score", p -> p.float_(f -> f))
                       .properties("updated_at", p -> p.date(d -> d))
                   )
               );
           }
       }
   
       public void indexWikiPage(WikiPage page) throws IOException {
           esClient.index(i -> i
               .index(INDEX_NAME)
               .id(page.getSlug())
               .document(Map.of(
                   "title", page.getTitle(),
                   "summary", page.getSummary(),
                   "content", page.getContent(),
                   "tags", page.getTags(),
                   "slug", page.getSlug(),
                   "quality_score", page.getQualityScore(),
                   "updated_at", page.getUpdatedAt()
               ))
           );
       }
   
       public List<EsSearchResult> fullTextSearch(String query, int size) throws IOException {
           SearchResponse<Map> response = esClient.search(s -> s
               .index(INDEX_NAME)
               .query(q -> q
                   .bool(b -> b
                       .should(List.of(
                           Query.of(qb -> qb.match(m -> m.field("title").query(query).boost(3.0f))),
                           Query.of(qb -> qb.match(m -> m.field("summary").query(query).boost(2.0f))),
                           Query.of(qb -> qb.match(m -> m.field("content").query(query)))
                       ))
                       .minimumShouldMatch("1")
                   )
               )
               .size(size)
               .source(src -> src.filter(f -> f.includes("slug", "title", "summary", "quality_score")))
               , Map.class
           );
   
           return response.hits().hits().stream()
               .map(hit -> new EsSearchResult(
                   hit.source().get("slug").toString(),
                   hit.source().get("title").toString(),
                   hit.source().get("summary").toString(),
                   hit.score()
               ))
               .collect(Collectors.toList());
       }
   }
   ```

   ------

   ## Phase 4：知识孤岛治理（Lint）

   **目标**：定期扫描知识图谱，发现孤立节点、冲突内容、过期知识，主动触发修复。
    **周期**：第 5-6 周
    **产出**：孤岛率 < 5%，每日自动 Lint 报告，人工审核队列

   ### 4.1 知识孤岛扫描器

   ```java
   // hub-lint/scanner/OrphanPageScanner.java
   
   @Component
   @Slf4j
   public class OrphanPageScanner {
   
       private final Driver neo4jDriver;
       private final WikiPageRepository wikiRepo;
       private final WikiCompileEngine compileEngine;
       private final NotificationService notifier;
   
       /**
        * 扫描知识孤岛：在 Neo4j 中入度和出度均为 0 的 Wiki 节点
        */
       public LintReport scanOrphans() {
           List<String> orphanSlugs = new ArrayList<>();
   
           try (Session session = neo4jDriver.session()) {
               // 查找孤立 Wiki 节点
               Result result = session.run("""
                   MATCH (w:WikiPage)
                   WHERE NOT (w)-[:RELATED_TO]-()
                     AND NOT ()-[:RELATED_TO]->(w)
                     AND NOT (w)<-[:MENTIONED_IN]-(:Entity)
                   RETURN w.slug AS slug, w.title AS title, w.updated_at AS updatedAt
                   ORDER BY updatedAt ASC
                   LIMIT 500
                   """);
   
               while (result.hasNext()) {
                   Record record = result.next();
                   orphanSlugs.add(record.get("slug").asString());
               }
           }
   
           log.info("Found {} orphan wiki pages", orphanSlugs.size());
   
           // 标记孤岛
           wikiRepo.markAsOrphans(orphanSlugs);
   
           // 尝试自动修复：让 LLM 找关联
           List<String> autoFixed = attemptAutoFix(orphanSlugs.subList(0, Math.min(20, orphanSlugs.size())));
   
           // 剩余推送人工审核
           List<String> needsReview = orphanSlugs.stream()
               .filter(s -> !autoFixed.contains(s))
               .collect(Collectors.toList());
   
           notifier.sendOrphanReport(orphanSlugs.size(), autoFixed.size(), needsReview);
   
           return LintReport.builder()
               .totalOrphans(orphanSlugs.size())
               .autoFixed(autoFixed.size())
               .pendingReview(needsReview.size())
               .reportTime(Instant.now())
               .build();
       }
   
       /**
        * 让 LLM 为孤岛页面找到关联点
        */
       private List<String> attemptAutoFix(List<String> orphanSlugs) {
           List<String> fixed = new ArrayList<>();
   
           // 获取所有 Wiki 页面标题（用于关联候选）
           List<String> allTitles = wikiRepo.findAllTitles();
   
           for (String slug : orphanSlugs) {
               try {
                   WikiPage orphan = wikiRepo.findBySlug(slug).orElseThrow();
   
                   String prompt = String.format("""
                       以下是一个孤立的 Wiki 知识页面（没有与其他页面的关联）：
                       
                       标题：%s
                       摘要：%s
                       
                       以下是系统中所有其他 Wiki 页面的标题列表：
                       %s
                       
                       请从上述列表中找出与该孤立页面最相关的 3-5 个页面，给出关系类型。
                       输出 JSON：[{"title": "相关页面标题", "relation": "关系描述"}]
                       只输出 JSON，不要解释。
                       """,
                       orphan.getTitle(),
                       orphan.getSummary(),
                       String.join("\n", allTitles)
                   );
   
                   String response = llm.generate(prompt);
                   List<RelationSuggestion> suggestions = parseRelations(response);
   
                   if (!suggestions.isEmpty()) {
                       writeRelationsToGraph(orphan, suggestions);
                       wikiRepo.clearOrphanFlag(slug);
                       fixed.add(slug);
                       log.info("Auto-fixed orphan: {}", slug);
                   }
   
               } catch (Exception e) {
                   log.warn("Auto-fix failed for orphan: {}", slug, e);
               }
           }
           return fixed;
       }
   }
   ```

   ### 4.2 冲突检测器

   ```java
   // hub-lint/conflict/ConflictDetector.java
   
   @Component
   @Slf4j
   public class ConflictDetector {
   
       /**
        * 检测同一实体在不同 Wiki 页面的矛盾表述
        */
       public List<ConflictReport> detectConflicts(String entityName) {
           // 1. 找出所有提到该实体的 Wiki 页面
           List<WikiPage> relatedPages = findPagesByEntity(entityName);
           if (relatedPages.size() < 2) return List.of();
   
           // 2. 提取各页面中关于该实体的陈述
           List<String> statements = relatedPages.stream()
               .map(page -> extractEntityStatements(page.getContent(), entityName))
               .filter(StringUtils::hasText)
               .collect(Collectors.toList());
   
           // 3. 用 LLM 判断是否存在冲突
           String prompt = String.format("""
               以下是来自不同文档中关于"%s"的陈述，请判断是否存在矛盾：
               
               %s
               
               输出 JSON：
               {
                 "has_conflict": true/false,
                 "conflict_description": "描述冲突点",
                 "recommended_resolution": "建议采用哪个说法及原因"
               }
               """,
               entityName,
               IntStream.range(0, statements.size())
                   .mapToObj(i -> "陈述" + (i+1) + "：" + statements.get(i))
                   .collect(Collectors.joining("\n\n"))
           );
   
           ConflictAnalysis analysis = parseAnalysis(llm.generate(prompt));
   
           if (analysis.isHasConflict()) {
               return List.of(ConflictReport.builder()
                   .entityName(entityName)
                   .affectedPages(relatedPages.stream().map(WikiPage::getSlug).collect(Collectors.toList()))
                   .description(analysis.getConflictDescription())
                   .recommendation(analysis.getRecommendedResolution())
                   .severity(Severity.MEDIUM)
                   .build());
           }
   
           return List.of();
       }
   }
   ```

   ### 4.3 Quartz 定时任务调度

   ```java
   // hub-lint/scheduler/LintSchedulerConfig.java
   
   @Configuration
   public class LintSchedulerConfig {
   
       @Bean
       public JobDetail orphanScanJob() {
           return JobBuilder.newJob(OrphanScanJob.class)
               .withIdentity("orphan-scan", "lint")
               .storeDurably()
               .build();
       }
   
       @Bean
       public Trigger orphanScanTrigger(JobDetail orphanScanJob) {
           return TriggerBuilder.newTrigger()
               .forJob(orphanScanJob)
               .withIdentity("orphan-scan-trigger", "lint")
               .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?"))  // 每日凌晨 2 点
               .build();
       }
   
       @Bean
       public JobDetail stalenessCheckJob() {
           return JobBuilder.newJob(StalenessCheckJob.class)
               .withIdentity("staleness-check", "lint")
               .storeDurably()
               .build();
       }
   
       @Bean
       public Trigger stalenessCheckTrigger(JobDetail stalenessCheckJob) {
           return TriggerBuilder.newTrigger()
               .forJob(stalenessCheckJob)
               .withIdentity("staleness-trigger", "lint")
               .withSchedule(CronScheduleBuilder.weeklyOnDayAndHourAndMinute(
                   DateBuilder.MONDAY, 3, 0))  // 每周一凌晨 3 点
               .build();
       }
   }
   
   // 过期内容检测 Job
   @Component
   public class StalenessCheckJob implements Job {
   
       private static final int STALE_DAYS = 180;  // 6 个月未更新视为过期
   
       @Override
       public void execute(JobExecutionContext context) {
           List<WikiPage> stalePages = wikiRepo.findByUpdatedAtBefore(
               LocalDateTime.now().minusDays(STALE_DAYS)
           );
   
           stalePages.forEach(page -> {
               page.addTag("⚠️ 待更新");
               page.setQualityScore(page.getQualityScore() * 0.8);  // 质量评分衰减
               wikiRepo.save(page);
           });
   
           notifier.sendStalenessReport(stalePages.size());
       }
   }
   ```

   ------

   ## Phase 5：混合检索引擎（Query）

   **目标**：融合向量、图谱、全文三路检索，RRF 重排后生成高质量答案，新结论写回 Wiki。
    **周期**：第 6-7 周
    **产出**：查询 P99 延迟 < 800ms，回答引用准确率 > 85%

   ### 5.1 混合检索编排器

   ```java
   // hub-query/hybrid/HybridSearchOrchestrator.java
   
   @Service
   @Slf4j
   public class HybridSearchOrchestrator {
   
       private final MilvusVectorAdapter vectorAdapter;
       private final ElasticsearchAdapter esAdapter;
       private final Neo4jGraphSearcher graphSearcher;
       private final EmbeddingModel embeddingModel;
       private final RrfReranker rrfReranker;
   
       /**
        * 三路并行检索 + RRF 融合
        */
       public List<RankedResult> search(String query, SearchOptions options) {
           // 生成查询向量
           float[] queryEmbedding = embeddingModel.embed(query).content().vector();
   
           // 三路并行检索（CompletableFuture）
           CompletableFuture<List<VectorSearchResult>> vectorFuture =
               CompletableFuture.supplyAsync(() ->
                   vectorAdapter.search(queryEmbedding, options.getTopK(), options.getFilters()));
   
           CompletableFuture<List<EsSearchResult>> fullTextFuture =
               CompletableFuture.supplyAsync(() -> {
                   try { return esAdapter.fullTextSearch(query, options.getTopK()); }
                   catch (IOException e) { return List.of(); }
               });
   
           CompletableFuture<List<GraphSearchResult>> graphFuture =
               CompletableFuture.supplyAsync(() ->
                   graphSearcher.searchByEntityAndRelation(query, options.getTopK()));
   
           // 等待所有检索完成
           CompletableFuture.allOf(vectorFuture, fullTextFuture, graphFuture).join();
   
           // RRF 融合排序
           return rrfReranker.fuse(
               vectorFuture.join(),
               fullTextFuture.join(),
               graphFuture.join(),
               options.getTopN()
           );
       }
   }
   ```

   ### 5.2 RRF 融合排序器

   ```java
   // hub-query/rerank/RrfReranker.java
   
   @Component
   public class RrfReranker {
   
       // RRF 常数 k（通常取 60）
       private static final int K = 60;
   
       // 各路权重（可配置）
       private static final double VECTOR_WEIGHT  = 0.5;
       private static final double FULLTEXT_WEIGHT = 0.3;
       private static final double GRAPH_WEIGHT   = 0.2;
   
       public List<RankedResult> fuse(
           List<VectorSearchResult>  vectorResults,
           List<EsSearchResult>      ftResults,
           List<GraphSearchResult>   graphResults,
           int topN
       ) {
           Map<String, Double> scoreMap = new HashMap<>();
   
           // 向量检索 RRF 贡献
           for (int i = 0; i < vectorResults.size(); i++) {
               String slug = vectorResults.get(i).getSlug();
               scoreMap.merge(slug, VECTOR_WEIGHT / (K + i + 1), Double::sum);
           }
   
           // 全文检索 RRF 贡献
           for (int i = 0; i < ftResults.size(); i++) {
               String slug = ftResults.get(i).getSlug();
               scoreMap.merge(slug, FULLTEXT_WEIGHT / (K + i + 1), Double::sum);
           }
   
           // 图谱检索 RRF 贡献
           for (int i = 0; i < graphResults.size(); i++) {
               String slug = graphResults.get(i).getSlug();
               scoreMap.merge(slug, GRAPH_WEIGHT / (K + i + 1), Double::sum);
           }
   
           // 按 RRF 分数降序排序，取 top-N
           return scoreMap.entrySet().stream()
               .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
               .limit(topN)
               .map(e -> RankedResult.builder()
                   .slug(e.getKey())
                   .rrfScore(e.getValue())
                   .sources(determineSources(e.getKey(), vectorResults, ftResults, graphResults))
                   .build())
               .collect(Collectors.toList());
       }
   }
   ```

   ### 5.3 答案生成器（含写回机制）

   ```java
   // hub-query/answer/AnswerGenerator.java
   
   @Service
   @Slf4j
   public class AnswerGenerator {
   
       private final HybridSearchOrchestrator searchOrchestrator;
       private final WikiPageRepository wikiRepo;
       private final WikiCompileEngine compileEngine;
       private final ChatLanguageModel llm;
   
       public AnswerResponse answer(String question, AnswerOptions options) {
           long start = System.currentTimeMillis();
   
           // 1. 混合检索
           List<RankedResult> candidates = searchOrchestrator.search(question, SearchOptions.defaults());
   
           // 2. 加载 Wiki 页面内容
           List<WikiPage> contextPages = candidates.stream()
               .limit(5)
               .map(r -> wikiRepo.findBySlug(r.getSlug()))
               .filter(Optional::isPresent)
               .map(Optional::get)
               .collect(Collectors.toList());
   
           // 3. 构造 RAG Prompt
           String context = contextPages.stream()
               .map(p -> String.format("## %s\n%s", p.getTitle(), p.getSummary()))
               .collect(Collectors.joining("\n\n"));
   
           String prompt = String.format("""
               基于以下知识库内容回答问题。
               
               ## 相关知识
               %s
               
               ## 问题
               %s
               
               ## 要求
               - 仅基于提供的知识库内容作答，不要编造信息
               - 若知识库中没有相关信息，明确说明
               - 回答末尾列出引用来源（Wiki 页面标题）
               - 若你的回答包含了原知识库未收录的新结论，在 [NEW_INSIGHT] 标签中单独列出
               """, context, question);
   
           String rawAnswer = llm.generate(prompt);
   
           // 4. 解析是否有新结论，触发写回（Writeback is mandatory）
           extractAndWritebackInsights(rawAnswer, contextPages);
   
           long latency = System.currentTimeMillis() - start;
           log.info("Answer generated in {}ms for: {}", latency, question.substring(0, Math.min(50, question.length())));
   
           return AnswerResponse.builder()
               .answer(cleanAnswer(rawAnswer))
               .sourceSlugs(contextPages.stream().map(WikiPage::getSlug).collect(Collectors.toList()))
               .latencyMs(latency)
               .build();
       }
   
       /**
        * 提取新结论并异步写回 Wiki（Karpathy 核心原则：写回是强制的）
        */
       @Async
       protected void extractAndWritebackInsights(String rawAnswer, List<WikiPage> sourcedPages) {
           String insightPattern = "\\[NEW_INSIGHT\\](.*?)\\[/NEW_INSIGHT\\]";
           Matcher matcher = Pattern.compile(insightPattern, Pattern.DOTALL).matcher(rawAnswer);
   
           if (matcher.find()) {
               String newInsight = matcher.group(1).trim();
               if (StringUtils.hasText(newInsight)) {
                   log.info("New insight detected, triggering writeback: {}", newInsight.substring(0, Math.min(100, newInsight.length())));
                   // 将新结论作为增量内容推入编译队列
                   CompileTask task = CompileTask.fromInsight(newInsight, sourcedPages);
                   kafkaTemplate.send("document-compile-queue", task);
               }
           }
       }
   }
   ```

   ### 5.4 图谱增强检索

   ```java
   // hub-query/hybrid/Neo4jGraphSearcher.java
   
   @Component
   public class Neo4jGraphSearcher {
   
       private final Driver neo4jDriver;
   
       /**
        * 基于实体识别的图谱路径检索
        * 解决知识孤岛：通过关系链找到直接检索无法发现的页面
        */
       public List<GraphSearchResult> searchByEntityAndRelation(String query, int topK) {
           try (Session session = neo4jDriver.session()) {
               // 从查询中提取潜在实体名（简单实现：分词后精确匹配）
               List<String> entityCandidates = extractEntityCandidates(query);
   
               if (entityCandidates.isEmpty()) return List.of();
   
               // 通过实体关系图找相关 Wiki 页面（2 跳以内）
               Result result = session.run("""
                   MATCH (e:Entity)
                   WHERE e.name IN $names
                   MATCH (e)-[:MENTIONED_IN]->(w:WikiPage)
                   WITH w, count(e) AS entityMatches
                   OPTIONAL MATCH (w)-[:RELATED_TO]-(w2:WikiPage)
                   RETURN DISTINCT w.slug AS slug, w.title AS title, 
                          entityMatches, w.quality_score AS score
                   UNION
                   MATCH (e:Entity)
                   WHERE e.name IN $names
                   MATCH (e)-[r]->(e2:Entity)-[:MENTIONED_IN]->(w:WikiPage)
                   RETURN DISTINCT w.slug AS slug, w.title AS title,
                          1 AS entityMatches, w.quality_score AS score
                   ORDER BY entityMatches DESC, score DESC
                   LIMIT $limit
                   """,
                   Map.of("names", entityCandidates, "limit", topK)
               );
   
               return result.list(record -> new GraphSearchResult(
                   record.get("slug").asString(),
                   record.get("title").asString(),
                   record.get("score").asDouble(0.0)
               ));
           }
       }
   }
   ```

   ------

   ## Phase 6：应用与接入层

   **目标**：提供 REST、GraphQL、MCP 多种接入方式，支持企业 SSO，前端知识地图。
    **周期**：第 7-8 周
    **产出**：可上线的应用服务，支持权限管控，知识地图可视化

   ### 6.1 REST API 控制器

   ```java
   // hub-api/rest/KnowledgeQueryController.java
   
   @RestController
   @RequestMapping("/api/v1/knowledge")
   @Slf4j
   public class KnowledgeQueryController {
   
       private final AnswerGenerator answerGenerator;
       private final WikiPageRepository wikiRepo;
       private final HybridSearchOrchestrator searchOrchestrator;
   
       // 智能问答
       @PostMapping("/ask")
       public ResponseEntity<AnswerResponse> ask(
           @RequestBody @Valid AskRequest request,
           @AuthenticationPrincipal UserDetails user
       ) {
           log.info("User {} asked: {}", user.getUsername(), request.getQuestion());
           AnswerResponse response = answerGenerator.answer(
               request.getQuestion(),
               AnswerOptions.from(request)
           );
           return ResponseEntity.ok(response);
       }
   
       // 搜索 Wiki 页面
       @GetMapping("/search")
       public ResponseEntity<SearchResponse> search(
           @RequestParam String q,
           @RequestParam(defaultValue = "10") int size
       ) {
           List<RankedResult> results = searchOrchestrator.search(q, SearchOptions.withSize(size));
           return ResponseEntity.ok(SearchResponse.from(results));
       }
   
       // 获取单个 Wiki 页面
       @GetMapping("/wiki/{slug}")
       public ResponseEntity<WikiPageDto> getWikiPage(@PathVariable String slug) {
           return wikiRepo.findBySlug(slug)
               .map(WikiPageDto::from)
               .map(ResponseEntity::ok)
               .orElse(ResponseEntity.notFound().build());
       }
   
       // 手动触发摄取（管理员）
       @PostMapping("/admin/ingest")
       @PreAuthorize("hasRole('ADMIN')")
       public ResponseEntity<JobResponse> triggerIngest(@RequestBody IngestRequest request) {
           JobExecution execution = jobLauncher.run(documentIngestJob,
               new JobParametersBuilder()
                   .addString("source", request.getSource())
                   .addLong("time", System.currentTimeMillis())
                   .toJobParameters()
           );
           return ResponseEntity.accepted().body(JobResponse.from(execution));
       }
   
       // 知识图谱数据（供前端可视化）
       @GetMapping("/graph")
       public ResponseEntity<GraphData> getGraph(
           @RequestParam(required = false) String centerSlug,
           @RequestParam(defaultValue = "2") int depth
       ) {
           GraphData data = graphSearcher.getSubgraph(centerSlug, depth);
           return ResponseEntity.ok(data);
       }
   }
   ```

   ### 6.2 Spring Security 配置

   ```java
   // hub-api/security/SecurityConfig.java
   
   @Configuration
   @EnableWebSecurity
   @EnableMethodSecurity
   public class SecurityConfig {
   
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           return http
               .csrf(csrf -> csrf.disable())
               .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
               .authorizeHttpRequests(auth -> auth
                   .requestMatchers("/actuator/health").permitAll()
                   .requestMatchers("/api/v1/knowledge/admin/**").hasRole("ADMIN")
                   .requestMatchers("/api/v1/**").authenticated()
                   .anyRequest().authenticated()
               )
               // JWT 验证
               .oauth2ResourceServer(oauth2 -> oauth2
                   .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
               )
               // 企业 SSO（LDAP / Azure AD / Okta）
               .oauth2Login(oauth2 -> oauth2
                   .defaultSuccessUrl("/api/v1/auth/callback")
               )
               .build();
       }
   }
   ```

   ### 6.3 MCP Tool 接口（供 Claude 等 AI 工具调用）

   ```java
   // hub-api/mcp/McpToolController.java
   
   /**
    * MCP (Model Context Protocol) 接口
    * 允许 Claude Desktop、Claude Code 等工具直接调用企业知识库
    */
   @RestController
   @RequestMapping("/mcp")
   public class McpToolController {
   
       @PostMapping("/tools/knowledge_search")
       public McpToolResult knowledgeSearch(@RequestBody McpToolCall call) {
           String query = call.getParam("query");
           List<RankedResult> results = searchOrchestrator.search(query, SearchOptions.defaults());
   
           String content = results.stream()
               .limit(3)
               .map(r -> {
                   WikiPage page = wikiRepo.findBySlug(r.getSlug()).orElseThrow();
                   return String.format("### %s\n%s", page.getTitle(), page.getSummary());
               })
               .collect(Collectors.joining("\n\n"));
   
           return McpToolResult.success(content);
       }
   
       // MCP 工具描述（供 AI 发现）
       @GetMapping("/tools")
       public List<McpToolDescription> listTools() {
           return List.of(
               McpToolDescription.builder()
                   .name("knowledge_search")
                   .description("搜索企业知识库，返回相关 Wiki 页面摘要")
                   .parameters(Map.of("query", "搜索关键词或问题"))
                   .build(),
               McpToolDescription.builder()
                   .name("get_wiki_page")
                   .description("获取指定 Wiki 页面的完整内容")
                   .parameters(Map.of("slug", "Wiki 页面的 slug 标识"))
                   .build()
           );
       }
   }
   ```

   ------

   ## Phase 7：运维与可观测性

   **目标**：全链路监控、告警、日志聚合，保障 SLA。
    **周期**：第 8-9 周
    **产出**：Grafana 监控大盘，PagerDuty 告警，结构化日志

   ### 7.1 关键指标埋点

   ```java
   // hub-common/metrics/KnowledgeHubMetrics.java
   
   @Component
   public class KnowledgeHubMetrics {
   
       private final MeterRegistry registry;
   
       // 编译引擎指标
       public Timer compileTimer() {
           return Timer.builder("compile.duration")
               .description("Wiki 页面编译耗时")
               .register(registry);
       }
   
       public Counter compileSuccessCounter() {
           return Counter.builder("compile.total")
               .tag("status", "success")
               .register(registry);
       }
   
       // 检索指标
       public Timer queryTimer() {
           return Timer.builder("query.duration")
               .description("混合检索总耗时（含 LLM 生成）")
               .register(registry);
       }
   
       // 孤岛指标（核心业务指标）
       public Gauge orphanRateGauge(Supplier<Number> supplier) {
           return Gauge.builder("knowledge.orphan_rate", supplier)
               .description("知识孤岛比率（目标 < 5%）")
               .register(registry);
       }
   
       // Wiki 质量分布
       public DistributionSummary qualityScoreSummary() {
           return DistributionSummary.builder("wiki.quality_score")
               .description("Wiki 页面质量评分分布")
               .publishPercentiles(0.5, 0.9, 0.99)
               .register(registry);
       }
   }
   ```

   ### 7.2 Prometheus + Grafana 配置

   ```yaml
   # prometheus.yml
   scrape_configs:
     - job_name: 'knowledge-hub'
       metrics_path: '/actuator/prometheus'
       static_configs:
         - targets: ['hub-api:8080']
       scrape_interval: 15s
   
   # Grafana 告警规则
   groups:
     - name: knowledge-hub-alerts
       rules:
         - alert: HighOrphanRate
           expr: knowledge_orphan_rate > 0.1
           for: 1h
           labels:
             severity: warning
           annotations:
             summary: "知识孤岛率超过 10%"
   
         - alert: CompileQueueBacklog
           expr: kafka_consumer_lag{topic="document-compile-queue"} > 10000
           for: 30m
           labels:
             severity: critical
           annotations:
             summary: "编译队列积压超过 1 万条"
   
         - alert: QueryLatencyHigh
           expr: histogram_quantile(0.99, rate(query_duration_bucket[5m])) > 2
           for: 10m
           labels:
             severity: warning
           annotations:
             summary: "查询 P99 延迟超过 2 秒"
   ```

   ### 7.3 应用配置（application.yml）

   ```yaml
   # application.yml
   
   spring:
     application:
       name: knowledge-hub
     datasource:
       url: jdbc:postgresql://localhost:5432/knowledge_hub
       username: khub
       password: ${DB_PASSWORD}
     kafka:
       bootstrap-servers: localhost:9092
       consumer:
         group-id: knowledge-hub
         auto-offset-reset: earliest
         max-poll-records: 100
       producer:
         acks: all
         retries: 3
   
   # LLM 配置
   llm:
     provider: openai                    # openai | anthropic | local
     openai:
       api-key: ${OPENAI_API_KEY}
       model: gpt-4o-mini               # 编译用小模型降成本
     compile:
       concurrency: 8                    # 并发编译线程数
       batch-size: 5                     # 每批处理文档数
       retry-max: 3
   
   # Milvus
   milvus:
     host: localhost
     port: 19530
   
   # Neo4j
   neo4j:
     uri: bolt://localhost:7687
     authentication:
       username: neo4j
       password: ${NEO4J_PASSWORD}
   
   # MinIO
   minio:
     endpoint: http://localhost:9000
     access-key: ${MINIO_USER}
     secret-key: ${MINIO_PASSWORD}
     bucket-raw: raw-documents
     bucket-wiki: wiki-pages
   
   # Lint 调度
   lint:
     orphan-scan:
       cron: "0 0 2 * * ?"           # 每日凌晨 2 点
       auto-fix-limit: 20            # 每次自动修复上限
     staleness:
       threshold-days: 180           # 6 个月过期
   
   # 性能
   server:
     tomcat:
       threads:
         max: 200
     compression:
       enabled: true
   
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,prometheus
     metrics:
       export:
         prometheus:
           enabled: true
   ```

   ------

   ## 规模估算与性能基准

   ### 存储规模（10w 文档）

   | 存储            | 数据量估算                          | 规格建议               |
   | --------------- | ----------------------------------- | ---------------------- |
   | MinIO 原始文档  | ~500GB（平均 5MB/文档）             | 3 副本，可扩展         |
   | Milvus 向量索引 | ~15GB（3w Wiki 页面 × 1536维 × 4B） | 32GB RAM，GPU 可选     |
   | Neo4j 图数据库  | ~5GB（实体 ~50w，关系 ~200w）       | 16GB RAM               |
   | Elasticsearch   | ~30GB（含倒排索引）                 | 3节点，每节点 32GB RAM |
   | PostgreSQL      | ~10GB（元数据 + pgvector 备用）     | 16GB RAM + SSD         |

   ### 性能基准目标

   | 指标           | 目标值                | 监控方式             |
   | -------------- | --------------------- | -------------------- |
   | 文档摄取速度   | ≥ 500 文档/分钟       | Spring Batch 进度    |
   | Wiki 编译速度  | ≥ 200 页面/小时       | Kafka 消费延迟       |
   | 首次全量编译   | < 72 小时（10w 文档） | Batch Job 历时       |
   | 增量日常编译   | < 30 分钟             | 变更文档数量         |
   | 查询 P50 延迟  | < 300ms               | Prometheus histogram |
   | 查询 P99 延迟  | < 800ms               | Prometheus histogram |
   | 知识孤岛率     | < 5%                  | 每日 Lint 报告       |
   | 回答引用准确率 | > 85%                 | 人工抽样评估         |
   | 系统可用性     | > 99.5%               | Uptime 监控          |

   ### LLM Token 成本估算

   | 场景                     | Token 消耗   | 成本估算（gpt-4o-mini） |
   | ------------------------ | ------------ | ----------------------- |
   | 单文档编译               | ~2000 tokens | ~¥0.01                  |
   | 10w 文档首次编译         | ~2亿 tokens  | ~¥1000                  |
   | 日常增量（1000 文档/天） | ~200w tokens | ~¥10/天                 |
   | 单次问答                 | ~1500 tokens | ~¥0.008                 |

   > **降本策略**：编译使用 `gpt-4o-mini` 或本地 `Qwen2.5-14b`，仅在答案生成时使用高质量模型。

   ------

   ## 分期交付计划

   ```
   Phase 0  基础设施准备        ████░░░░░░░░  第1周
   Phase 1  文档摄取管道        ████████░░░░  第2-3周
   Phase 2  LLM编译引擎         ████████████  第3-5周（并行）
   Phase 3  知识存储层          ████████░░░░  第3-4周（并行）
   Phase 4  知识孤岛治理        ████████░░░░  第5-6周
   Phase 5  混合检索引擎        ████████░░░░  第6-7周
   Phase 6  应用与接入层        ████████░░░░  第7-8周
   Phase 7  运维可观测性        ████████░░░░  第8-9周
   ─────────────────────────────────────────────────
   MVP上线（基础问答）                         第5周末
   V2上线（图谱+孤岛治理）                    第7周末
   V3上线（全功能+监控）                      第9周末
   ```

   ### MVP 最小功能集（第5周末上线）

   - [x] 支持 PDF/Word/HTML 摄取
   - [x] LLM 编译生成 Wiki 页面
   - [x] pgvector 向量检索
   - [x] 基础问答 REST API
   - [x] Spring Security + JWT

   ### V2 功能集（第7周末上线）

   - [x] Neo4j 知识图谱
   - [x] 混合检索（向量 + 全文 + 图谱）
   - [x] 知识孤岛 Lint 扫描
   - [x] Kafka 摄取管道
   - [x] 基础监控大盘

   ### V3 功能集（第9周末上线）

   - [x] MCP Tool 接口
   - [x] 知识地图可视化
   - [x] 企业 SSO 集成
   - [x] 完整告警体系
   - [x] 多 LLM 提供商切换
   - [x] K8s Helm Chart 部署

   ------

   ## 附录：关键设计决策

   ### 为什么选 Milvus 而不是纯 pgvector？

   10w 文档场景下，Wiki 页面约 3-5w，pgvector 的 IVFFlat 索引在此规模已经够用。但考虑到后续增长和多租户，Milvus 提供更好的水平扩展能力。**建议 MVP 阶段用 pgvector 快速交付，V2 阶段迁移 Milvus**。

   ### 为什么 Neo4j 是打通知识孤岛的关键？

   传统 RAG 文档之间没有显式关系，两个相关文档永远是孤岛。Neo4j 强制 LLM 在编译时输出实体关系，并通过图遍历在检索时找到"语义上距离较近但关键词差异大"的内容——这是向量检索无法做到的。

   ### Writeback 为什么是强制的？

   这是 Karpathy 思想的核心：**每次问答产生的新结论必须写回 Wiki**，否则下次相同问题仍要重新推理，AI 永远不会"变聪明"。写回机制将系统从"工具"升级为"会学习的知识库"。

   ### 如何控制 LLM 幻觉？

   1. 编译阶段：低温度（0.1）+ JSON 强制输出格式
   2. 查询阶段：Prompt 中明确限制"仅基于知识库内容"
   3. Lint 阶段：定期用 LLM 交叉验证同实体不同页面的陈述是否矛盾
   4. 质量评分：质量评分低于阈值（0.5）的页面在检索时降权