package com.honmen.drivingexam.service;

import com.honmen.drivingexam.dto.AiDtos.AiChatRequest;
import com.honmen.drivingexam.dto.AiDtos.AiChatResponse;
import com.honmen.drivingexam.service.ai.AiContextBuilder;
import com.honmen.drivingexam.service.ai.DrivingExamAiAssistant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiChatService {
    private final String apiKey;
    private final String model;
    private final DrivingExamAiAssistant assistant;
    private final AiContextBuilder contextBuilder;

    public AiChatService(
        @Value("${ai.api-key:}") String apiKey,
        @Value("${ai.model:deepseek-v4-pro}") String model,
        DrivingExamAiAssistant assistant,
        AiContextBuilder contextBuilder
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.assistant = assistant;
        this.contextBuilder = contextBuilder;
    }

    public AiChatResponse chat(AiChatRequest request) {
        String message = normalize(request.message());
        if (message.isBlank()) {
            return new AiChatResponse("你可以问我题目解析、错题复盘或复习计划。", model, "fallback", LocalDateTime.now());
        }
        if (apiKey == null || apiKey.isBlank()) {
            return new AiChatResponse(fallbackReply(message, request.subject(), request.scene()), model, "fallback", LocalDateTime.now());
        }

        String reply;
        try {
            reply = assistant.chat(
                message,
                contextBuilder.build(request),
                contextBuilder.history(request)
            ).trim();
        } catch (Exception ex) {
            reply = fallbackReply(message, request.subject(), request.scene());
        }
        if (reply.isBlank()) {
            reply = fallbackReply(message, request.subject(), request.scene());
        }

        return new AiChatResponse(reply, model, "ai", LocalDateTime.now());
    }

    private String fallbackReply(String message, Integer subject, String scene) {
        if ("wrong_question_analysis".equals(scene)) {
            return "结论：这道题要先看题干关键词，再对照正确答案判断规则。\n\n**错因分析**：通常是把相近概念混在一起，或只凭生活习惯选答案。\n\n**记忆方法**：把正确答案和题干关键词绑定记忆，遇到类似题先找“让行、减速、禁止、确认安全”等核心词。\n\n**下次判断步骤**：先读问法，再排除危险做法，最后选择最安全、最符合法规的一项。";
        }
        if ("mock_exam_report".equals(scene)) {
            return "本次报告建议先看三点：分数是否达到 90 分、错题是否集中在同类知识点、是否有未答题。\n\n**复盘重点**：先清空本次错题，再把高频错因整理成规则。\n\n**下一步计划**：每天顺序练习一组题，错题本复盘 15 分钟，再做 1 套模拟卷，正确率稳定到 90% 以上再约考。";
        }
        String subjectText = subject == null ? "当前科目" : "科目" + subject;
        if (message.contains("错题") || message.contains("老错") || message.contains("总错")) {
            return "可以按“原因分类”复盘错题：先看题目问的是让行、速度、灯光还是标志，再对照正确答案记规则。建议把同类错题连续重做 3 遍，直到能说出为什么错。";
        }
        if (message.contains("计划") || message.contains("怎么学") || message.contains("复习")) {
            return "建议这样安排：" + subjectText + "每天先顺序练习 80-120 题，再复盘错题本，最后做一套模拟考试。正确率稳定到 90% 以上，再重点刷易错题和收藏题。";
        }
        if (message.contains("科目一") || message.contains("科一")) {
            return "科目一重点是交通法规、标志标线、信号灯、让行规则和扣分罚款。刷题时不要只背答案，要抓关键词，比如“依次交替通行”“减速让行”“确认安全后”。";
        }
        if (message.contains("科目四") || message.contains("科四")) {
            return "科目四更偏安全文明驾驶，遇到题目优先选安全、减速、让行、观察、保护行人和非机动车，避免选择急打方向、加速、抢行。";
        }
        return "我可以帮你解释驾考题、整理错题原因、制定复习计划。你可以这样问：“这道题为什么选 B？”、“科一扣分题怎么记？”、“我错题多该怎么复习？”";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        return clean.length() > 1200 ? clean.substring(0, 1200) : clean;
    }

}
