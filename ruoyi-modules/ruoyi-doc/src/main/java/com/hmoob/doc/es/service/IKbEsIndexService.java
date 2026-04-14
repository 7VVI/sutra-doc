package com.hmoob.doc.es.service;

import com.hmoob.doc.es.document.KbDocDocument;

import java.util.List;

/**
 * KB ES索引管理服务接口
 *
 * @author hmoob
 */
public interface IKbEsIndexService {

    /**
     * 索引单个文档到ES
     *
     * @param document 文档对象
     */
    void indexDocument(KbDocDocument document);

    /**
     * 批量索引文档
     *
     * @param documents 文档列表
     */
    void batchIndexDocuments(List<KbDocDocument> documents);

    /**
     * 更新文档索引
     *
     * @param document 文档对象
     */
    void updateDocument(KbDocDocument document);

    /**
     * 删除文档索引
     *
     * @param docId 文档ID
     */
    void deleteDocument(Long docId);

    /**
     * 批量删除文档索引
     *
     * @param docIds 文档ID列表
     */
    void batchDeleteDocuments(List<Long> docIds);

    /**
     * 从数据库重建整个索引
     */
    void rebuildIndex();

    /**
     * 检查索引是否存在
     *
     * @return 是否存在
     */
    boolean indexExists();

    /**
     * 创建索引(使用模板)
     */
    void createIndex();

    /**
     * 删除索引
     */
    void deleteIndex();

    /**
     * 刷新索引
     */
    void refreshIndex();

    /**
     * 获取索引文档数量
     *
     * @return 文档数量
     */
    long getDocumentCount();
}