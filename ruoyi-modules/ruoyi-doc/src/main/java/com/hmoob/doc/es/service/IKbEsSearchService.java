package com.hmoob.doc.es.service;

import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.es.document.KbSearchRequest;
import com.hmoob.doc.es.document.KbSearchResultVo;

import java.util.List;

/**
 * KB ES搜索服务接口
 *
 * @author hmoob
 */
public interface IKbEsSearchService {

    /**
     * 全文检索(带高亮)
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    TableDataInfo<KbSearchResultVo> search(KbSearchRequest request);

    /**
     * 带过滤条件的搜索(文件夹、主题、业务类型)
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    TableDataInfo<KbSearchResultVo> searchWithFilters(KbSearchRequest request);

    /**
     * 获取热门搜索关键词
     *
     * @param limit 限制数量
     * @param days 统计天数
     * @return 热门关键词列表
     */
    List<String> getHotKeywords(Integer limit, Integer days);

    /**
     * 获取搜索建议(自动补全)
     *
     * @param prefix 前缀词
     * @param limit 限制数量
     * @return 建议词列表
     */
    List<String> getSearchSuggestions(String prefix, Integer limit);

    /**
     * 根据文件夹ID搜索文档
     *
     * @param folderId 文件夹ID
     * @param request  搜索请求
     * @return 搜索结果
     */
    TableDataInfo<KbSearchResultVo> searchByFolder(Long folderId, KbSearchRequest request);

    /**
     * 搜索相似文档
     *
     * @param docId  文档ID
     * @param limit  限制数量
     * @return 相似文档列表
     */
    List<KbSearchResultVo> searchSimilarDocs(Long docId, Integer limit);
}