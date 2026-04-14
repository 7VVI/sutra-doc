package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
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
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.bo.KbSysConfigBo;
import com.hmoob.doc.domain.vo.KbSysConfigVo;
import com.hmoob.doc.service.IKbSysConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KB系统配置管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/config")
public class KbSysConfigController extends BaseController {

    private final IKbSysConfigService configService;

    /**
     * 查询系统配置列表
     */
    @SaCheckPermission("kb:config:list")
    @GetMapping("/list")
    public R<List<KbSysConfigVo>> list(KbSysConfigBo bo) {
        return R.ok(configService.selectConfigList(bo));
    }

    /**
     * 导出系统配置列表
     */
    @SaCheckPermission("kb:config:export")
    @Log(title = "系统配置管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KbSysConfigBo bo, HttpServletResponse response) {
        List<KbSysConfigVo> list = configService.selectConfigList(bo);
        ExcelUtil.exportExcel(list, "系统配置管理", KbSysConfigVo.class, response);
    }

    /**
     * 获取系统配置详细信息
     *
     * @param configId 配置ID
     */
    @SaCheckPermission("kb:config:query")
    @GetMapping("/{configId}")
    public R<KbSysConfigVo> getInfo(@NotNull(message = "配置ID不能为空") @PathVariable Long configId) {
        return R.ok(configService.selectConfigById(configId));
    }

    /**
     * 根据配置项编码查询
     *
     * @param itemCode 配置项编码
     */
    @SaCheckPermission("kb:config:query")
    @GetMapping("/code/{itemCode}")
    public R<KbSysConfigVo> getByCode(@PathVariable String itemCode) {
        return R.ok(configService.selectConfigByCode(itemCode));
    }

    /**
     * 获取配置项值
     *
     * @param itemCode 配置项编码
     */
    @SaCheckPermission("kb:config:query")
    @GetMapping("/value/{itemCode}")
    public R<String> getValue(@PathVariable String itemCode) {
        return R.ok(configService.getConfigValue(itemCode));
    }

    /**
     * 新增系统配置
     */
    @SaCheckPermission("kb:config:add")
    @Log(title = "系统配置管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KbSysConfigBo bo) {
        if (!configService.checkConfigCodeUnique(bo)) {
            return R.fail("新增配置'" + bo.getItemName() + "'失败,配置项编码已存在");
        }
        return toAjax(configService.insertConfig(bo));
    }

    /**
     * 修改系统配置
     */
    @SaCheckPermission("kb:config:edit")
    @Log(title = "系统配置管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KbSysConfigBo bo) {
        if (!configService.checkConfigCodeUnique(bo)) {
            return R.fail("修改配置'" + bo.getItemName() + "'失败,配置项编码已存在");
        }
        return toAjax(configService.updateConfig(bo));
    }

    /**
     * 删除系统配置
     *
     * @param configId 配置ID
     */
    @SaCheckPermission("kb:config:remove")
    @Log(title = "系统配置管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configId}")
    public R<Void> remove(@PathVariable Long configId) {
        return toAjax(configService.deleteConfigById(configId));
    }

}