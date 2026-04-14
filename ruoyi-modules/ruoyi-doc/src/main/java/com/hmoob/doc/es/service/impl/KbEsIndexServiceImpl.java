package com.hmoob.doc.es.service.impl;

import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.hmoob.doc.es.constant.KbEsConstant;
import com.hmoob.doc.es.document.KbDocDocument;
import com.hmoob.doc.es.service.IKbEsIndexService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * KB ES索引管理服务实现
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(prefix = "kb.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KbEsIndexServiceImpl implements IKbEsIndexService {

    private final ElasticsearchClient esClient;

    @Override
    public void indexDocument(KbDocDocument document) {
        try {
            IndexRequest<KbDocDocument> request = IndexRequest.of(i -> i
                .index(KbEsConstant.DOC_INDEX_NAME)
                .id(String.valueOf(document.getDocId()))
                .document(document)
                .refresh(Refresh.True)
            );
            esClient.index(request);
            log.info("索引文档成功: docId={}", document.getDocId());
        } catch (IOException e) {
            log.error("索引文档失败: docId={}, error={}", document.getDocId(), e.getMessage(), e);
            throw new RuntimeException("索引文档失败", e);
        }
    }

    @Override
    public void batchIndexDocuments(List<KbDocDocument> documents) {
        if (CollUtil.isEmpty(documents)) {
            return;
        }

        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            bulkBuilder.refresh(Refresh.True);

            for (KbDocDocument doc : documents) {
                bulkBuilder.operations(op -> op.index(idx -> idx
                    .index(KbEsConstant.DOC_INDEX_NAME)
                    .id(String.valueOf(doc.getDocId()))
                    .document(doc)
                ));
            }

            BulkResponse response = esClient.bulk(bulkBuilder.build());

            if (response.errors()) {
                List<String> errors = response.items().stream()
                    .filter(item -> item.error() != null)
                    .map(item -> "docId=" + item.id() + ", error=" + item.error().reason())
                    .collect(Collectors.toList());
                log.error("批量索引部分失败: {}", errors);
                throw new RuntimeException("批量索引部分失败: " + errors.size() + "条记录失败");
            }

            log.info("批量索引文档成功: count={}", documents.size());
        } catch (IOException e) {
            log.error("批量索引文档失败: error={}", e.getMessage(), e);
            throw new RuntimeException("批量索引文档失败", e);
        }
    }

    @Override
    public void updateDocument(KbDocDocument document) {
        try {
            UpdateRequest<KbDocDocument, KbDocDocument> request = UpdateRequest.of(u -> u
                .index(KbEsConstant.DOC_INDEX_NAME)
                .id(String.valueOf(document.getDocId()))
                .doc(document)
                .refresh(Refresh.True)
            );
            esClient.update(request, KbDocDocument.class);
            log.info("更新文档索引成功: docId={}", document.getDocId());
        } catch (IOException e) {
            log.error("更新文档索引失败: docId={}, error={}", document.getDocId(), e.getMessage(), e);
            throw new RuntimeException("更新文档索引失败", e);
        }
    }

    @Override
    public void deleteDocument(Long docId) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d
                .index(KbEsConstant.DOC_INDEX_NAME)
                .id(String.valueOf(docId))
                .refresh(Refresh.True)
            );
            esClient.delete(request);
            log.info("删除文档索引成功: docId={}", docId);
        } catch (IOException e) {
            log.error("删除文档索引失败: docId={}, error={}", docId, e.getMessage(), e);
            throw new RuntimeException("删除文档索引失败", e);
        }
    }

    @Override
    public void batchDeleteDocuments(List<Long> docIds) {
        if (CollUtil.isEmpty(docIds)) {
            return;
        }

        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            bulkBuilder.refresh(Refresh.True);

            for (Long docId : docIds) {
                bulkBuilder.operations(op -> op.delete(del -> del
                    .index(KbEsConstant.DOC_INDEX_NAME)
                    .id(String.valueOf(docId))
                ));
            }

            BulkResponse response = esClient.bulk(bulkBuilder.build());

            if (response.errors()) {
                List<String> errors = response.items().stream()
                    .filter(item -> item.error() != null)
                    .map(item -> "docId=" + item.id() + ", error=" + item.error().reason())
                    .collect(Collectors.toList());
                log.error("批量删除部分失败: {}", errors);
                throw new RuntimeException("批量删除部分失败: " + errors.size() + "条记录失败");
            }

            log.info("批量删除文档索引成功: count={}", docIds.size());
        } catch (IOException e) {
            log.error("批量删除文档索引失败: error={}", e.getMessage(), e);
            throw new RuntimeException("批量删除文档索引失败", e);
        }
    }

    @Override
    public void rebuildIndex() {
        // 该方法需要配合数据库查询来实现,此处只提供框架
        // 实际实现需要在Controller层或业务层调用Service查询数据库后进行重建
        log.info("开始重建索引...");

        // 删除旧索引
        if (indexExists()) {
            deleteIndex();
        }

        // 创建新索引
        createIndex();

        log.info("索引重建框架完成,请调用业务层批量导入数据");
    }

    @Override
    public boolean indexExists() {
        try {
            ExistsRequest request = ExistsRequest.of(e -> e
                .index(KbEsConstant.DOC_INDEX_NAME)
            );
            return esClient.indices().exists(request).value();
        } catch (IOException e) {
            log.error("检查索引是否存在失败: error={}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void createIndex() {
        try {
            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                .index(KbEsConstant.DOC_INDEX_NAME)
                // 索引设置和映射由ES模板文件定义,此处不重复定义
            );
            esClient.indices().create(request);
            log.info("创建索引成功: {}", KbEsConstant.DOC_INDEX_NAME);
        } catch (IOException e) {
            log.error("创建索引失败: error={}", e.getMessage(), e);
            throw new RuntimeException("创建索引失败", e);
        }
    }

    @Override
    public void deleteIndex() {
        try {
            DeleteIndexRequest request = DeleteIndexRequest.of(d -> d
                .index(KbEsConstant.DOC_INDEX_NAME)
            );
            esClient.indices().delete(request);
            log.info("删除索引成功: {}", KbEsConstant.DOC_INDEX_NAME);
        } catch (IOException e) {
            log.error("删除索引失败: error={}", e.getMessage(), e);
            throw new RuntimeException("删除索引失败", e);
        }
    }

    @Override
    public void refreshIndex() {
        try {
            RefreshRequest request = RefreshRequest.of(r -> r
                .index(KbEsConstant.DOC_INDEX_NAME)
            );
            esClient.indices().refresh(request);
            log.info("刷新索引成功: {}", KbEsConstant.DOC_INDEX_NAME);
        } catch (IOException e) {
            log.error("刷新索引失败: error={}", e.getMessage(), e);
        }
    }

    @Override
    public long getDocumentCount() {
        try {
            CountRequest request = CountRequest.of(c -> c
                .index(KbEsConstant.DOC_INDEX_NAME)
            );
            CountResponse response = esClient.count(request);
            return response.count();
        } catch (IOException e) {
            log.error("获取文档数量失败: error={}", e.getMessage(), e);
            return 0;
        }
    }
}