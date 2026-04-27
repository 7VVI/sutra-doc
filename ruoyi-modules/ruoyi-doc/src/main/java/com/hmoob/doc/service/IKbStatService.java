package com.hmoob.doc.service;

import com.hmoob.doc.domain.vo.KbDashboardVo;
import com.hmoob.doc.domain.vo.KbDeptStatVo;

import java.util.List;

/**
 * KB统计 服务层
 *
 * @author hmoob
 */
public interface IKbStatService {

    /**
     * 获取首页卡片统计数据
     *
     * @return 卡片统计
     */
    KbDashboardVo getDashboardStat();

    /**
     * 获取部门文件明细统计（不分页）
     *
     * @return 部门统计列表
     */
    List<KbDeptStatVo> getDeptStatList();

}
