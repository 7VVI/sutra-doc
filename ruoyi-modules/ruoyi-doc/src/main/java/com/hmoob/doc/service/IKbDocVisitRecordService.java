package com.hmoob.doc.service;

import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.bo.KbDocVisitRecordBo;
import com.hmoob.doc.domain.vo.KbDocVisitRecordVo;

import java.util.List;
import java.util.Map;

/**
 * KB文档访问记录 服务层
 *
 * @author hmoob
 */
public interface IKbDocVisitRecordService {

    /**
     * 分页查询访问记录数据
     *
     * @param bo        访问记录信息
     * @param pageQuery 分页对象
     * @return 访问记录信息集合
     */
    TableDataInfo<KbDocVisitRecordVo> selectPageVisitRecordList(KbDocVisitRecordBo bo, PageQuery pageQuery);

    /**
     * 查询访问记录数据
     *
     * @param bo 访问记录信息
     * @return 访问记录信息集合
     */
    List<KbDocVisitRecordVo> selectVisitRecordList(KbDocVisitRecordBo bo);

    /**
     * 根据文档ID查询访问记录列表
     *
     * @param docId 文档ID
     * @return 访问记录列表
     */
    List<KbDocVisitRecordVo> selectVisitRecordByDocId(Long docId);

    /**
     * 根据用户ID查询访问记录列表
     *
     * @param userId 用户ID
     * @return 访问记录列表
     */
    List<KbDocVisitRecordVo> selectVisitRecordByUserId(Long userId);

    /**
     * 记录文档访问
     *
     * @param docId     文档ID
     * @param userId    用户ID
     * @param visitType 访问类型: 1-上传, 2-预览, 3-下载, 4-删除, 5-编辑, 6-分享
     * @param sourceIp  来源IP地址
     * @return 访问记录ID
     */
    Long recordVisit(Long docId, Long userId, Integer visitType, String sourceIp);

    /**
     * 记录文档访问（简化版，自动获取当前用户）
     *
     * @param docId     文档ID
     * @param folderId  目录ID
     * @param visitType 访问类型
     * @param sourceIp  来源IP地址
     */
    void logVisit(Long docId, Long folderId, Integer visitType, String sourceIp);

    /**
     * 获取文档访问统计
     *
     * @param docId 文档ID
     * @return 各访问类型的数量统计
     */
    Map<Integer, Long> getVisitStatistics(Long docId);

    /**
     * 获取用户最近访问记录
     *
     * @param userId 用户ID
     * @param limit  数量限制
     * @return 最近访问记录列表
     */
    List<KbDocVisitRecordVo> getRecentVisits(Long userId, Integer limit);

    /**
     * 根据访问记录ID查询信息
     *
     * @param visitId 访问记录ID
     * @return 访问记录信息
     */
    KbDocVisitRecordVo selectVisitRecordById(Long visitId);

    /**
     * 新增保存访问记录信息
     *
     * @param bo 访问记录信息
     * @return 结果
     */
    int insertVisitRecord(KbDocVisitRecordBo bo);

    /**
     * 删除访问记录信息
     *
     * @param visitId 访问记录ID
     * @return 结果
     */
    int deleteVisitRecordById(Long visitId);

}