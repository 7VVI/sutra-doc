package com.hmoob.doc.listener;

import com.hmoob.doc.event.KbDownloadLimitExceedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * KB文档下载超限事件监听器
 * 订阅 KbDownloadLimitExceedEvent，可在此处实现消息通知逻辑
 *
 * @author hmoob
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class KbDownloadLimitListener {

    /**
     * 处理下载超限事件
     * 可扩展：发送邮件、站内信、WebSocket推送等通知
     *
     * @param event 下载超限事件
     */
    @Async
    @EventListener
    public void onDownloadLimitExceeded(KbDownloadLimitExceedEvent event) {
        log.warn("收到下载超限通知: tenantId={}, userId={}, userName={}, docId={}, docName={}, count={}/{}",
            event.getTenantId(),
            event.getUserId(),
            event.getUserName(),
            event.getDocId(),
            event.getDocName(),
            event.getCurrentCount(),
            event.getLimitCount());

        // TODO: 在此扩展通知逻辑，例如：
        // 1. 发送站内消息
        // 2. 发送邮件通知管理员
        // 3. 通过WebSocket推送告警
        // 4. 记录到操作日志
    }

}
