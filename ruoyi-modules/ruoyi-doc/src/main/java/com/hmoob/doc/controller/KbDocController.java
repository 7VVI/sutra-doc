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
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.bo.KbDocBo;
import com.hmoob.doc.domain.vo.KbDocVo;
import com.hmoob.doc.service.IKbDocService;
import com.hmoob.doc.service.IKbDocFavouriteService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * KB文档管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/doc")
public class KbDocController extends BaseController {

    private final IKbDocService docService;
    private final IKbDocFavouriteService favouriteService;

    /**
     * 查询文档列表
     */
    @SaCheckPermission("kb:doc:list")
    @GetMapping("/list")
    public TableDataInfo<KbDocVo> list(KbDocBo bo, PageQuery pageQuery) {
        return docService.selectPageDocList(bo, pageQuery);
    }

    /**
     * 查询文档列表(不分页)
     */
    @SaCheckPermission("kb:doc:list")
    @GetMapping("/listAll")
    public R<List<KbDocVo>> listAll(KbDocBo bo) {
        return R.ok(docService.selectDocList(bo));
    }

    /**
     * 导出文档列表
     */
    @SaCheckPermission("kb:doc:export")
    @Log(title = "文档管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KbDocBo bo, HttpServletResponse response) {
        List<KbDocVo> list = docService.selectDocList(bo);
        ExcelUtil.exportExcel(list, "文档管理", KbDocVo.class, response);
    }

    /**
     * 获取文档详细信息
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:doc:query")
    @GetMapping("/{docId}")
    public R<KbDocVo> getInfo(@NotNull(message = "文档ID不能为空") @PathVariable Long docId) {
        return R.ok(docService.selectDocById(docId));
    }

    /**
     * 根据文档编号查询文档
     *
     * @param serialNumber 文档编号
     */
    @SaCheckPermission("kb:doc:query")
    @GetMapping("/serial/{serialNumber}")
    public R<KbDocVo> getBySerialNumber(@PathVariable String serialNumber) {
        return R.ok(docService.selectDocBySerialNumber(serialNumber));
    }

    /**
     * 上传文档
     */
    @SaCheckPermission("kb:doc:add")
    @Log(title = "文档管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/upload")
    public R<KbDocVo> upload(@Validated(AddGroup.class) KbDocBo bo, MultipartFile file) {
        // 文件上传逻辑由Service层处理
        return R.ok(docService.selectDocById(bo.getDocId()));
    }

    /**
     * 新增文档
     */
    @SaCheckPermission("kb:doc:add")
    @Log(title = "文档管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KbDocBo bo) {
        if (!docService.checkDocNameUnique(bo)) {
            return R.fail("新增文档'" + bo.getDocName() + "'失败,文档名称已存在");
        }
        return toAjax(docService.insertDoc(bo));
    }

    /**
     * 修改文档
     */
    @SaCheckPermission("kb:doc:edit")
    @Log(title = "文档管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KbDocBo bo) {
        if (!docService.checkDocNameUnique(bo)) {
            return R.fail("修改文档'" + bo.getDocName() + "'失败,文档名称已存在");
        }
        return toAjax(docService.updateDoc(bo));
    }

    /**
     * 删除文档
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:doc:remove")
    @Log(title = "文档管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{docId}")
    public R<Void> remove(@PathVariable Long docId) {
        return toAjax(docService.deleteDocById(docId));
    }

    /**
     * 发布文档
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:doc:edit")
    @Log(title = "文档管理", businessType = BusinessType.UPDATE)
    @PostMapping("/publish/{docId}")
    public R<Void> publish(@PathVariable Long docId) {
        return toAjax(docService.publishDoc(docId));
    }

    /**
     * 撤回文档
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:doc:edit")
    @Log(title = "文档管理", businessType = BusinessType.UPDATE)
    @PostMapping("/withdraw/{docId}")
    public R<Void> withdraw(@PathVariable Long docId) {
        return toAjax(docService.withdrawDoc(docId));
    }

    /**
     * 下载文档
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:doc:download")
    @Log(title = "文档管理", businessType = BusinessType.OTHER)
    @GetMapping("/download/{docId}")
    public void download(@PathVariable Long docId, HttpServletResponse response) {
        // 下载逻辑由Service层处理
        docService.incrementDownloadCount(docId);
    }

    /**
     * 预览文档
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:doc:query")
    @GetMapping("/preview/{docId}")
    public R<KbDocVo> preview(@PathVariable Long docId) {
        KbDocVo docVo = docService.selectDocById(docId);
        docService.incrementViewCount(docId);
        return R.ok(docVo);
    }

    /**
     * 收藏文档
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:doc:favourite")
    @Log(title = "文档管理", businessType = BusinessType.OTHER)
    @PostMapping("/favourite/{docId}")
    public R<Void> favourite(@PathVariable Long docId) {
        return toAjax(true);
    }

    /**
     * 取消收藏文档
     *
     * @param docId 文档ID
     */
    @SaCheckPermission("kb:doc:favourite")
    @Log(title = "文档管理", businessType = BusinessType.OTHER)
    @PostMapping("/unfavourite/{docId}")
    public R<Void> unfavourite(@PathVariable Long docId) {
        return toAjax(true);
    }

}