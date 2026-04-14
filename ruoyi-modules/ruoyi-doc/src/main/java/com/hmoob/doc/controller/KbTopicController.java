package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.lang.tree.Tree;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.core.validate.AddGroup;
import com.hmoob.common.core.validate.EditGroup;
import com.hmoob.common.excel.utils.ExcelUtil;
import com.hmoob.common.idempotent.annotation.RepeatSubmit;
import com.hmoob.common.log.annotation.Log;
import com.hmoob.common.log.enums.BusinessType;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.bo.KbTopicBo;
import com.hmoob.doc.domain.vo.KbTopicVo;
import com.hmoob.doc.service.IKbTopicService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KB主题管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/topic")
public class KbTopicController extends BaseController {

    private final IKbTopicService topicService;

    /**
     * 查询主题列表
     */
    @SaCheckPermission("kb:topic:list")
    @GetMapping("/list")
    public TableDataInfo<KbTopicVo> list(KbTopicBo bo, PageQuery pageQuery) {
        return topicService.selectPageTopicList(bo, pageQuery);
    }

    /**
     * 查询主题列表(不分页)
     */
    @SaCheckPermission("kb:topic:list")
    @GetMapping("/listAll")
    public R<List<KbTopicVo>> listAll(KbTopicBo bo) {
        return R.ok(topicService.selectTopicList(bo));
    }

    /**
     * 查询主题树结构
     */
    @SaCheckPermission("kb:topic:list")
    @GetMapping("/tree")
    public R<List<Tree<Long>>> tree(KbTopicBo bo) {
        return R.ok(topicService.selectTopicTreeList(bo));
    }

    /**
     * 导出主题列表
     */
    @SaCheckPermission("kb:topic:export")
    @Log(title = "主题管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KbTopicBo bo, HttpServletResponse response) {
        List<KbTopicVo> list = topicService.selectTopicList(bo);
        ExcelUtil.exportExcel(list, "主题管理", KbTopicVo.class, response);
    }

    /**
     * 获取主题详细信息
     *
     * @param topicId 主题ID
     */
    @SaCheckPermission("kb:topic:query")
    @GetMapping("/{topicId}")
    public R<KbTopicVo> getInfo(@NotNull(message = "主题ID不能为空") @PathVariable Long topicId) {
        return R.ok(topicService.selectTopicById(topicId));
    }

    /**
     * 根据主题编码查询
     *
     * @param topicCode 主题编码
     */
    @SaCheckPermission("kb:topic:query")
    @GetMapping("/code/{topicCode}")
    public R<KbTopicVo> getByCode(@PathVariable String topicCode) {
        return R.ok(topicService.selectTopicByCode(topicCode));
    }

    /**
     * 新增主题
     */
    @SaCheckPermission("kb:topic:add")
    @Log(title = "主题管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KbTopicBo bo) {
        if (!topicService.checkTopicCodeUnique(bo)) {
            return R.fail("新增主题'" + bo.getTopicName() + "'失败,主题编码已存在");
        }
        return toAjax(topicService.insertTopic(bo));
    }

    /**
     * 修改主题
     */
    @SaCheckPermission("kb:topic:edit")
    @Log(title = "主题管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KbTopicBo bo) {
        if (!topicService.checkTopicCodeUnique(bo)) {
            return R.fail("修改主题'" + bo.getTopicName() + "'失败,主题编码已存在");
        }
        return toAjax(topicService.updateTopic(bo));
    }

    /**
     * 删除主题
     *
     * @param topicId 主题ID
     */
    @SaCheckPermission("kb:topic:remove")
    @Log(title = "主题管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{topicId}")
    public R<Void> remove(@PathVariable Long topicId) {
        if (topicService.hasChildByTopicId(topicId)) {
            return R.fail("存在下级主题,不允许删除");
        }
        return toAjax(topicService.deleteTopicById(topicId));
    }

}