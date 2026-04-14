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
import com.hmoob.doc.domain.bo.KbBusinessTypeBo;
import com.hmoob.doc.domain.vo.KbBusinessTypeVo;
import com.hmoob.doc.service.IKbBusinessTypeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KB业务类型管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/businessType")
public class KbBusinessTypeController extends BaseController {

    private final IKbBusinessTypeService businessTypeService;

    /**
     * 查询业务类型列表
     */
    @SaCheckPermission("kb:businessType:list")
    @GetMapping("/list")
    public TableDataInfo<KbBusinessTypeVo> list(KbBusinessTypeBo bo, PageQuery pageQuery) {
        return businessTypeService.selectPageBusinessTypeList(bo, pageQuery);
    }

    /**
     * 查询业务类型列表(不分页)
     */
    @SaCheckPermission("kb:businessType:list")
    @GetMapping("/listAll")
    public R<List<KbBusinessTypeVo>> listAll(KbBusinessTypeBo bo) {
        return R.ok(businessTypeService.selectBusinessTypeList(bo));
    }

    /**
     * 查询业务类型树结构
     */
    @SaCheckPermission("kb:businessType:list")
    @GetMapping("/tree")
    public R<List<Tree<Long>>> tree(KbBusinessTypeBo bo) {
        return R.ok(businessTypeService.selectBusinessTypeTreeList(bo));
    }

    /**
     * 导出业务类型列表
     */
    @SaCheckPermission("kb:businessType:export")
    @Log(title = "业务类型管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KbBusinessTypeBo bo, HttpServletResponse response) {
        List<KbBusinessTypeVo> list = businessTypeService.selectBusinessTypeList(bo);
        ExcelUtil.exportExcel(list, "业务类型管理", KbBusinessTypeVo.class, response);
    }

    /**
     * 获取业务类型详细信息
     *
     * @param businessTypeId 业务类型ID
     */
    @SaCheckPermission("kb:businessType:query")
    @GetMapping("/{businessTypeId}")
    public R<KbBusinessTypeVo> getInfo(@NotNull(message = "业务类型ID不能为空") @PathVariable Long businessTypeId) {
        return R.ok(businessTypeService.selectBusinessTypeById(businessTypeId));
    }

    /**
     * 根据业务类型编码查询
     *
     * @param businessTypeCode 业务类型编码
     */
    @SaCheckPermission("kb:businessType:query")
    @GetMapping("/code/{businessTypeCode}")
    public R<KbBusinessTypeVo> getByCode(@PathVariable String businessTypeCode) {
        return R.ok(businessTypeService.selectBusinessTypeByCode(businessTypeCode));
    }

    /**
     * 新增业务类型
     */
    @SaCheckPermission("kb:businessType:add")
    @Log(title = "业务类型管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KbBusinessTypeBo bo) {
        if (!businessTypeService.checkBusinessTypeCodeUnique(bo)) {
            return R.fail("新增业务类型'" + bo.getBusinessTypeName() + "'失败,业务类型编码已存在");
        }
        return toAjax(businessTypeService.insertBusinessType(bo));
    }

    /**
     * 修改业务类型
     */
    @SaCheckPermission("kb:businessType:edit")
    @Log(title = "业务类型管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KbBusinessTypeBo bo) {
        if (!businessTypeService.checkBusinessTypeCodeUnique(bo)) {
            return R.fail("修改业务类型'" + bo.getBusinessTypeName() + "'失败,业务类型编码已存在");
        }
        return toAjax(businessTypeService.updateBusinessType(bo));
    }

    /**
     * 删除业务类型
     *
     * @param businessTypeId 业务类型ID
     */
    @SaCheckPermission("kb:businessType:remove")
    @Log(title = "业务类型管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{businessTypeId}")
    public R<Void> remove(@PathVariable Long businessTypeId) {
        if (businessTypeService.hasChildByBusinessTypeId(businessTypeId)) {
            return R.fail("存在下级业务类型,不允许删除");
        }
        return toAjax(businessTypeService.deleteBusinessTypeById(businessTypeId));
    }

}