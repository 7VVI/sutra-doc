package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.common.satoken.utils.LoginHelper;
import com.hmoob.doc.domain.KbSearchRecord;
import com.hmoob.doc.domain.bo.KbSearchRecordBo;
import com.hmoob.doc.domain.vo.KbSearchRecordVo;
import com.hmoob.doc.mapper.KbSearchRecordMapper;
import com.hmoob.doc.service.IKbSearchRecordService;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * KB搜索记录 服务实现
 *
 * @author hmoob
 */
@Slf4j
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

    /**
     * 获取用户搜索历史
     *
     * @param userId 用户ID
     * @param limit  数量限制
     * @return 搜索历史列表
     */
    @Override
    public List<KbSearchRecordVo> getUserSearchHistory(Long userId, Integer limit) {
        if (userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<KbSearchRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(KbSearchRecord::getDelFlag, SystemConstants.NORMAL);
        wrapper.eq(KbSearchRecord::getUserId, userId);
        wrapper.isNotNull(KbSearchRecord::getKeywords);
        wrapper.orderByDesc(KbSearchRecord::getCreateTime);
        wrapper.last("LIMIT " + (limit != null ? limit : 20));
        return baseMapper.selectVoList(wrapper);
    }

    /**
     * 记录搜索日志(完整参数)
     *
     * @param keywords      搜索关键词
     * @param resultCount   搜索结果数量
     * @param topicCodes    主题编码
     * @param businessTypes 业务类型
     * @param folderIds     文件夹ID
     * @param req           HTTP请求对象
     */
    @Override
    public void logSearch(String keywords, Long resultCount, String[] topicCodes,
                          String[] businessTypes, Long[] folderIds, HttpServletRequest req) {
        try {
            KbSearchRecord record = new KbSearchRecord();
            record.setKeywords(keywords);
            record.setResultCount(resultCount != null ? resultCount.intValue() : 0);
            record.setSearchType(1); // 关键词搜索

            // 设置主题编码
            if (topicCodes != null && topicCodes.length > 0) {
                record.setTopicCodes(Arrays.stream(topicCodes).collect(Collectors.joining(",")));
            }

            // 设置业务类型
            if (businessTypes != null && businessTypes.length > 0) {
                record.setBusinessTypes(Arrays.stream(businessTypes).collect(Collectors.joining(",")));
            }

            // 设置文件夹ID
            if (folderIds != null && folderIds.length > 0) {
                record.setFolderIds(Arrays.stream(folderIds).map(String::valueOf).collect(Collectors.joining(",")));
            }

            // 设置来源IP
            if (req != null) {
                String sourceIp = getClientIpAddress(req);
                record.setSourceIp(sourceIp);
            }

            // 设置用户ID
            Long userId = LoginHelper.getUserId();
            if (userId != null) {
                record.setUserId(userId);
            }

            baseMapper.insert(record);
        } catch (Exception e) {
            log.warn("记录搜索日志失败: {}", e.getMessage());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}