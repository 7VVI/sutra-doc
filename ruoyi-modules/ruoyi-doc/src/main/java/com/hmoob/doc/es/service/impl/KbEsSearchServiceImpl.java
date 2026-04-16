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
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.satoken.utils.LoginHelper;
import com.hmoob.common.tenant.helper.TenantHelper;
import com.hmoob.doc.domain.KbDoc;
import com.hmoob.doc.domain.KbDocFavourite;
import com.hmoob.doc.mapper.KbDocMapper;
import com.hmoob.doc.mapper.KbDocFavouriteMapper;
import com.hmoob.doc.es.constant.KbEsConstant;
import com.hmoob.doc.es.document.KbDocDocument;
import com.hmoob.doc.es.document.KbSearchLogDocument;
import com.hmoob.doc.es.document.KbSearchRequest;
import com.hmoob.doc.es.document.KbSearchResultVo;
import com.hmoob.doc.es.document.KbSearchFilterParam;
import com.hmoob.doc.es.service.IKbEsSearchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * KB ES搜索服务实现
 * 参考旧系统 KmDocServiceImpl.searchESKmDoc 设计
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(prefix = "kb.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KbEsSearchServiceImpl implements IKbEsSearchService {

    private final ElasticsearchClient esClient;
    private final KbDocMapper docMapper;
    private final KbDocFavouriteMapper favouriteMapper;

    /**
     * 高亮前缀标签(红色加粗)
     */
    private static final String HIGHLIGHT_PRE_TAG = "<span style='color:red;font-weight:bold'>";

    /**
     * 高亮后缀标签
     */
    private static final String HIGHLIGHT_POST_TAG = "</span>";

    @Override
    public TableDataInfo<KbSearchResultVo> search(KbSearchRequest request) {
        try {
            // 构建搜索查询
            Query query = buildSearchQuery(request);

            // 构建排序
            List<SortOptions> sortOptions = buildSortOptions(request);

            // 获取分页参数
            int from = request.getOffset();
            final int size = Math.min(request.getPageSize() != null ? request.getPageSize() : 10, 100);

            // 执行搜索(带高亮)
            SearchResponse<KbDocDocument> response = esClient.search(s -> s
                .index(KbEsConstant.DOC_INDEX_NAME)
                .query(query)
                // 高亮配置 - 参考旧系统设置
                .highlight(h -> h
                    // 标题高亮
                    .fields("docTitle", f -> f
                        .preTags(HIGHLIGHT_PRE_TAG)
                        .postTags(HIGHLIGHT_POST_TAG)
                        .numberOfFragments(1)
                        .fragmentSize(200)
                        .requireFieldMatch(false)
                    )
                    // 内容高亮
                    .fields("content", f -> f
                        .preTags(HIGHLIGHT_PRE_TAG)
                        .postTags(HIGHLIGHT_POST_TAG)
                        .numberOfFragments(1)
                        .fragmentSize(200)
                        .noMatchSize(200)
                        .requireFieldMatch(false)
                    )
                    // 文档名高亮
                    .fields("docName", f -> f
                        .preTags(HIGHLIGHT_PRE_TAG)
                        .postTags(HIGHLIGHT_POST_TAG)
                        .numberOfFragments(1)
                        .fragmentSize(200)
                        .requireFieldMatch(false)
                    )
                )
                // 排除content字段(减少返回数据量)
                .source(sc -> sc
                    .filter(sf -> sf
                        .excludes("content")
                    )
                )
                .sort(sortOptions)
                .from(from)
                .size(size)
            , KbDocDocument.class);

            // 转换结果并补充DB信息
            List<KbSearchResultVo> results = convertToResultVo(response.hits().hits());

            return new TableDataInfo<>(results, response.hits().total().value());
        } catch (Exception e) {
            log.error("ES搜索失败: {}", e.getMessage(), e);
            return TableDataInfo.build();
        }
    }

    @Override
    public TableDataInfo<KbSearchResultVo> searchWithFilters(KbSearchRequest request) {
        // searchWithFilters 与 search 共用同一逻辑，filter条件已在buildSearchQuery中处理
        return search(request);
    }

    @Override
    public List<String> getHotKeywords(Integer limit, Integer days) {
        try {
            final int finalLimit = (limit == null || limit < 1) ? 10 : limit;
            final int finalDays = (days == null || days < 1) ? 7 : days;

            TermsAggregation termsAgg = TermsAggregation.of(t -> t
                .field("keywordsMax")
                .size(finalLimit)
            );

            SearchResponse<KbSearchLogDocument> response = esClient.search(s -> s
                .index(KbEsConstant.SEARCH_LOG_INDEX_NAME)
                .size(0)
                .aggregations("hot_keywords", Aggregation.of(a -> a.terms(termsAgg)))
            , KbSearchLogDocument.class);

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

            final int finalLimit = (limit == null || limit < 1) ? 10 : limit;
            final String queryPrefix = prefix.toLowerCase() + "*";

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
        folderRequest.setTitle(request.getTitle());
        folderRequest.setContent(request.getContent());
        folderRequest.setFolderIds(new Long[]{folderId});
        folderRequest.setPageNum(request.getPageNum());
        folderRequest.setPageSize(request.getPageSize());
        folderRequest.setColumn(request.getColumn());
        folderRequest.setOrder(request.getOrder());
        folderRequest.setTenantId(request.getTenantId());
        folderRequest.setPublishedOnly(request.getPublishedOnly());
        folderRequest.setAdvSearchFlag(request.getAdvSearchFlag());
        folderRequest.setPhraseMatchSearchFlag(request.getPhraseMatchSearchFlag());

        return search(folderRequest);
    }

    @Override
    public List<KbSearchResultVo> searchSimilarDocs(Long docId, Integer limit) {
        try {
            if (docId == null) {
                return Collections.emptyList();
            }

            final int finalLimit = (limit == null || limit < 1) ? 5 : limit;

            KbDocDocument sourceDoc = esClient.get(g -> g
                .index(KbEsConstant.DOC_INDEX_NAME)
                .id(String.valueOf(docId))
            , KbDocDocument.class).source();

            if (sourceDoc == null) {
                return Collections.emptyList();
            }

            final String sourceKeywords = sourceDoc.getKeywords();
            final String sourceTitle = sourceDoc.getDocTitle();
            final String sourceTenantId = sourceDoc.getTenantId();

            Query query = Query.of(q -> q.bool(b -> {
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
                b.mustNot(mn -> mn.term(t -> t.field("docId").value(docId)));
                if (StringUtils.isNotBlank(sourceTenantId)) {
                    b.must(m -> m.term(t -> t.field("tenantId").value(sourceTenantId)));
                }
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
     * 构建搜索查询
     * 参考旧系统 searchESKmDoc 方法的查询构建逻辑
     */
    private Query buildSearchQuery(KbSearchRequest request) {
        BoolQuery.Builder boolFinalBuilder = new BoolQuery.Builder();

        // 普通检索的合并条件容器
        BoolQuery.Builder boolDefaultBuilder = new BoolQuery.Builder();

        // 1. 结果中查询的过滤条件(filterParams)
        if (request.getFilterParams() != null && !request.getFilterParams().isEmpty()) {
            Query filterQuery = buildFilterParamsQuery(request.getFilterParams());
            boolFinalBuilder.filter(filterQuery);
        }

        // 2. 标题检索
        if (StringUtils.isNotBlank(request.getTitle())) {
            Query titleQuery = buildTitleQuery(request);
            if (Boolean.TRUE.equals(request.getAdvSearchFlag())) {
                // 高级检索: 必须满足
                boolFinalBuilder.must(titleQuery);
            } else {
                // 普通检索: 作为候选条件
                boolDefaultBuilder.should(titleQuery);
            }
        }

        // 3. 内容/全文检索
        if (StringUtils.isNotBlank(request.getContent())) {
            Query contentQuery = buildContentQuery(request);
            if (Boolean.TRUE.equals(request.getAdvSearchFlag())) {
                boolFinalBuilder.must(contentQuery);
            } else {
                boolDefaultBuilder.should(contentQuery);
            }
        }

        // 4. 关键词检索
        if (StringUtils.isNotBlank(request.getKeywords())) {
            // 通用关键词搜索: 多字段匹配
            Query keywordsQuery = buildKeywordsQuery(request);
            if (Boolean.TRUE.equals(request.getAdvSearchFlag())) {
                boolFinalBuilder.must(keywordsQuery);
            } else {
                boolDefaultBuilder.should(keywordsQuery);
            }
        }

        // 处理普通检索的合并条件
        if (!Boolean.TRUE.equals(request.getAdvSearchFlag())) {
            boolFinalBuilder.must(Query.of(q -> q.bool(boolDefaultBuilder.build())));
        }

        // 5. 分类过滤
        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            List<FieldValue> categoryValues = request.getCategory().stream()
                .map(FieldValue::of)
                .collect(Collectors.toList());
            boolFinalBuilder.filter(Query.of(q -> q.terms(t -> t
                .field("category")
                .terms(tt -> tt.value(categoryValues))
            )));
        }

        // 6. 业务类型过滤
        if (request.getBusinessTypes() != null && request.getBusinessTypes().length > 0) {
            List<FieldValue> businessTypeValues = Arrays.stream(request.getBusinessTypes())
                .map(FieldValue::of)
                .collect(Collectors.toList());
            boolFinalBuilder.filter(Query.of(q -> q.terms(t -> t
                .field("businessTypes")
                .terms(tt -> tt.value(businessTypeValues))
            )));
        }

        // 7. 主题过滤(支持前缀模糊匹配)
        if (request.getTopicCodes() != null && request.getTopicCodes().length > 0) {
            BoolQuery.Builder topicQueryBuilder = new BoolQuery.Builder();
            for (String topicCode : request.getTopicCodes()) {
                topicQueryBuilder.should(Query.of(q -> q.prefix(p -> p
                    .field("topicCodes")
                    .value(topicCode)
                )));
            }
            boolFinalBuilder.filter(Query.of(q -> q.bool(topicQueryBuilder.build())));
        }

        // 8. 文件夹过滤
        if (request.getFolderIds() != null && request.getFolderIds().length > 0) {
            List<FieldValue> folderIdValues = Arrays.stream(request.getFolderIds())
                .map(FieldValue::of)
                .collect(Collectors.toList());
            boolFinalBuilder.filter(Query.of(q -> q.terms(t -> t
                .field("folderId")
                .terms(tt -> tt.value(folderIdValues))
            )));
        }

        // 9. 文件类型过滤
        if (StringUtils.isNotBlank(request.getFileType())) {
            boolFinalBuilder.filter(Query.of(q -> q.term(t -> t
                .field("fileType")
                .value(request.getFileType())
            )));
        }

        // 10. 时间范围过滤
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        if (request.getCreateTimeEnd() != null) {
            final String endTime = format.format(request.getCreateTimeEnd());
            boolFinalBuilder.filter(Query.of(q -> q.range(r -> r
                .untyped(u -> u.field("createTime").lte(co.elastic.clients.json.JsonData.of(endTime)))
            )));
        }
        if (request.getCreateTimeStart() != null) {
            final String startTime = format.format(request.getCreateTimeStart());
            boolFinalBuilder.filter(Query.of(q -> q.range(r -> r
                .untyped(u -> u.field("createTime").gte(co.elastic.clients.json.JsonData.of(startTime)))
            )));
        }

        // 11. 租户隔离
        final String tenantId = StringUtils.isNotBlank(request.getTenantId()) ?
            request.getTenantId() : TenantHelper.getTenantId();
        if (StringUtils.isNotBlank(tenantId)) {
            boolFinalBuilder.filter(Query.of(q -> q.term(t -> t
                .field("tenantId")
                .value(tenantId)
            )));
        }

        // 12. 只搜索已发布文档
        if (request.getPublishedOnly() != null && request.getPublishedOnly()) {
            boolFinalBuilder.filter(Query.of(q -> q.term(t -> t
                .field("releaseFlag")
                .value(1)
            )));
        }

        return Query.of(q -> q.bool(boolFinalBuilder.build()));
    }

    /**
     * 构建标题查询
     */
    private Query buildTitleQuery(KbSearchRequest request) {
        if (Boolean.TRUE.equals(request.getPhraseMatchSearchFlag())) {
            // 精确短语匹配
            return Query.of(q -> q.matchPhrase(mp -> mp
                .field("docTitle")
                .query(request.getTitle())
                .slop(2)
            ));
        } else {
            // 模糊匹配，使用IK分词器
            return Query.of(q -> q.match(m -> m
                .field("docTitle")
                .query(request.getTitle())
                .analyzer(KbEsConstant.ANALYZER_IK_SMART)
            ));
        }
    }

    /**
     * 构建内容查询
     */
    private Query buildContentQuery(KbSearchRequest request) {
        if (Boolean.TRUE.equals(request.getPhraseMatchSearchFlag())) {
            return Query.of(q -> q.matchPhrase(mp -> mp
                .field("content")
                .query(request.getContent())
                .slop(2)
            ));
        } else {
            return Query.of(q -> q.match(m -> m
                .field("content")
                .query(request.getContent())
                .analyzer(KbEsConstant.ANALYZER_IK_SMART)
            ));
        }
    }

    /**
     * 构建关键词查询(多字段匹配)
     */
    private Query buildKeywordsQuery(KbSearchRequest request) {
        return Query.of(q -> q.multiMatch(mm -> mm
            .fields("docName",
                "docTitle^" + KbEsConstant.TITLE_SEARCH_BOOST,
                "content^" + KbEsConstant.CONTENT_SEARCH_BOOST,
                "keywords^" + KbEsConstant.KEYWORD_SEARCH_BOOST)
            .query(request.getKeywords())
            .analyzer(KbEsConstant.ANALYZER_IK_SMART)
            .type(TextQueryType.BestFields)
        ));
    }

    /**
     * 构建过滤参数查询
     */
    private Query buildFilterParamsQuery(List<KbSearchFilterParam> filterParams) {
        BoolQuery.Builder filterBuilder = new BoolQuery.Builder();

        for (KbSearchFilterParam param : filterParams) {
            Query fieldQuery = buildFieldFilterQuery(param);
            filterBuilder.must(fieldQuery);
        }

        return Query.of(q -> q.bool(filterBuilder.build()));
    }

    /**
     * 构建单个字段过滤查询
     */
    private Query buildFieldFilterQuery(KbSearchFilterParam param) {
        String field = param.getField();
        String operator = param.getOperator();
        final String value = param.getValue();

        switch (operator) {
            case "eq":
                return Query.of(q -> q.term(t -> t.field(field).value(value)));
            case "ne":
                return Query.of(q -> q.bool(b -> b.mustNot(mn -> mn.term(t -> t.field(field).value(value)))));
            case "gt":
                return Query.of(q -> q.range(r -> r.untyped(u -> u.field(field).gt(co.elastic.clients.json.JsonData.of(value)))));
            case "gte":
                return Query.of(q -> q.range(r -> r.untyped(u -> u.field(field).gte(co.elastic.clients.json.JsonData.of(value)))));
            case "lt":
                return Query.of(q -> q.range(r -> r.untyped(u -> u.field(field).lt(co.elastic.clients.json.JsonData.of(value)))));
            case "lte":
                return Query.of(q -> q.range(r -> r.untyped(u -> u.field(field).lte(co.elastic.clients.json.JsonData.of(value)))));
            case "like":
                return Query.of(q -> q.wildcard(w -> w.field(field).value("*" + value + "*")));
            case "in":
                if (param.getValues() != null && param.getValues().length > 0) {
                    List<FieldValue> values = Arrays.stream(param.getValues())
                        .map(FieldValue::of)
                        .collect(Collectors.toList());
                    return Query.of(q -> q.terms(t -> t.field(field).terms(tt -> tt.value(values))));
                }
                return Query.of(q -> q.term(t -> t.field(field).value(value)));
            default:
                return Query.of(q -> q.term(t -> t.field(field).value(value)));
        }
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
                case "downloadCount":
                    sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("downloadCount").order(order))));
                    break;
                case "score":
                    // ES默认按相关性评分排序
                    break;
                default:
                    sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc))));
            }
        } else {
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc))));
        }

        return sortOptions;
    }

    /**
     * 转换搜索结果为视图对象，并补充数据库信息
     * 参考旧系统 retrieveDocDbInfo 方法
     */
    private List<KbSearchResultVo> convertToResultVo(List<Hit<KbDocDocument>> hits) {
        if (CollUtil.isEmpty(hits)) {
            return Collections.emptyList();
        }

        // 收集文档ID列表
        List<Long> docIds = hits.stream()
            .map(h -> h.source().getDocId())
            .collect(Collectors.toList());

        // 从数据库获取补充信息
        Map<Long, KbDoc> docMap = retrieveDocInfoFromDb(docIds);

        // 获取当前用户的收藏列表
        Set<Long> favouriteDocIds = retrieveFavouriteDocIds(docIds);

        return hits.stream().map(hit -> {
            KbDocDocument doc = hit.source();
            KbSearchResultVo vo = new KbSearchResultVo();

            // ES基本信息
            vo.setDocId(doc.getDocId());
            vo.setDocName(doc.getDocName());
            vo.setDocTitle(doc.getDocTitle());
            vo.setKeywords(doc.getKeywords());
            vo.setFileType(doc.getFileType());
            vo.setFolderId(doc.getFolderId());
            vo.setFolderPath(doc.getFolderPath());
            vo.setCategory(doc.getCategory());
            vo.setTopicCodes(doc.getTopicCodes());
            vo.setBusinessTypes(doc.getBusinessTypes());
            vo.setViewCount(doc.getViewCount());
            vo.setDownloadCount(doc.getDownloadCount());
            vo.setCreateTime(doc.getCreateTime());
            vo.setReleaseFlag(doc.getReleaseFlag());
            vo.setOrgCode(doc.getOrgCode());

            // 设置评分
            if (hit.score() != null) {
                vo.setScore(hit.score().floatValue());
            }

            // 提取高亮内容
            if (hit.highlight() != null) {
                // 标题高亮
                if (hit.highlight().get("docTitle") != null && !hit.highlight().get("docTitle").isEmpty()) {
                    vo.setHighlightTitle(StrUtil.join("", hit.highlight().get("docTitle")));
                    vo.setDocTitle(vo.getHighlightTitle());
                }
                // 内容高亮片段
                if (hit.highlight().get("content") != null && !hit.highlight().get("content").isEmpty()) {
                    vo.setContentSnippet(StrUtil.join("...", hit.highlight().get("content")));
                }
                // 文档名高亮
                if (hit.highlight().get("docName") != null && !hit.highlight().get("docName").isEmpty()) {
                    vo.setDocName(StrUtil.join("", hit.highlight().get("docName")));
                }
            }

            // 补充数据库信息
            KbDoc dbDoc = docMap.get(doc.getDocId());
            if (dbDoc != null) {
                vo.setFileId(dbDoc.getFileId());
                vo.setPreviewFileId(dbDoc.getPreviewFileId());
                vo.setFileSize(dbDoc.getFileSize());
                vo.setPublicRemark(dbDoc.getPublicRemark());
                vo.setRemark(dbDoc.getRemark());
                vo.setCreateBy(dbDoc.getCreateBy() != null ? String.valueOf(dbDoc.getCreateBy()) : null);
            }

            // 设置收藏标记
            vo.setFavourite(favouriteDocIds.contains(doc.getDocId()) ? 1 : 0);

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 从数据库获取文档补充信息
     */
    private Map<Long, KbDoc> retrieveDocInfoFromDb(List<Long> docIds) {
        if (CollUtil.isEmpty(docIds)) {
            return Collections.emptyMap();
        }

        List<KbDoc> docs = docMapper.selectBatchIds(docIds);
        return docs.stream()
            .collect(Collectors.toMap(KbDoc::getDocId, d -> d, (a, b) -> a));
    }

    /**
     * 获取当前用户收藏的文档ID集合
     */
    private Set<Long> retrieveFavouriteDocIds(List<Long> docIds) {
        try {
            Long userId = LoginHelper.getUserId();
            if (userId == null || CollUtil.isEmpty(docIds)) {
                return Collections.emptySet();
            }

            LambdaQueryWrapper<KbDocFavourite> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(KbDocFavourite::getDocId, docIds)
                .eq(KbDocFavourite::getUserId, userId)
                .eq(KbDocFavourite::getDelFlag, "0");

            List<KbDocFavourite> favourites = favouriteMapper.selectList(wrapper);
            return favourites.stream()
                .map(KbDocFavourite::getDocId)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("获取收藏信息失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }
}