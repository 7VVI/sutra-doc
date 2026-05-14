package com.hmoob.doc.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.hmoob.common.core.domain.R;
import com.hmoob.doc.service.ChatAssistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI聊天验证Controller
 * 用于验证LangChain4j与智谱AI是否正常工作
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/kb/ai")
public class KbAiChatController {

    @Autowired
    private ChatAssistant chatAssistant;

    @GetMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> model(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return chatAssistant.chat(message);
    }
}
