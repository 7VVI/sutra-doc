package com.hmoob.doc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.domain.dto.UserDTO;
import com.hmoob.common.core.service.UserService;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.KbDocVisitRecord;
import com.hmoob.doc.domain.query.KbDocLogQuery;
import com.hmoob.doc.domain.vo.KbDocLogVo;
import com.hmoob.doc.mapper.KbDocVisitRecordMapper;
import com.hmoob.doc.service.IKbDocLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * KB文档日志 服务实现
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KbDocLogServiceImpl implements IKbDocLogService {

    private final KbDocVisitRecordMapper baseMapper;
    private final UserService userService;

    /**
     * 操作类型映射
     */
    private static final Map<Integer, String> VISIT_TYPE_MAP = Map.of(
        1, "上传",
        2, "预览",
        3, "下载",
        4, "删除",
        5, "编辑",
        6, "分享"
    );

    /**
     * 分页查询文档日志
     *
     * @param query 查询参数
     * @return 日志分页列表
     */
    @Override
    public TableDataInfo<KbDocLogVo> selectPageLogList(KbDocLogQuery query) {
        LambdaQueryWrapper<KbDocVisitRecord> wrapper = buildQueryWrapper(query);
        Page<KbDocVisitRecord> page = baseMapper.selectPage(query.build(), wrapper);

        // 转换并填充用户名
        List<KbDocLogVo> voList = convertAndFillUserInfo(page.getRecords());

        // 设置序号
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        for (int i = 0; i < voList.size(); i++) {
            voList.get(i).setRowNum(offset + i + 1);
        }

        return new TableDataInfo<>(voList, page.getTotal());
    }

    /**
     * 查询文档日志列表（不分页，用于导出）
     *
     * @param query 查询参数
     * @return 日志列表
     */
    @Override
    public List<KbDocLogVo> selectLogList(KbDocLogQuery query) {
        LambdaQueryWrapper<KbDocVisitRecord> wrapper = buildQueryWrapper(query);
        // 导出限制最多10000条
        wrapper.last("LIMIT 10000");
        List<KbDocVisitRecord> records = baseMapper.selectList(wrapper);
        List<KbDocLogVo> voList = convertAndFillUserInfo(records);

        // 设置序号
        for (int i = 0; i < voList.size(); i++) {
            voList.get(i).setRowNum(i + 1);
        }
        return voList;
    }

    /**
     * 根据ID列表查询日志（用于导出选中）
     *
     * @param visitIds 日志ID列表
     * @return 日志列表
     */
    @Override
    public List<KbDocLogVo> selectLogByIds(List<Long> visitIds) {
        if (CollUtil.isEmpty(visitIds)) {
            return Collections.emptyList();
        }
        List<KbDocVisitRecord> records = baseMapper.selectBatchIds(visitIds);
        List<KbDocLogVo> voList = convertAndFillUserInfo(records);

        // 设置序号
        for (int i = 0; i < voList.size(); i++) {
            voList.get(i).setRowNum(i + 1);
        }
        return voList;
    }

    /**
     * 批量删除文档日志
     *
     * @param visitIds 日志ID列表
     * @return 删除数量
     */
    @Override
    public int deleteLogByIds(List<Long> visitIds) {
        if (CollUtil.isEmpty(visitIds)) {
            return 0;
        }
        return baseMapper.deleteBatchIds(visitIds);
    }

    /**
     * 构建查询条件
     *
     * @param query 查询参数
     * @return 查询条件
     */
    private LambdaQueryWrapper<KbDocVisitRecord> buildQueryWrapper(KbDocLogQuery query) {
        LambdaQueryWrapper<KbDocVisitRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(KbDocVisitRecord::getDelFlag, SystemConstants.NORMAL);

        // 时间范围
        if (query.getCreateTimeStart() != null) {
            wrapper.ge(KbDocVisitRecord::getCreateTime, query.getCreateTimeStart());
        }
        if (query.getCreateTimeEnd() != null) {
            wrapper.le(KbDocVisitRecord::getCreateTime, query.getCreateTimeEnd());
        }

        // 操作类型
        if (query.getVisitType() != null) {
            wrapper.eq(KbDocVisitRecord::getVisitType, query.getVisitType());
        }

        // 操作人ID
        if (query.getUserId() != null) {
            wrapper.eq(KbDocVisitRecord::getUserId, query.getUserId());
        }

        // 按时间倒序
        wrapper.orderByDesc(KbDocVisitRecord::getCreateTime);

        return wrapper;
    }

    /**
     * 转换记录并填充用户名
     *
     * @param records 访问记录列表
     * @return 日志VO列表
     */
    private List<KbDocLogVo> convertAndFillUserInfo(List<KbDocVisitRecord> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }

        // 收集用户ID
        List<Long> userIds = records.stream()
            .map(KbDocVisitRecord::getUserId)
            .filter(ObjectUtil::isNotNull)
            .distinct()
            .collect(Collectors.toList());

        // 批量查询用户名
        Map<Long, String> userNameMap = getUserNameMap(userIds);

        // 转换
        List<KbDocLogVo> voList = new ArrayList<>();
        for (KbDocVisitRecord record : records) {
            KbDocLogVo vo = new KbDocLogVo();
            vo.setVisitId(record.getVisitId());
            vo.setVisitType(record.getVisitType());
            vo.setVisitTypeText(VISIT_TYPE_MAP.getOrDefault(record.getVisitType(), "未知"));
            vo.setUserId(record.getUserId());
            vo.setUserName(record.getUserId() != null ?
                userNameMap.getOrDefault(record.getUserId(), "未知用户") : "匿名用户");
            vo.setKeywords(StrUtil.isNotBlank(record.getKeywords()) ? record.getKeywords() :
                StrUtil.isNotBlank(record.getKeywordsMax()) ? record.getKeywordsMax() : "");
            vo.setSourceIp(record.getSourceIp());
            vo.setCreateTime(record.getCreateTime());
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 批量查询用户名称
     *
     * @param userIds 用户ID列表
     * @return 用户ID-名称映射
     */
    private Map<Long, String> getUserNameMap(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        try {
            // 使用 UserService 批量查询用户信息
            List<UserDTO> users = userService.selectListByIds(userIds);
            
            // 转换为用户ID-昵称映射
            return users.stream()
                .collect(Collectors.toMap(
                    UserDTO::getUserId,
                    user -> StrUtil.isNotBlank(user.getNickName()) ? user.getNickName() : "未知用户",
                    (a, b) -> a));
        } catch (Exception e) {
            log.warn("查询用户名称失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

}