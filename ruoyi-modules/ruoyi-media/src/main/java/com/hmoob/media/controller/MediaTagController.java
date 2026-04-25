package com.hmoob.media.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.media.domain.bo.MediaTagBo;
import com.hmoob.media.domain.vo.MediaTagVo;
import com.hmoob.media.service.IMediaTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 分类标签管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/media/tag")
public class MediaTagController extends BaseController {

    private final IMediaTagService tagService;

    /** 查询标签列表 */
    @GetMapping("/list")
    public R<List<MediaTagVo>> list(@RequestParam(required = false) Integer type) {
        return R.ok(tagService.selectTagList(type));
    }

    /** 查询标签树 */
    @GetMapping("/tree")
    public R<List<MediaTagVo>> tree(@RequestParam(required = false) Integer type) {
        return R.ok(tagService.selectTagTree(type));
    }

    /** 新增标签 */
    @SaCheckPermission("media:tag:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody MediaTagBo bo) {
        return toAjax(tagService.insertTag(bo));
    }

    /** 修改标签 */
    @SaCheckPermission("media:tag:edit")
    @PutMapping("/{tagId}")
    public R<Void> edit(@Validated @RequestBody MediaTagBo bo) {
        return toAjax(tagService.updateTag(bo));
    }

    /** 删除标签 */
    @SaCheckPermission("media:tag:remove")
    @DeleteMapping("/{tagId}")
    public R<Void> remove(@PathVariable Long tagId) {
        return toAjax(tagService.deleteTag(tagId));
    }
}
