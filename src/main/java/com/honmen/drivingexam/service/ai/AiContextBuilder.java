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
        context.append("可用工具：learningProgress、recentWrongQuestions、favoriteQuestions、subjectStrategy、studyPlan。");
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
                .append(normalize(item.content()))
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
        return normalize(scene);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        return clean.length() > 1200 ? clean.substring(0, 1200) : clean;
    }
}
