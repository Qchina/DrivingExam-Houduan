package com.honmen.drivingexam.service;

import com.honmen.drivingexam.dto.AiDtos.AiChatRequest;
import com.honmen.drivingexam.dto.AiDtos.AiChatResponse;
import com.honmen.drivingexam.dto.AiDtos.AiDailyTask;
import com.honmen.drivingexam.dto.AiDtos.AiDailyTaskRequest;
import com.honmen.drivingexam.dto.AiDtos.AiDailyTaskResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honmen.drivingexam.service.ai.AiContextBuilder;
import com.honmen.drivingexam.service.ai.DrivingExamAiAssistant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiChatService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final DrivingExamAiAssistant assistant;
    private final AiContextBuilder contextBuilder;
    private final JdbcTemplate jdbc;

    public AiChatService(
        @Value("${ai.api-key:}") String apiKey,
        @Value("${ai.model:deepseek-v4-pro}") String model,
        DrivingExamAiAssistant assistant,
        AiContextBuilder contextBuilder,
        JdbcTemplate jdbc
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.assistant = assistant;
        this.contextBuilder = contextBuilder;
        this.jdbc = jdbc;
        ensureDailyTaskTable();
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

    public AiDailyTaskResponse dailyTasks(AiDailyTaskRequest request) {
        if (request == null) {
            return fallbackDailyTasks(1, "fallback");
        }
        Integer subject = normalizeSubject(request.subject());
        Long userId = request.userId();
        boolean forceRefresh = Boolean.TRUE.equals(request.forceRefresh());
        boolean cacheOnly = Boolean.TRUE.equals(request.cacheOnly());

        if (!forceRefresh && userId != null && userId > 0) {
            AiDailyTaskResponse cached = findTodayDailyTasks(userId, subject);
            if (cached != null) {
                return cached;
            }
        }
        if (cacheOnly) {
            return fallbackDailyTasks(subject, "empty");
        }

        AiDailyTaskResponse result;
        if (apiKey == null || apiKey.isBlank()) {
            result = fallbackDailyTasks(subject, "fallback");
        } else {
            try {
                AiChatRequest contextRequest = new AiChatRequest(
                    "生成首页今日任务",
                    userId,
                    subject,
                    "study_dashboard",
                    request.context(),
                    List.of()
                );
                String reply = assistant.dailyTasks(contextBuilder.build(contextRequest)).trim();
                AiDailyTaskResponse parsed = parseDailyTasks(reply);
                if (parsed != null && parsed.tasks() != null && !parsed.tasks().isEmpty()) {
                    result = new AiDailyTaskResponse(
                        normalizeText(parsed.summary(), "今日复习"),
                        normalizeTasks(parsed.tasks()),
                        model,
                        "ai",
                        false,
                        LocalDateTime.now()
                    );
                } else {
                    result = fallbackDailyTasks(subject, "fallback");
                }
            } catch (Exception ignored) {
                // Fall back to stable local tasks when the model is unavailable or returns malformed JSON.
                result = fallbackDailyTasks(subject, "fallback");
            }
        }

        if (userId != null && userId > 0) {
            saveTodayDailyTasks(userId, subject, result);
        }
        return result;
    }

    private String fallbackReply(String message, Integer subject, String scene) {
        if ("wrong_question_analysis".equals(scene)) {
            return "结论：这道题要先看题干关键词，再对照正确答案判断规则。\n\n**错因分析**：通常是把相近概念混在一起，或只凭生活习惯选答案。\n\n**记忆方法**：把正确答案和题干关键词绑定记忆，遇到类似题先找“让行、减速、禁止、确认安全”等核心词。\n\n**下次判断步骤**：先读问法，再排除危险做法，最后选择最安全、最符合法规的一项。";
        }
        if ("mock_exam_report".equals(scene)) {
            return "本次报告建议先看三点：分数是否达到 90 分、错题是否集中在同类知识点、是否有未答题。\n\n**复盘重点**：先清空本次错题，再把高频错因整理成规则。\n\n**下一步计划**：每天顺序练习一组题，错题本复盘 15 分钟，再做 1 套模拟卷，正确率稳定到 90% 以上再约考。";
        }
        if ("study_dashboard".equals(scene)) {
            String subjectText = subject != null && subject == 4 ? "科目四" : "科目一";
            return "**当前状态**：我会优先看你的" + subjectText + "顺序练习、错题本、收藏题和最近模考。\n\n**今日3个动作**：\n- 先顺序练习 30-50 题，补齐基础样本。\n- 复盘错题本，把反复错的题标记出来。\n- 做 1 次模拟考试，用成绩判断是否进入冲刺。\n\n后端 AI 暂时不可用时，这是兜底建议；AI 可用后会结合真实数据细化。";
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

    private AiDailyTaskResponse fallbackDailyTasks(Integer subject, String mode) {
        String subjectText = subject != null && subject == 4 ? "科四" : "科一";
        return new AiDailyTaskResponse(
            subjectText + "今日复习",
            List.of(
                new AiDailyTask("顺序练习", "先刷一组题", "practice"),
                new AiDailyTask("错题复盘", "清掉薄弱点", "error"),
                new AiDailyTask("模拟测验", "检验通过率", "mock")
            ),
            model,
            mode,
            false,
            LocalDateTime.now()
        );
    }

    private void ensureDailyTaskTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS ai_daily_task (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                subject INT NOT NULL,
                task_date DATE NOT NULL,
                summary VARCHAR(100) NOT NULL,
                tasks_json JSON NOT NULL,
                model VARCHAR(80) NOT NULL,
                mode VARCHAR(30) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_ai_daily_task_user_subject_date (user_id, subject, task_date)
            )
            """);
    }

    private AiDailyTaskResponse findTodayDailyTasks(long userId, int subject) {
        try {
            return jdbc.query("""
                SELECT summary, tasks_json, model, mode, updated_at
                FROM ai_daily_task
                WHERE user_id = ? AND subject = ? AND task_date = CURDATE()
                LIMIT 1
                """, rs -> {
                if (!rs.next()) {
                    return null;
                }
                List<AiDailyTask> tasks = parseStoredTasks(rs.getString("tasks_json"));
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                return new AiDailyTaskResponse(
                    rs.getString("summary"),
                    normalizeTasks(tasks),
                    rs.getString("model"),
                    rs.getString("mode"),
                    true,
                    updatedAt == null ? LocalDateTime.now() : updatedAt.toLocalDateTime()
                );
            }, userId, subject);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<AiDailyTask> parseStoredTasks(String value) {
        try {
            return OBJECT_MAPPER.readValue(
                value,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, AiDailyTask.class)
            );
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void saveTodayDailyTasks(long userId, int subject, AiDailyTaskResponse response) {
        try {
            jdbc.update("""
                INSERT INTO ai_daily_task (user_id, subject, task_date, summary, tasks_json, model, mode)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    summary = VALUES(summary),
                    tasks_json = VALUES(tasks_json),
                    model = VALUES(model),
                    mode = VALUES(mode),
                    updated_at = CURRENT_TIMESTAMP
                """,
                userId,
                subject,
                LocalDate.now(),
                normalizeText(response.summary(), "今日复习"),
                OBJECT_MAPPER.writeValueAsString(normalizeTasks(response.tasks())),
                normalizeText(response.model(), model),
                normalizeText(response.mode(), "fallback")
            );
        } catch (Exception ignored) {
            // Task persistence is an enhancement; AI generation should still return normally if storage fails.
        }
    }

    private AiDailyTaskResponse parseDailyTasks(String reply) throws Exception {
        String json = extractJson(reply);
        if (json.isBlank()) {
            return null;
        }
        return OBJECT_MAPPER.readValue(json, AiDailyTaskResponse.class);
    }

    private List<AiDailyTask> normalizeTasks(List<AiDailyTask> tasks) {
        return tasks.stream()
            .filter(task -> task != null)
            .limit(3)
            .map(task -> new AiDailyTask(
                normalizeText(task.title(), "今日任务"),
                normalizeText(task.subtitle(), "继续练习"),
                normalizeAction(task.actionType())
            ))
            .toList();
    }

    private String extractJson(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        clean = clean.replace("```json", "").replace("```", "").trim();
        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return "";
        }
        return clean.substring(start, end + 1);
    }

    private String normalizeAction(String actionType) {
        if (actionType == null) {
            return "practice";
        }
        return switch (actionType) {
            case "error", "mock", "favorite", "ai" -> actionType;
            default -> "practice";
        };
    }

    private int normalizeSubject(Integer subject) {
        return subject != null && subject == 4 ? 4 : 1;
    }

    private String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        return clean.length() > 1200 ? clean.substring(0, 1200) : clean;
    }

}
