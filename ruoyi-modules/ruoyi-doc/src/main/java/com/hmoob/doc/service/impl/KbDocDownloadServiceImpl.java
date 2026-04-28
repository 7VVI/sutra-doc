package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.hmoob.common.core.utils.SpringUtils;
import com.hmoob.common.redis.utils.RedisUtils;
import com.hmoob.common.satoken.utils.LoginHelper;
import com.hmoob.common.tenant.helper.TenantHelper;
import com.hmoob.doc.domain.KbDoc;
import com.hmoob.doc.event.KbDownloadLimitExceedEvent;
import com.hmoob.doc.mapper.KbDocMapper;
import com.hmoob.doc.service.IKbDocDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * KB文档下载限制 服务实现
 * 使用Redis原子计数器统计用户每日下载次数
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KbDocDownloadServiceImpl implements IKbDocDownloadService {

    private final KbDocMapper docMapper;

    /**
     * Redis key前缀: 用户每日全局下载计数
     * 格式: kb:dl:global:{tenantId}:{userId}:{yyyyMMdd}
     */
    private static final String KEY_PREFIX_GLOBAL = "kb:dl:global:";

    /**
     * 用户每日全局下载次数限制(默认)
     */
    private static final int DEFAULT_GLOBAL_LIMIT = 50;

    /**
     * 获取当天剩余秒数，用于设置Redis key过期时间
     */
    private static Duration getTodayRemainDuration() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        return Duration.between(now, endOfDay);
    }

    /**
     * 构建用户每日全局下载计数的Redis key
     */
    private String buildGlobalKey(Long userId) {
        return KEY_PREFIX_GLOBAL + ":" + userId + ":" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }

    /**
     * 检查用户是否可以下载
     * 判断用户当日全局下载次数是否超限
     */
    @Override
    public boolean checkDownloadAllowed(Long userId, Long docId) {
        String key = buildGlobalKey(userId);
        Integer currentCount = ObjectUtil.defaultIfNull(RedisUtils.<Integer>getCacheObject(key), 0);
        return currentCount < DEFAULT_GLOBAL_LIMIT;
    }

    /**
     * 记录一次下载并递增计数
     */
    @Override
    public long recordDownload(Long userId, Long docId) {
        String key = buildGlobalKey(userId);
        long count = RedisUtils.incrAtomicValue(key);
        // 首次计数时设置过期时间（到当天结束）
        if (count == 1) {
            RedisUtils.expire(key, getTodayRemainDuration());
        }

        // 超限时发布事件（仅在刚好等于限制值时触发，避免重复发布）
        if (count == DEFAULT_GLOBAL_LIMIT) {
            publishLimitExceedEvent(userId, docId, count);
        }

        return count;
    }

    /**
     * 获取用户当日对指定文档的下载次数
     */
    @Override
    public long getDownloadCount(Long userId, Long docId) {
        String key = buildGlobalKey(userId);
        Long count = RedisUtils.getCacheObject(key);
        return ObjectUtil.isNull(count) ? 0 : count;
    }

    /**
     * 获取用户当日全局下载次数
     */
    @Override
    public long getGlobalDownloadCount(Long userId) {
        return getDownloadCount(userId, null);
    }

    /**
     * 发布下载超限事件
     */
    private void publishLimitExceedEvent(Long userId, Long docId, long currentCount) {
        try {
            KbDownloadLimitExceedEvent event = new KbDownloadLimitExceedEvent();
            event.setDocId(docId);
            event.setUserId(userId);
            event.setUserName(LoginHelper.getUsername());
            event.setCurrentCount(currentCount);
            event.setLimitCount(DEFAULT_GLOBAL_LIMIT);
            event.setTenantId(TenantHelper.getTenantId());

            // 查询文档名称
            KbDoc doc = docMapper.selectById(docId);
            if (doc != null) {
                event.setDocName(doc.getDocName());
            }

            SpringUtils.context().publishEvent(event);
            log.info("用户下载次数超限: userId={}, docId={}, count={}, limit={}",
                userId, docId, currentCount, DEFAULT_GLOBAL_LIMIT);
        } catch (Exception e) {
            log.error("发布下载超限事件失败: userId={}, docId={}", userId, docId, e);
        }
    }

}
