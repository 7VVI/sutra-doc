package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.KbSearchRecord;
import com.hmoob.doc.domain.bo.KbSearchRecordBo;
import com.hmoob.doc.domain.vo.KbSearchRecordVo;
import com.hmoob.doc.mapper.KbSearchRecordMapper;
import com.hmoob.doc.service.IKbSearchRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * KB搜索记录 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbSearchRecordServiceImpl implements IKbSearchRecordService {

    private final KbSearchRecordMapper baseMapper;

    /**
     * 分页查询搜索记录数据
     *
     * @param bo        搜索记录信息
     * @param pageQuery 分页对象
     * @return 搜索记录信息集合
     */
    @Override
    public TableDataInfo<KbSearchRecordVo> selectPageSearchRecordList(KbSearchRecordBo bo, PageQuery pageQuery) {
        Page<KbSearchRecordVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    /**
     * 查询搜索记录数据
     *
     * @param bo 搜索记录信息
     * @return 搜索记录信息集合
     */
    @Override
    public List<KbSearchRecordVo> selectSearchRecordList(KbSearchRecordBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构建查询条件
     *
     * @param bo 搜索记录信息
     * @return 查询条件
     */
    private LambdaQueryWrapper<KbSearchRecord> buildQueryWrapper(KbSearchRecordBo bo) {
        LambdaQueryWrapper<KbSearchRecord> lqw = Wrappers.lambdaQuery();
        lqw.eq(KbSearchRecord::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getSearchId()), KbSearchRecord::getSearchId, bo.getSearchId());
        lqw.like(StringUtils.isNotBlank(bo.getKeywords()), KbSearchRecord::getKeywords, bo.getKeywords());
        lqw.eq(ObjectUtil.isNotNull(bo.getSearchType()), KbSearchRecord::getSearchType, bo.getSearchType());
        lqw.eq(ObjectUtil.isNotNull(bo.getUserId()), KbSearchRecord::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getSourceIp()), KbSearchRecord::getSourceIp, bo.getSourceIp());
        lqw.orderByDesc(KbSearchRecord::getCreateTime);
        return lqw;
    }

    /**
     * 根据用户ID查询搜索记录列表
     *
     * @param userId 用户ID
     * @return 搜索记录列表
     */
    @Override
    public List<KbSearchRecordVo> selectSearchRecordByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    /**
     * 记录搜索操作
     *
     * @param keywords    搜索关键词
     * @param userId      用户ID
     * @param resultCount 搜索结果数量
     * @return 搜索记录ID
     */
    @Override
    public Long recordSearch(String keywords, Long userId, Integer resultCount) {
        KbSearchRecord record = new KbSearchRecord();
        record.setKeywords(keywords);
        record.setUserId(userId);
        record.setResultCount(resultCount);
        record.setSearchType(1); // 默认为关键词搜索
        baseMapper.insert(record);
        return record.getSearchId();
    }

    /**
     * 获取热门搜索关键词
     *
     * @param limit 数量限制
     * @param days  时间范围(最近N天)
     * @return 热门关键词列表
     */
    @Override
    public List<String> getHotKeywords(Integer limit, Integer days) {
        // 计算时间范围
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        // 查询热门关键词
        LambdaQueryWrapper<KbSearchRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(KbSearchRecord::getDelFlag, SystemConstants.NORMAL);
        wrapper.isNotNull(KbSearchRecord::getKeywords);
        wrapper.ge(KbSearchRecord::getCreateTime, startTime);
        wrapper.groupBy(KbSearchRecord::getKeywords);
        wrapper.orderByDesc(KbSearchRecord::getCreateTime);
        wrapper.last("LIMIT " + limit);
        List<KbSearchRecordVo> records = baseMapper.selectVoList(wrapper);
        return records.stream()
            .map(KbSearchRecordVo::getKeywords)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 清空用户搜索历史
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public int clearUserSearchHistory(Long userId) {
        LambdaUpdateWrapper<KbSearchRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(KbSearchRecord::getUserId, userId);
        wrapper.set(KbSearchRecord::getDelFlag, SystemConstants.DISABLE);
        return baseMapper.update(null, wrapper);
    }

    /**
     * 根据搜索记录ID查询信息
     *
     * @param searchId 搜索记录ID
     * @return 搜索记录信息
     */
    @Override
    public KbSearchRecordVo selectSearchRecordById(Long searchId) {
        return baseMapper.selectVoById(searchId);
    }

    /**
     * 新增保存搜索记录信息
     *
     * @param bo 搜索记录信息
     * @return 结果
     */
    @Override
    public int insertSearchRecord(KbSearchRecordBo bo) {
        KbSearchRecord record = MapstructUtils.convert(bo, KbSearchRecord.class);
        return baseMapper.insert(record);
    }

    /**
     * 删除搜索记录信息
     *
     * @param searchId 搜索记录ID
     * @return 结果
     */
    @Override
    public int deleteSearchRecordById(Long searchId) {
        return baseMapper.deleteById(searchId);
    }

}