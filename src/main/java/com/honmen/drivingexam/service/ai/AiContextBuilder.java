package com.honmen.drivingexam.service.ai;

import com.honmen.drivingexam.dto.AiDtos.AiChatRequest;
import org.springframework.stereotype.Component;

@Component
public class AiContextBuilder {
    public String build(AiChatRequest request) {
        StringBuilder context = new StringBuilder();
        context.append("用户ID：").append(request.userId() == null ? "未登录" : request.userId()).append("\n");
        context.append("当前科目：").append(subjectText(request.subject())).append("\n");
        context.append("当前场景：").append(sceneText(request.scene())).append("\n");
        context.append("场景要求：").append(sceneInstruction(request.scene())).append("\n");
        String extraContext = normalize(request.context(), 1800);
        if (!extraContext.isBlank()) {
            context.append("前端传入的业务上下文：\n").append(extraContext).append("\n");
        }
        context.append("可用工具：learningProgress、recentWrongQuestions、favoriteQuestions、latestMockExamReport、subjectStrategy、studyPlan。");
        return context.toString();
    }

    public String history(AiChatRequest request) {
        if (request.history() == null || request.history().isEmpty()) {
            return "暂无历史对话。";
        }

        StringBuilder history = new StringBuilder();
        int start = Math.max(0, request.history().size() - 8);
        for (int index = start; index < request.history().size(); index++) {
            var item = request.history().get(index);
            if (!"user".equals(item.role()) && !"assistant".equals(item.role())) {
                continue;
            }
            history
                .append("user".equals(item.role()) ? "学员：" : "助手：")
                .append(normalize(item.content(), 1200))
                .append("\n");
        }
        return history.isEmpty() ? "暂无历史对话。" : history.toString();
    }

    private String subjectText(Integer subject) {
        if (subject != null && subject == 4) {
            return "科目四";
        }
        return "科目一";
    }

    private String sceneText(String scene) {
        if (scene == null || scene.isBlank()) {
            return "普通问答";
        }
        return normalize(scene, 1200);
    }

    private String sceneInstruction(String scene) {
        if ("wrong_question_analysis".equals(scene)) {
            return "这是错题单题分析。必须优先围绕题干、正确答案、用户错选和官方解析回答，输出结论、错因分析、记忆方法、下次判断步骤。";
        }
        if ("mock_exam_report".equals(scene)) {
            return "这是模拟考试报告分析。必须先调用 latestMockExamReport(userId, subject) 查询最近一次模考真实数据，再基于工具结果输出本次结果、具体错因、复盘重点、下一步计划。不要直接输出通用复习计划。";
        }
        return "普通驾考问答。根据用户问题解释题目、复盘错题或制定学习计划。";
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        return clean.length() > maxLength ? clean.substring(0, maxLength) : clean;
    }
}
