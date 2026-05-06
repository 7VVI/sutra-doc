package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.lang.tree.Tree;
import io.swagger.v3.oas.annotations.Parameter;
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
import com.hmoob.doc.domain.bo.KbFolderBo;
import com.hmoob.doc.domain.vo.KbFolderVo;
import com.hmoob.doc.domain.vo.KbDeptDocTreeNodeVo;
import com.hmoob.doc.service.IKbFolderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KB目录管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/folder")
public class KbFolderController extends BaseController {

    private final IKbFolderService folderService;

    /**
     * 查询目录列表
     */
    @SaCheckPermission("kb:folder:list")
    @GetMapping("/list")
    public TableDataInfo<KbFolderVo> list(KbFolderBo bo, PageQuery pageQuery) {
        return folderService.selectPageFolderList(bo, pageQuery);
    }

    /**
     * 查询目录列表(不分页)
     */
    @SaCheckPermission("kb:folder:list")
    @GetMapping("/listAll")
    public R<List<KbFolderVo>> listAll(KbFolderBo bo) {
        return R.ok(folderService.selectFolderList(bo));
    }

    /**
     * 查询目录树结构
     */
    @SaCheckPermission("kb:folder:list")
    @GetMapping("/tree")
    public R<List<Tree<Long>>> tree(KbFolderBo bo) {
        return R.ok(folderService.selectFolderTreeList(bo));
    }

    /**
     * 导出目录列表
     */
    @SaCheckPermission("kb:folder:export")
    @Log(title = "目录管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KbFolderBo bo, HttpServletResponse response) {
        List<KbFolderVo> list = folderService.selectFolderList(bo);
        ExcelUtil.exportExcel(list, "目录管理", KbFolderVo.class, response);
    }

    /**
     * 获取目录详细信息
     *
     * @param folderId 目录ID
     */
    @SaCheckPermission("kb:folder:query")
    @GetMapping("/{folderId}")
    public R<KbFolderVo> getInfo(@NotNull(message = "目录ID不能为空") @PathVariable Long folderId) {
        return R.ok(folderService.selectFolderById(folderId));
    }

    /**
     * 新增目录
     */
    @SaCheckPermission("kb:folder:add")
    @Log(title = "目录管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KbFolderBo bo) {
        if (!folderService.checkFolderNameUnique(bo)) {
            return R.fail("新增目录'" + bo.getFolderName() + "'失败,目录名称已存在");
        }
        return toAjax(folderService.insertFolder(bo));
    }

    /**
     * 修改目录
     */
    @SaCheckPermission("kb:folder:edit")
    @Log(title = "目录管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KbFolderBo bo) {
        if (!folderService.checkFolderNameUnique(bo)) {
            return R.fail("修改目录'" + bo.getFolderName() + "'失败,目录名称已存在");
        }
        return toAjax(folderService.updateFolder(bo));
    }

    /**
     * 删除目录
     *
     * @param folderId 目录ID
     */
    @SaCheckPermission("kb:folder:remove")
    @Log(title = "目录管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{folderId}")
    public R<Void> remove(@PathVariable Long folderId) {
        if (folderService.hasChildByFolderId(folderId)) {
            return R.fail("存在下级目录,不允许删除");
        }
        if (folderService.checkFolderExistDoc(folderId)) {
            return R.fail("目录下存在文档,不允许删除");
        }
        return toAjax(folderService.deleteFolderById(folderId));
    }

    /**
     * 获取目录统计数据
     *
     * @param folderId 目录ID
     */
    @SaCheckPermission("kb:folder:query")
    @GetMapping("/statistics/{folderId}")
    public R<KbFolderVo> statistics(@PathVariable Long folderId) {
        KbFolderVo vo = folderService.selectFolderById(folderId);
        return R.ok(vo);
    }

    /**
     * 查询部门下的文档目录结构（懒加载，每次返回一层）
     * 返回当前层级的子目录和文档，点击时传入 parentId 加载下一层
     *
     * @param deptId   部门ID（不传则使用当前登录用户的部门ID）
     * @param parentId 父目录ID（不传默认0，表示根目录）
     * @return 当前层级的目录和文档节点列表
     */
    @SaCheckPermission("kb:folder:list")
    @GetMapping("/deptDocTree")
    public R<List<KbDeptDocTreeNodeVo>> deptDocTree(
        @Parameter(description = "部门ID（不传则使用当前用户部门）") @RequestParam(required = false) Long deptId,
        @Parameter(description = "父目录ID（默认0表示根目录）") @RequestParam(required = false, defaultValue = "0") Long parentId) {
        return R.ok(folderService.selectDeptDocTree(deptId, parentId));
    }

    /**
     * 懒加载查询子目录列表（每次返回一层，仅目录，含 hasChildren）
     *
     * @param parentId 父目录ID（不传默认0，表示根目录）
     * @return 当前层级的目录列表
     */
    @SaCheckPermission("kb:folder:list")
    @GetMapping("/lazyTree")
    public R<List<KbFolderVo>> lazyList(
        @Parameter(description = "父目录ID（默认0表示根目录）") @RequestParam(required = false, defaultValue = "0") Long parentId) {
        return R.ok(folderService.selectFolderLazyList(parentId));
    }

}
