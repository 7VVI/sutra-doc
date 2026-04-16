package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.satoken.utils.LoginHelper;
import com.hmoob.doc.domain.KbDocVisitRecord;
import com.hmoob.doc.domain.bo.KbDocVisitRecordBo;
import com.hmoob.doc.domain.vo.KbDocVisitRecordVo;
import com.hmoob.doc.mapper.KbDocVisitRecordMapper;
import com.hmoob.doc.service.IKbDocVisitRecordService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KB文档访问记录 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbDocVisitRecordServiceImpl implements IKbDocVisitRecordService {

    private final KbDocVisitRecordMapper baseMapper;

    /**
     * 分页查询访问记录数据
     *
     * @param bo        访问记录信息
     * @param pageQuery 分页对象
     * @return 访问记录信息集合
     */
    @Override
    public TableDataInfo<KbDocVisitRecordVo> selectPageVisitRecordList(KbDocVisitRecordBo bo, PageQuery pageQuery) {
        Page<KbDocVisitRecordVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    /**
     * 查询访问记录数据
     *
     * @param bo 访问记录信息
     * @return 访问记录信息集合
     */
    @Override
    public List<KbDocVisitRecordVo> selectVisitRecordList(KbDocVisitRecordBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构建查询条件
     *
     * @param bo 访问记录信息
     * @return 查询条件
     */
    private LambdaQueryWrapper<KbDocVisitRecord> buildQueryWrapper(KbDocVisitRecordBo bo) {
        LambdaQueryWrapper<KbDocVisitRecord> lqw = Wrappers.lambdaQuery();
        lqw.eq(KbDocVisitRecord::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getVisitId()), KbDocVisitRecord::getVisitId, bo.getVisitId());
        lqw.eq(ObjectUtil.isNotNull(bo.getDocId()), KbDocVisitRecord::getDocId, bo.getDocId());
        lqw.eq(ObjectUtil.isNotNull(bo.getFolderId()), KbDocVisitRecord::getFolderId, bo.getFolderId());
        lqw.eq(ObjectUtil.isNotNull(bo.getVisitType()), KbDocVisitRecord::getVisitType, bo.getVisitType());
        lqw.like(StringUtils.isNotBlank(bo.getKeywords()), KbDocVisitRecord::getKeywords, bo.getKeywords());
        lqw.eq(ObjectUtil.isNotNull(bo.getUserId()), KbDocVisitRecord::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getSourceIp()), KbDocVisitRecord::getSourceIp, bo.getSourceIp());
        lqw.eq(StringUtils.isNotBlank(bo.getDeviceType()), KbDocVisitRecord::getDeviceType, bo.getDeviceType());
        lqw.orderByDesc(KbDocVisitRecord::getCreateTime);
        return lqw;
    }

    /**
     * 根据文档ID查询访问记录列表
     *
     * @param docId 文档ID
     * @return 访问记录列表
     */
    @Override
    public List<KbDocVisitRecordVo> selectVisitRecordByDocId(Long docId) {
        return baseMapper.selectByDocId(docId);
    }

    /**
     * 根据用户ID查询访问记录列表
     *
     * @param userId 用户ID
     * @return 访问记录列表
     */
    @Override
    public List<KbDocVisitRecordVo> selectVisitRecordByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    /**
     * 记录文档访问
     *
     * @param docId     文档ID
     * @param userId    用户ID
     * @param visitType 访问类型: 1-上传, 2-预览, 3-下载, 4-删除, 5-编辑, 6-分享
     * @param sourceIp  来源IP地址
     * @return 访问记录ID
     */
    @Override
    public Long recordVisit(Long docId, Long userId, Integer visitType, String sourceIp) {
        KbDocVisitRecord record = new KbDocVisitRecord();
        record.setDocId(docId);
        record.setUserId(userId);
        record.setVisitType(visitType);
        record.setSourceIp(sourceIp);
        baseMapper.insert(record);
        return record.getVisitId();
    }

    /**
     * 记录文档访问（简化版，自动获取当前用户）
     *
     * @param docId     文档ID
     * @param folderId  目录ID
     * @param visitType 访问类型
     * @param sourceIp  来源IP地址
     */
    @Override
    public void logVisit(Long docId, Long folderId, Integer visitType, String sourceIp) {
        try {
            KbDocVisitRecord record = new KbDocVisitRecord();
            record.setDocId(docId);
            record.setFolderId(folderId);
            record.setVisitType(visitType);
            record.setSourceIp(sourceIp);
            // 自动获取当前登录用户ID
            Long userId = LoginHelper.getUserId();
            if (userId != null) {
                record.setUserId(userId);
            }
            baseMapper.insert(record);
        } catch (Exception e) {
            // 记录访问日志失败不影响主流程
            // 仅记录日志
        }
    }

    /**
     * 获取文档访问统计
     *
     * @param docId 文档ID
     * @return 各访问类型的数量统计
     */
    @Override
    public Map<Integer, Long> getVisitStatistics(Long docId) {
        Map<Integer, Long> statistics = new HashMap<>();
        // 查询各访问类型的数量
        LambdaQueryWrapper<KbDocVisitRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(KbDocVisitRecord::getDocId, docId);
        wrapper.eq(KbDocVisitRecord::getDelFlag, SystemConstants.NORMAL);
        List<KbDocVisitRecord> records = baseMapper.selectList(wrapper);
        // 按访问类型统计
        for (KbDocVisitRecord record : records) {
            Integer visitType = record.getVisitType();
            statistics.merge(visitType, 1L, Long::sum);
        }
        return statistics;
    }

    /**
     * 获取用户最近访问记录
     *
     * @param userId 用户ID
     * @param limit  数量限制
     * @return 最近访问记录列表
     */
    @Override
    public List<KbDocVisitRecordVo> getRecentVisits(Long userId, Integer limit) {
        LambdaQueryWrapper<KbDocVisitRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(KbDocVisitRecord::getUserId, userId);
        wrapper.eq(KbDocVisitRecord::getDelFlag, SystemConstants.NORMAL);
        wrapper.orderByDesc(KbDocVisitRecord::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return baseMapper.selectVoList(wrapper);
    }

    /**
     * 根据访问记录ID查询信息
     *
     * @param visitId 访问记录ID
     * @return 访问记录信息
     */
    @Override
    public KbDocVisitRecordVo selectVisitRecordById(Long visitId) {
        return baseMapper.selectVoById(visitId);
    }

    /**
     * 新增保存访问记录信息
     *
     * @param bo 访问记录信息
     * @return 结果
     */
    @Override
    public int insertVisitRecord(KbDocVisitRecordBo bo) {
        KbDocVisitRecord record = MapstructUtils.convert(bo, KbDocVisitRecord.class);
        return baseMapper.insert(record);
    }

    /**
     * 删除访问记录信息
     *
     * @param visitId 访问记录ID
     * @return 结果
     */
    @Override
    public int deleteVisitRecordById(Long visitId) {
        return baseMapper.deleteById(visitId);
    }

}