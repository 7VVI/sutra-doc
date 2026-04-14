package com.hmoob.doc.es.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.tenant.helper.TenantHelper;
import com.hmoob.doc.es.constant.KbEsConstant;
import com.hmoob.doc.es.document.KbDocDocument;
import com.hmoob.doc.es.document.KbSearchLogDocument;
import com.hmoob.doc.es.document.KbSearchRequest;
import com.hmoob.doc.es.document.KbSearchResultVo;
import com.hmoob.doc.es.service.IKbEsSearchService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * KB ES搜索服务实现
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(prefix = "kb.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KbEsSearchServiceImpl implements IKbEsSearchService {

    private final ElasticsearchClient esClient;

    @Override
    public TableDataInfo<KbSearchResultVo> search(KbSearchRequest request) {
        try {
            // 构建基础查询
            Query query = buildSearchQuery(request);

            // 构建排序
            List<SortOptions> sortOptions = buildSortOptions(request);

            // 获取分页参数
            int from = request.getOffset();
            int size = request.getPageSize() != null ? request.getPageSize() : 10;

            // 执行搜索
            SearchResponse<KbDocDocument> response = esClient.search(s -> s
                .index(KbEsConstant.DOC_INDEX_NAME)
                .query(query)
                .highlight(h -> h
                    .fields("content", f -> f
                        .preTags("<em>")
                        .postTags("</em>")
                        .numberOfFragments(3)
                        .fragmentSize(200)
                    )
                    .fields("docTitle", f -> f
                        .preTags("<em>")
                        .postTags("</em>")
                    )
                    .fields("docName", f -> f
                        .preTags("<em>")
                        .postTags("</em>")
                    )
                )
                .sort(sortOptions)
                .from(from)
                .size(size)
            , KbDocDocument.class);

            // 转换结果
            List<KbSearchResultVo> results = convertToResultVo(response.hits().hits());

            return new TableDataInfo<>(results, response.hits().total().value());
        } catch (Exception e) {
            log.error("ES搜索失败: {}", e.getMessage(), e);
            return TableDataInfo.build();
        }
    }

    @Override
    public TableDataInfo<KbSearchResultVo> searchWithFilters(KbSearchRequest request) {
        try {
            // 构建带过滤条件的查询
            Query query = buildFilteredQuery(request);

            // 构建排序
            List<SortOptions> sortOptions = buildSortOptions(request);

            // 获取分页参数
            int from = request.getOffset();
            int size = request.getPageSize() != null ? request.getPageSize() : 10;

            // 执行搜索
            SearchResponse<KbDocDocument> response = esClient.search(s -> s
                .index(KbEsConstant.DOC_INDEX_NAME)
                .query(query)
                .highlight(h -> h
                    .fields("content", f -> f
                        .preTags("<em>")
                        .postTags("</em>")
                        .numberOfFragments(3)
                        .fragmentSize(200)
                    )
                    .fields("docTitle", f -> f
                        .preTags("<em>")
                        .postTags("</em>")
                    )
                )
                .sort(sortOptions)
                .from(from)
                .size(size)
            , KbDocDocument.class);

            // 转换结果
            List<KbSearchResultVo> results = convertToResultVo(response.hits().hits());

            return new TableDataInfo<>(results, response.hits().total().value());
        } catch (Exception e) {
            log.error("ES带条件搜索失败: {}", e.getMessage(), e);
            return TableDataInfo.build();
        }
    }

    @Override
    public List<String> getHotKeywords(Integer limit, Integer days) {
        try {
            // 确保参数有效,使用final变量
            final int finalLimit = (limit == null || limit < 1) ? 10 : limit;
            final int finalDays = (days == null || days < 1) ? 7 : days;

            // 使用聚合查询统计热门关键词
            TermsAggregation termsAgg = TermsAggregation.of(t -> t
                .field("keywordsMax")
                .size(finalLimit)
            );

            SearchResponse<KbSearchLogDocument> response = esClient.search(s -> s
                .index(KbEsConstant.SEARCH_LOG_INDEX_NAME)
                .size(0)
                .aggregations("hot_keywords", Aggregation.of(a -> a.terms(termsAgg)))
            , KbSearchLogDocument.class);

            // 提取聚合结果
            if (response.aggregations() != null && response.aggregations().get("hot_keywords") != null) {
                return response.aggregations().get("hot_keywords").sterms().buckets().array()
                    .stream()
                    .map(bucket -> bucket.key().stringValue())
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("获取热门关键词失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getSearchSuggestions(String prefix, Integer limit) {
        try {
            if (StringUtils.isBlank(prefix)) {
                return Collections.emptyList();
            }

            // 使用final变量
            final int finalLimit = (limit == null || limit < 1) ? 10 : limit;
            final String queryPrefix = prefix.toLowerCase() + "*";

            // 使用前缀匹配查询搜索建议
            Query query = Query.of(q -> q.wildcard(w -> w
                .field("keywords")
                .value(queryPrefix)
            ));

            SearchResponse<KbDocDocument> response = esClient.search(s -> s
                .index(KbEsConstant.DOC_INDEX_NAME)
                .query(query)
                .size(finalLimit)
            , KbDocDocument.class);

            return response.hits().hits().stream()
                .map(hit -> hit.source().getKeywords())
                .filter(StringUtils::isNotBlank)
                .distinct()
                .limit(finalLimit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取搜索建议失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public TableDataInfo<KbSearchResultVo> searchByFolder(Long folderId, KbSearchRequest request) {
        if (folderId == null) {
            return TableDataInfo.build();
        }

        KbSearchRequest folderRequest = new KbSearchRequest();
        folderRequest.setKeywords(request.getKeywords());
        folderRequest.setFolderIds(new Long[]{folderId});
        folderRequest.setPageNum(request.getPageNum());
        folderRequest.setPageSize(request.getPageSize());
        folderRequest.setSortField(request.getSortField());
        folderRequest.setSortOrder(request.getSortOrder());
        folderRequest.setTenantId(request.getTenantId());
        folderRequest.setPublishedOnly(request.getPublishedOnly());

        return searchWithFilters(folderRequest);
    }

    @Override
    public List<KbSearchResultVo> searchSimilarDocs(Long docId, Integer limit) {
        try {
            if (docId == null) {
                return Collections.emptyList();
            }

            // 使用final变量
            final int finalLimit = (limit == null || limit < 1) ? 5 : limit;

            // 先获取当前文档
            KbDocDocument sourceDoc = esClient.get(g -> g
                .index(KbEsConstant.DOC_INDEX_NAME)
                .id(String.valueOf(docId))
            , KbDocDocument.class).source();

            if (sourceDoc == null) {
                return Collections.emptyList();
            }

            // 获取源文档的关键信息用于查询
            final String sourceKeywords = sourceDoc.getKeywords();
            final String sourceTitle = sourceDoc.getDocTitle();
            final String sourceTenantId = sourceDoc.getTenantId();

            // 使用关键词和标题查找相似文档
            Query query = Query.of(q -> q.bool(b -> {
                // 相同关键词或相似标题
                if (StringUtils.isNotBlank(sourceKeywords)) {
                    b.should(s -> s.match(m -> m
                        .field("keywords")
                        .query(sourceKeywords)
                        .analyzer(KbEsConstant.ANALYZER_IK_SMART)
                    ));
                }
                if (StringUtils.isNotBlank(sourceTitle)) {
                    b.should(s -> s.match(m -> m
                        .field("docTitle")
                        .query(sourceTitle)
                        .analyzer(KbEsConstant.ANALYZER_IK_SMART)
                    ));
                }
                // 排除自身
                b.mustNot(mn -> mn.term(t -> t.field("docId").value(docId)));
                // 租户隔离
                if (StringUtils.isNotBlank(sourceTenantId)) {
                    b.must(m -> m.term(t -> t.field("tenantId").value(sourceTenantId)));
                }
                // 只查询已发布文档
                b.must(m -> m.term(t -> t.field("releaseFlag").value(1)));
                return b;
            }));

            SearchResponse<KbDocDocument> response = esClient.search(s -> s
                .index(KbEsConstant.DOC_INDEX_NAME)
                .query(query)
                .size(finalLimit)
            , KbDocDocument.class);

            return convertToResultVo(response.hits().hits());
        } catch (Exception e) {
            log.error("搜索相似文档失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建基础搜索查询
     */
    private Query buildSearchQuery(KbSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 关键词搜索 - 多字段匹配
        if (StringUtils.isNotBlank(request.getKeywords())) {
            final String keywords = request.getKeywords();
            Query multiMatchQuery = Query.of(q -> q.multiMatch(m -> m
                .fields("docName", "docTitle^" + KbEsConstant.TITLE_SEARCH_BOOST,
                    "content^" + KbEsConstant.CONTENT_SEARCH_BOOST,
                    "keywords^" + KbEsConstant.KEYWORD_SEARCH_BOOST)
                .query(keywords)
                .analyzer(KbEsConstant.ANALYZER_IK_SMART)
                .type(TextQueryType.BestFields)
            ));
            boolBuilder.must(multiMatchQuery);
        }

        // 租户隔离
        String tenantId = request.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            tenantId = TenantHelper.getTenantId();
        }
        if (StringUtils.isNotBlank(tenantId)) {
            final String finalTenantId = tenantId;
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("tenantId").value(finalTenantId))));
        }

        // 只搜索已发布文档
        if (request.getPublishedOnly() != null && request.getPublishedOnly()) {
            boolBuilder.must(Query.of(q -> q.term(t -> t.field("releaseFlag").value(1))));
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    /**
     * 构建带过滤条件的搜索查询
     */
    private Query buildFilteredQuery(KbSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 关键词搜索 - 多字段匹配
        if (StringUtils.isNotBlank(request.getKeywords())) {
            final String keywords = request.getKeywords();
            Query multiMatchQuery = Query.of(q -> q.multiMatch(m -> m
                .fields("docName", "docTitle^" + KbEsConstant.TITLE_SEARCH_BOOST,
                    "content^" + KbEsConstant.CONTENT_SEARCH_BOOST,
                    "keywords^" + KbEsConstant.KEYWORD_SEARCH_BOOST)
                .query(keywords)
                .analyzer(KbEsConstant.ANALYZER_IK_SMART)
                .type(TextQueryType.BestFields)
            ));
            boolBuilder.must(multiMatchQuery);
        }

        // 文件夹过滤
        if (request.getFolderIds() != null && request.getFolderIds().length > 0) {
            List<FieldValue> folderIdValues = Arrays.stream(request.getFolderIds())
                .map(FieldValue::of)
                .collect(Collectors.toList());
            boolBuilder.filter(Query.of(q -> q.terms(t -> t
                .field("folderId")
                .terms(tt -> tt.value(folderIdValues))
            )));
        }

        // 主题过滤
        if (request.getTopicCodes() != null && request.getTopicCodes().length > 0) {
            List<FieldValue> topicCodeValues = Arrays.stream(request.getTopicCodes())
                .map(FieldValue::of)
                .collect(Collectors.toList());
            boolBuilder.filter(Query.of(q -> q.terms(t -> t
                .field("topicCodes")
                .terms(tt -> tt.value(topicCodeValues))
            )));
        }

        // 业务类型过滤
        if (request.getBusinessTypes() != null && request.getBusinessTypes().length > 0) {
            List<FieldValue> businessTypeValues = Arrays.stream(request.getBusinessTypes())
                .map(FieldValue::of)
                .collect(Collectors.toList());
            boolBuilder.filter(Query.of(q -> q.terms(t -> t
                .field("businessTypes")
                .terms(tt -> tt.value(businessTypeValues))
            )));
        }

        // 文件类型过滤
        if (StringUtils.isNotBlank(request.getFileType())) {
            final String fileType = request.getFileType();
            boolBuilder.filter(Query.of(q -> q.term(t -> t
                .field("fileType")
                .value(fileType)
            )));
        }

        // 租户隔离
        String tenantId = request.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            tenantId = TenantHelper.getTenantId();
        }
        if (StringUtils.isNotBlank(tenantId)) {
            final String finalTenantId = tenantId;
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("tenantId").value(finalTenantId))));
        }

        // 只搜索已发布文档
        if (request.getPublishedOnly() != null && request.getPublishedOnly()) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("releaseFlag").value(1))));
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    /**
     * 构建排序选项
     */
    private List<SortOptions> buildSortOptions(KbSearchRequest request) {
        List<SortOptions> sortOptions = new ArrayList<>();

        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();

        if (StringUtils.isNotBlank(sortField)) {
            SortOrder order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;

            switch (sortField) {
                case "createTime":
                    sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createTime").order(order))));
                    break;
                case "viewCount":
                    sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("viewCount").order(order))));
                    break;
                case "score":
                    // ES默认按相关性评分排序,不需要额外设置
                    break;
                default:
                    sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc))));
            }
        } else {
            // 默认按创建时间降序
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc))));
        }

        return sortOptions;
    }

    /**
     * 转换搜索结果为视图对象
     */
    private List<KbSearchResultVo> convertToResultVo(List<Hit<KbDocDocument>> hits) {
        if (CollUtil.isEmpty(hits)) {
            return Collections.emptyList();
        }

        return hits.stream().map(hit -> {
            KbDocDocument doc = hit.source();
            KbSearchResultVo vo = new KbSearchResultVo();

            vo.setDocId(doc.getDocId());
            vo.setDocName(doc.getDocName());
            vo.setDocTitle(doc.getDocTitle());
            vo.setKeywords(doc.getKeywords());
            vo.setFileType(doc.getFileType());
            vo.setFolderId(doc.getFolderId());
            vo.setFolderPath(doc.getFolderPath());
            vo.setTopicCodes(doc.getTopicCodes());
            vo.setBusinessTypes(doc.getBusinessTypes());
            vo.setViewCount(doc.getViewCount());
            vo.setDownloadCount(doc.getDownloadCount());
            vo.setCreateTime(doc.getCreateTime());
            vo.setReleaseFlag(doc.getReleaseFlag());

            // 设置评分
            if (hit.score() != null) {
                vo.setScore(hit.score().floatValue());
            }

            // 提取高亮内容
            if (hit.highlight() != null && hit.highlight().get("content") != null) {
                List<String> fragments = hit.highlight().get("content");
                vo.setContentSnippet(StrUtil.join("...", fragments));
            } else if (hit.highlight() != null && hit.highlight().get("docTitle") != null) {
                List<String> fragments = hit.highlight().get("docTitle");
                vo.setDocTitle(StrUtil.join("", fragments));
            }

            return vo;
        }).collect(Collectors.toList());
    }
}