package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.excel.utils.ExcelUtil;
import com.hmoob.common.log.annotation.Log;
import com.hmoob.common.log.enums.BusinessType;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.query.KbDocLogQuery;
import com.hmoob.doc.domain.vo.KbDocLogVo;
import com.hmoob.doc.service.IKbDocLogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KB文档日志管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/doclog")
public class KbDocLogController extends BaseController {

    private final IKbDocLogService docLogService;

    /**
     * 分页查询文档日志
     */
    @SaCheckPermission("kb:doclog:list")
    @GetMapping("/list")
    public TableDataInfo<KbDocLogVo> list(KbDocLogQuery query) {
        return docLogService.selectPageLogList(query);
    }

    /**
     * 批量删除文档日志
     *
     * @param ids 日志ID列表（逗号分隔）
     */
    @SaCheckPermission("kb:doclog:remove")
    @Log(title = "文档日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable String ids) {
        List<Long> visitIds = StrUtil.split(ids, ',')
            .stream()
            .map(Long::parseLong)
            .toList();
        if (CollUtil.isEmpty(visitIds)) {
            return R.fail("请选择要删除的日志");
        }
        return toAjax(docLogService.deleteLogByIds(visitIds));
    }

    /**
     * 导出文档日志（全部）
     */
    @SaCheckPermission("kb:doclog:export")
    @Log(title = "文档日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KbDocLogQuery query, HttpServletResponse response) {
        List<KbDocLogVo> list = docLogService.selectLogList(query);
        ExcelUtil.exportExcel(list, "文档日志", KbDocLogVo.class, response);
    }

    /**
     * 导出文档日志（选中）
     *
     * @param ids 日志ID列表（逗号分隔）
     */
    @SaCheckPermission("kb:doclog:export")
    @Log(title = "文档日志", businessType = BusinessType.EXPORT)
    @PostMapping("/exportSelected")
    public void exportSelected(@RequestParam @NotEmpty(message = "请选择要导出的日志") String ids,
                               HttpServletResponse response) {
        List<Long> visitIds = StrUtil.split(ids, ',')
            .stream()
            .map(Long::parseLong)
            .toList();
        List<KbDocLogVo> list = docLogService.selectLogByIds(visitIds);
        ExcelUtil.exportExcel(list, "文档日志", KbDocLogVo.class, response);
    }

}