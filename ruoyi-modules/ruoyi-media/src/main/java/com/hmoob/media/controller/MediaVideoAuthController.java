package com.hmoob.media.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.media.domain.bo.MediaVideoAuthBo;
import com.hmoob.media.domain.vo.MediaVideoAuthVo;
import com.hmoob.media.service.IMediaVideoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 视频授权管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/media/auth")
public class MediaVideoAuthController extends BaseController {

    private final IMediaVideoAuthService authService;

    /** 新增授权 */
    @SaCheckPermission("media:auth:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody MediaVideoAuthBo bo) {
        return toAjax(authService.insertAuth(bo));
    }

    /** 删除授权 */
    @SaCheckPermission("media:auth:remove")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(authService.deleteAuth(id));
    }

    /** 查询视频授权列表 */
    @GetMapping("/{videoId}")
    public TableDataInfo<MediaVideoAuthVo> list(@PathVariable Long videoId, PageQuery pageQuery) {
        return authService.selectAuthPage(videoId, pageQuery);
    }
}
