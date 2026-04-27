package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.domain.R;
import com.hmoob.common.web.core.BaseController;
import com.hmoob.doc.domain.vo.KbDashboardVo;
import com.hmoob.doc.domain.vo.KbDeptStatVo;
import com.hmoob.doc.service.IKbStatService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * KB统计管理
 *
 * @author hmoob
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/kb/stat")
public class KbStatController extends BaseController {

    private final IKbStatService statService;

    /**
     * 获取首页卡片统计数据
     * 包含: 文档总数、已发布数、待处理数、文件总大小、下载次数、浏览次数
     */
    @SaCheckPermission("kb:stat:list")
    @GetMapping("/dashboard")
    public R<KbDashboardVo> getDashboardStat() {
        return R.ok(statService.getDashboardStat());
    }

    /**
     * 获取部门文件明细统计（不分页）
     * 包含: 部门名称、文档数量、文件大小、下载次数、浏览次数
     */
    @SaCheckPermission("kb:stat:list")
    @GetMapping("/dept")
    public R<List<KbDeptStatVo>> getDeptStatList() {
        return R.ok(statService.getDeptStatList());
    }

}
