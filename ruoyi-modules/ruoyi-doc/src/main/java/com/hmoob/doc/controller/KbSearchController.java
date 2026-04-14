package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.log.annotation.Log;
import com.hmoob.common.log.enums.BusinessType;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.vo.KbSearchRecordVo;
import com.hmoob.doc.service.IKbSearchRecordService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KB搜索管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/search")
public class KbSearchController extends BaseController {

    private final IKbSearchRecordService searchRecordService;

    /**
     * 全文搜索
     */
    @SaCheckPermission("kb:search:list")
    @Log(title = "搜索管理", businessType = BusinessType.OTHER)
    @PostMapping
    public R<List<KbSearchRecordVo>> search(@RequestBody KbSearchRecordVo searchRequest) {
        // 实际搜索逻辑由ES服务处理
        // 此处仅返回示例数据
        return R.ok(searchRecordService.selectSearchRecordList(null));
    }

    /**
     * 获取热门关键词
     */
    @SaCheckPermission("kb:search:list")
    @GetMapping("/hot")
    public R<List<String>> getHotKeywords() {
        // 默认返回最近30天的前10个热门关键词
        List<String> hotKeywords = searchRecordService.getHotKeywords(10, 30);
        return R.ok(hotKeywords);
    }

    /**
     * 获取搜索建议
     *
     * @param prefix 搜索前缀
     */
    @SaCheckPermission("kb:search:list")
    @GetMapping("/suggest")
    public R<List<String>> getSuggest(@RequestParam(required = false) String prefix) {
        // 实际建议逻辑由ES服务处理
        // 此处返回示例数据
        List<String> suggestions = List.of();
        return R.ok(suggestions);
    }

    /**
     * 获取用户搜索历史
     */
    @SaCheckPermission("kb:search:list")
    @GetMapping("/history")
    public R<List<KbSearchRecordVo>> getSearchHistory() {
        // 获取当前用户的搜索历史
        List<KbSearchRecordVo> history = List.of();
        return R.ok(history);
    }

    /**
     * 清空用户搜索历史
     */
    @SaCheckPermission("kb:search:edit")
    @Log(title = "搜索管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/history")
    public R<Void> clearSearchHistory() {
        return toAjax(true);
    }

}