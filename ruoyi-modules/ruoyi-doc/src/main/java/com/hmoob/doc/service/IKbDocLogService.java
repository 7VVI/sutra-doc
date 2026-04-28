package com.hmoob.doc.service;

import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.query.KbDocLogQuery;
import com.hmoob.doc.domain.vo.KbDocLogVo;

import java.util.List;

/**
 * KB文档日志 服务层
 *
 * @author hmoob
 */
public interface IKbDocLogService {

    /**
     * 分页查询文档日志
     *
     * @param query 查询参数
     * @return 日志分页列表
     */
    TableDataInfo<KbDocLogVo> selectPageLogList(KbDocLogQuery query);

    /**
     * 查询文档日志列表（不分页，用于导出）
     *
     * @param query 查询参数
     * @return 日志列表
     */
    List<KbDocLogVo> selectLogList(KbDocLogQuery query);

    /**
     * 根据ID列表查询日志（用于导出选中）
     *
     * @param visitIds 日志ID列表
     * @return 日志列表
     */
    List<KbDocLogVo> selectLogByIds(List<Long> visitIds);

    /**
     * 批量删除文档日志
     *
     * @param visitIds 日志ID列表
     * @return 删除数量
     */
    int deleteLogByIds(List<Long> visitIds);

}