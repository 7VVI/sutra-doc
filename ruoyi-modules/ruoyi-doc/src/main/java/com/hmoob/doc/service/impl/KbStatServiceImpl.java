package com.hmoob.doc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hmoob.common.core.utils.SpringUtils;
import com.hmoob.doc.domain.KbDoc;
import com.hmoob.doc.domain.KbFile;
import com.hmoob.doc.domain.KbSearchRecord;
import com.hmoob.doc.domain.vo.KbDashboardVo;
import com.hmoob.doc.domain.vo.KbDeptStatVo;
import com.hmoob.doc.mapper.KbDocMapper;
import com.hmoob.doc.mapper.KbFileMapper;
import com.hmoob.doc.mapper.KbSearchRecordMapper;
import com.hmoob.doc.service.IKbStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * KB统计 服务实现
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KbStatServiceImpl implements IKbStatService {

    private final KbFileMapper fileMapper;
    private final KbDocMapper docMapper;
    private final KbSearchRecordMapper searchRecordMapper;

    /**
     * 获取首页卡片统计数据
     */
    @Override
    public KbDashboardVo getDashboardStat() {
        KbDashboardVo vo = new KbDashboardVo();

        // 1. 文件总数 + 环比上月增长
        long fileTotal = fileMapper.selectCount(Wrappers.<KbFile>lambdaQuery());
        vo.setFileTotal(fileTotal);
        vo.setFileTotalGrowthRate(calcMonthlyGrowthRate(fileMapper,
            KbFile::getCreateTime));

        // 2. 本周新增文件数 + 环比上周增长
        LocalDateTime thisWeekStart = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime lastWeekStart = thisWeekStart.minusWeeks(1);

        long weeklyNewFiles = fileMapper.selectCount(Wrappers.<KbFile>lambdaQuery()
            .ge(KbFile::getCreateTime, toDate(thisWeekStart)));
        vo.setWeeklyNewFiles(weeklyNewFiles);

        long lastWeekFiles = fileMapper.selectCount(Wrappers.<KbFile>lambdaQuery()
            .ge(KbFile::getCreateTime, toDate(lastWeekStart))
            .lt(KbFile::getCreateTime, toDate(thisWeekStart)));
        vo.setWeeklyGrowthRate(calcGrowthRate(weeklyNewFiles, lastWeekFiles));

        // 3. 活跃部门数量(最近一周有文件上传的部门)
        List<KbDoc> docs = docMapper.selectList(Wrappers.<KbDoc>lambdaQuery()
            .select(KbDoc::getDepId)
            .isNotNull(KbDoc::getDepId)
            .ge(KbDoc::getCreateTime, toDate(thisWeekStart)));
        Set<Long> deptIds = docs.stream()
            .map(KbDoc::getDepId)
            .filter(ObjectUtil::isNotNull)
            .collect(Collectors.toSet());
        vo.setActiveDeptCount((long) deptIds.size());

        // 4. 今日检索次数累计
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todaySearchCount = searchRecordMapper.selectCount(Wrappers.<KbSearchRecord>lambdaQuery()
            .ge(KbSearchRecord::getCreateTime, toDate(todayStart)));
        vo.setTodaySearchCount(todaySearchCount);

        return vo;
    }

    /**
     * 获取部门文件明细统计（不分页）
     * 所有部门都返回，没有文档的部门统计值为0
     */
    @Override
    public List<KbDeptStatVo> getDeptStatList() {
        // 上周一的起始时间
        LocalDateTime thisWeekStart = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();

        // 1. 获取所有部门
        Map<Long, String> allDeptMap = getAllDeptMap();
        if (CollUtil.isEmpty(allDeptMap)) {
            return new ArrayList<>();
        }

        // 2. 查询所有有部门ID的文档
        List<KbDoc> allDocs = docMapper.selectList(Wrappers.<KbDoc>lambdaQuery()
            .isNotNull(KbDoc::getDepId));

        // 3. 按部门ID分组
        Map<Long, List<KbDoc>> deptDocMap = CollUtil.isEmpty(allDocs)
            ? new java.util.HashMap<>()
            : allDocs.stream()
                .filter(d -> d.getDepId() != null)
                .collect(Collectors.groupingBy(KbDoc::getDepId));

        // 4. 遍历所有部门，组装结果（没有文档的部门也返回统计值为0）
        return allDeptMap.entrySet().stream().map(entry -> {
            Long deptId = entry.getKey();
            String deptName = entry.getValue();
            List<KbDoc> docs = deptDocMap.getOrDefault(deptId, new ArrayList<>());

            KbDeptStatVo vo = new KbDeptStatVo();
            vo.setDeptId(deptId);
            vo.setDeptName(deptName);
            vo.setFileCount((long) docs.size());

            // 比上周新增数量(本周创建的文档)
            long weeklyNewCount = docs.stream()
                .filter(d -> d.getCreateTime() != null
                    && d.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    .isAfter(thisWeekStart))
                .count();
            vo.setWeeklyNewCount(weeklyNewCount);

            // 最新新增文件名(按创建时间倒序取第一个)
            String latestFileName = docs.stream()
                .filter(d -> d.getCreateTime() != null)
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .map(KbDoc::getDocName)
                .findFirst()
                .orElse(null);
            vo.setLatestFileName(latestFileName);

            return vo;
        }).sorted((a, b) -> Long.compare(b.getFileCount(), a.getFileCount()))
          .collect(Collectors.toList());
    }

    /**
     * 计算环比上月增长百分比
     * 对比: 本月新增 vs 上月新增
     */
    private <T> Double calcMonthlyGrowthRate(
        com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
        com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> timeField) {
        LocalDate today = LocalDate.now();
        LocalDateTime thisMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);

        long thisMonthCount = mapper.selectCount(Wrappers.<T>lambdaQuery()
            .ge(timeField, toDate(thisMonthStart)));
        long lastMonthCount = mapper.selectCount(Wrappers.<T>lambdaQuery()
            .ge(timeField, toDate(lastMonthStart))
            .lt(timeField, toDate(thisMonthStart)));

        return calcGrowthRate(thisMonthCount, lastMonthCount);
    }

    /**
     * 计算增长百分比
     */
    private Double calcGrowthRate(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        double rate = ((double) (current - previous) / previous) * 100;
        return Math.round(rate * 100.0) / 100.0;
    }

    /**
     * LocalDateTime -> Date
     */
    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取所有部门ID和名称的映射（通过反射调用系统服务）
     */
    @SuppressWarnings("unchecked")
    private Map<Long, String> getAllDeptMap() {
        try {
            Object deptService = SpringUtils.getBean(
                Class.forName("com.hmoob.system.service.ISysDeptService"));
            Object deptBo = Class.forName("com.hmoob.system.domain.bo.SysDeptBo")
                .getDeclaredConstructor().newInstance();
            List<?> deptVos = (List<?>) deptService.getClass()
                .getMethod("selectDeptList", Class.forName("com.hmoob.system.domain.bo.SysDeptBo"))
                .invoke(deptService, deptBo);
            return ((List<?>) deptVos).stream()
                .collect(Collectors.toMap(
                    d -> {
                        try { return (Long) d.getClass().getMethod("getDeptId").invoke(d); }
                        catch (Exception e) { return 0L; }
                    },
                    d -> {
                        try { return (String) d.getClass().getMethod("getDeptName").invoke(d); }
                        catch (Exception e) { return "未知部门"; }
                    },
                    (a, b) -> a));
        } catch (Exception e) {
            log.warn("查询所有部门失败: {}", e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    /**
     * 批量查询部门名称
     */
    @SuppressWarnings("unchecked")
    private Map<Long, String> getDeptNameMap(List<Long> deptIds) {
        try {
            Object deptService = SpringUtils.getBean(
                Class.forName("com.hmoob.system.service.ISysDeptService"));
            List<?> deptVos = (List<?>) deptService.getClass()
                .getMethod("selectDeptByIds", List.class)
                .invoke(deptService, deptIds);

            return ((List<?>) deptVos).stream()
                .collect(Collectors.toMap(
                    d -> {
                        try { return (Long) d.getClass().getMethod("getDeptId").invoke(d); }
                        catch (Exception e) { return 0L; }
                    },
                    d -> {
                        try { return (String) d.getClass().getMethod("getDeptName").invoke(d); }
                        catch (Exception e) { return "未知部门"; }
                    },
                    (a, b) -> a));
        } catch (Exception e) {
            log.warn("查询部门名称失败: {}", e.getMessage());
            return deptIds.stream()
                .collect(Collectors.toMap(id -> id, id -> "部门" + id, (a, b) -> a));
        }
    }

}
