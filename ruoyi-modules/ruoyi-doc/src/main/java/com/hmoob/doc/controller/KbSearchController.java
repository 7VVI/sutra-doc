package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.log.annotation.Log;
import com.hmoob.common.log.enums.BusinessType;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.satoken.utils.LoginHelper;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.vo.KbSearchRecordVo;
import com.hmoob.doc.domain.vo.SearchStatVo;
import com.hmoob.doc.es.document.KbSearchRequest;
import com.hmoob.doc.es.document.KbSearchResultVo;
import com.hmoob.doc.es.service.IKbEsSearchService;
import com.hmoob.doc.service.IKbSearchRecordService;
import com.hmoob.doc.service.IKbDocService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * KB搜索管理
 * 参考旧系统 KmDocController.searchDoc 设计
 *
 * @author hmoob
 */
@Tag(name = "KB搜索管理")
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/search")
public class KbSearchController extends BaseController {

    private final IKbEsSearchService esSearchService;
    private final IKbSearchRecordService searchRecordService;
    private final IKbDocService docService;

    /**
     * 普通检索 - 关键词搜索
     * 参考旧系统 /searchDoc 接口
     */
    @Operation(summary = "普通检索", description = "关键词搜索文档")
    @SaIgnore
    @Log(title = "KB搜索-普通检索", businessType = BusinessType.OTHER)
    @PostMapping("/searchDoc")
    public R<TableDataInfo<KbSearchResultVo>> searchDoc(
        @RequestBody KbSearchRequest request,
        HttpServletRequest req) {
        try {
            // 设置默认分页参数
            if (request.getPageNum() == null) {
                request.setPageNum(1);
            }
            if (request.getPageSize() == null) {
                request.setPageSize(10);
            }
            // 默认只搜索已发布文档
            request.setPublishedOnly(true);

            // 执行ES搜索
            TableDataInfo<KbSearchResultVo> result = esSearchService.search(request);

            // 记录搜索日志
            saveSearchLog(request, result.getTotal(), req);

            return R.ok(result);
        } catch (Exception e) {
            log.error("搜索失败: {}", e.getMessage(), e);
            return R.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 高级检索 - 多条件组合搜索
     * 参考旧系统 /advanceSearchDoc 接口
     */
    @Operation(summary = "高级检索", description = "多条件组合搜索文档")
    @SaIgnore
    @Log(title = "KB搜索-高级检索", businessType = BusinessType.OTHER)
    @GetMapping("/advanceSearchDoc")
    public R<TableDataInfo<KbSearchResultVo>> advanceSearchDoc(
        KbSearchRequest request,
        @Parameter(description = "页码") @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
        @Parameter(description = "每页大小") @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
        HttpServletRequest req) {
        try {
            request.setPageNum(pageNum);
            request.setPageSize(pageSize);
            request.setAdvSearchFlag(true); // 高级检索标识
            request.setPublishedOnly(true);

            TableDataInfo<KbSearchResultVo> result = esSearchService.search(request);

            saveSearchLog(request, result.getTotal(), req);

            return R.ok(result);
        } catch (Exception e) {
            log.error("高级检索失败: {}", e.getMessage(), e);
            return R.fail("高级检索失败: " + e.getMessage());
        }
    }

    /**
     * 文件夹内搜索
     */
    @Operation(summary = "文件夹内搜索", description = "在指定文件夹内搜索文档")
    @SaIgnore
    @Log(title = "KB搜索-文件夹搜索", businessType = BusinessType.OTHER)
    @PostMapping("/searchInFolder/{folderId}")
    public R<TableDataInfo<KbSearchResultVo>> searchInFolder(
        @Parameter(description = "文件夹ID") @PathVariable Long folderId,
        @RequestBody(required = false) KbSearchRequest request,
        HttpServletRequest req) {
        try {
            if (request == null) {
                request = new KbSearchRequest();
            }
            request.setPageNum(request.getPageNum() != null ? request.getPageNum() : 1);
            request.setPageSize(request.getPageSize() != null ? request.getPageSize() : 10);
            request.setPublishedOnly(true);

            TableDataInfo<KbSearchResultVo> result = esSearchService.searchByFolder(folderId, request);

            saveSearchLog(request, result.getTotal(), req);

            return R.ok(result);
        } catch (Exception e) {
            log.error("文件夹内搜索失败: {}", e.getMessage(), e);
            return R.fail("文件夹内搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门关键词
     */
    @Operation(summary = "获取热门关键词", description = "获取最近一段时间的热门搜索关键词")
    @SaIgnore
    @GetMapping("/hotKeywords")
    public R<List<String>> getHotKeywords(
        @Parameter(description = "数量限制") @RequestParam(required = false, defaultValue = "10") Integer limit,
        @Parameter(description = "统计天数") @RequestParam(required = false, defaultValue = "7") Integer days) {
        List<String> hotKeywords = esSearchService.getHotKeywords(limit, days);
        return R.ok(hotKeywords);
    }

    /**
     * 获取搜索建议(自动补全)
     */
    @Operation(summary = "获取搜索建议", description = "根据输入前缀获取搜索建议")
    @SaIgnore
    @GetMapping("/suggestions")
    public R<List<String>> getSearchSuggestions(
        @Parameter(description = "搜索前缀") @RequestParam String prefix,
        @Parameter(description = "数量限制") @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<String> suggestions = esSearchService.getSearchSuggestions(prefix, limit);
        return R.ok(suggestions);
    }

    /**
     * 获取相似文档
     */
    @Operation(summary = "获取相似文档", description = "根据文档ID获取相似文档列表")
    @SaIgnore
    @GetMapping("/similar/{docId}")
    public R<List<KbSearchResultVo>> getSimilarDocs(
        @Parameter(description = "文档ID") @PathVariable Long docId,
        @Parameter(description = "数量限制") @RequestParam(required = false, defaultValue = "5") Integer limit) {
        List<KbSearchResultVo> similarDocs = esSearchService.searchSimilarDocs(docId, limit);
        return R.ok(similarDocs);
    }

    /**
     * 获取搜索统计信息
     */
    @Operation(summary = "获取搜索统计", description = "获取搜索相关的统计数据")
    @GetMapping("/stat")
    public R<SearchStatVo> getSearchStat() {
        SearchStatVo statVo = new SearchStatVo();
        // 获取最近7天热门关键词
        statVo.setHotKeywords(esSearchService.getHotKeywords(10, 7));
        // 设置其他统计数据
        statVo.setTotalSearchCount(0L);
        statVo.setTodaySearchCount(0L);
        return R.ok(statVo);
    }

    /**
     * 获取用户搜索历史
     */
    @Operation(summary = "获取搜索历史", description = "获取当前用户的搜索历史记录")
    @GetMapping("/history")
    public R<List<KbSearchRecordVo>> getSearchHistory(
        @Parameter(description = "数量限制") @RequestParam(required = false, defaultValue = "20") Integer limit) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return R.ok(List.of());
        }
        List<KbSearchRecordVo> history = searchRecordService.getUserSearchHistory(userId, limit);
        return R.ok(history);
    }

    /**
     * 清空用户搜索历史
     */
    @Operation(summary = "清空搜索历史", description = "清空当前用户的搜索历史记录")
    @Log(title = "KB搜索-清空历史", businessType = BusinessType.DELETE)
    @DeleteMapping("/history")
    public R<Void> clearSearchHistory() {
        Long userId = LoginHelper.getUserId();
        if (userId != null) {
            searchRecordService.clearUserSearchHistory(userId);
        }
        return R.ok();
    }

    /**
     * 保存搜索日志
     */
    private void saveSearchLog(KbSearchRequest request, Long resultCount, HttpServletRequest req) {
        try {
            // 记录搜索关键词到数据库和ES
            String keywords = StrUtil.isNotBlank(request.getKeywords()) ? request.getKeywords() :
                (StrUtil.isNotBlank(request.getTitle()) ? request.getTitle() :
                    (StrUtil.isNotBlank(request.getContent()) ? request.getContent() : null));

            if (StrUtil.isNotBlank(keywords)) {
                searchRecordService.logSearch(
                    keywords,
                    resultCount,
                    request.getTopicCodes(),
                    request.getBusinessTypes(),
                    request.getFolderIds(),
                    req
                );
            }
        } catch (Exception e) {
            log.warn("保存搜索日志失败: {}", e.getMessage());
        }
    }
}