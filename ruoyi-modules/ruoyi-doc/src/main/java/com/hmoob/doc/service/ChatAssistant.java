package com.hmoob.doc.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

/**
 * @author zhangyh
 * @Date 2026/5/14 17:21
 * @desc
 */
@AiService
public interface ChatAssistant {

    Flux<String> chat(@UserMessage String userMessage);
}
