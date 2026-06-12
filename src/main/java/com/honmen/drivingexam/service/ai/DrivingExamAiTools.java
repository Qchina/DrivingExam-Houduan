package com.honmen.drivingexam.service.ai;

import com.honmen.drivingexam.dto.BusinessDtos.StatsOverview;
import com.honmen.drivingexam.dto.BusinessDtos.SubjectOverview;
import com.honmen.drivingexam.dto.BusinessDtos.ErrorQuestion;
import com.honmen.drivingexam.dto.BusinessDtos.FavoriteQuestion;
import com.honmen.drivingexam.dto.PageResult;
import com.honmen.drivingexam.exception.ApiException;
import com.honmen.drivingexam.model.ExamHistory;
import com.honmen.drivingexam.model.Question;
import com.honmen.drivingexam.service.DrivingExamService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Component
public class DrivingExamAiTools {
    private final DrivingExamService drivingExamService;

    public DrivingExamAiTools(DrivingExamService drivingExamService) {
        this.drivingExamService = drivingExamService;
    }

    @Tool("查询当前学员科目一和科目四练习进度，用于制定复习计划和错题复盘建议。")
    public String learningProgress(@P("当前登录用户ID") Long userId) {
        if (userId == null || userId <= 0) {
            return "用户未登录，无法查询个人学习进度。";
        }
        try {
            StatsOverview overview = drivingExamService.statsOverview(userId);
            return "科目一：" + formatSubjectOverview(overview.subject1())
                + "；科目四：" + formatSubjectOverview(overview.subject4());
        } catch (ApiException ex) {
            return "学习进度查询失败：" + ex.getMessage();
        }
    }

    @Tool("获取指定科目的考试重点和答题策略。")
    public String subjectStrategy(@P("科目编号，只支持1或4") Integer subject) {
        if (subject != null && subject == 4) {
            return "科目四重点：安全文明驾驶、恶劣天气、事故处理、紧急避险。答题倾向是减速、让行、观察、保护行人和非机动车。";
        }
        return "科目一重点：交通信号、标志标线、让行规则、速度限制、扣分罚款。答题时先看关键词，再判断是禁止、警告、指示还是让行。";
    }

    @Tool("生成驾考理论学习计划。")
    public String studyPlan(@P("科目编号，只支持1或4") Integer subject, @P("每天可学习分钟数") Integer dailyMinutes) {
        int minutes = dailyMinutes == null || dailyMinutes <= 0 ? 45 : Math.min(dailyMinutes, 180);
        String subjectText = subject != null && subject == 4 ? "科目四" : "科目一";
        return subjectText + "建议每天学习 " + minutes + " 分钟：前 60% 时间顺序练习，接着复盘错题本，最后做 10-15 分钟模拟考试或收藏题回顾。正确率稳定到 90% 后再考前冲刺。";
    }

    @Tool("查询用户最近错题样本，用于总体分析易错知识点、错因和复习建议。")
    public String recentWrongQuestions(
        @P("当前登录用户ID") Long userId,
        @P("科目编号，只支持1或4") Integer subject
    ) {
        if (userId == null || userId <= 0) {
            return "用户未登录，无法查询错题。";
        }
        try {
            int safeSubject = normalizeSubject(subject);
            PageResult<ErrorQuestion> result = drivingExamService.listErrors(userId, safeSubject, 0, "all", 1, 6);
            if (result.total() == 0 || result.list().isEmpty()) {
                return subjectText(safeSubject) + "暂无未掌握错题。";
            }
            StringBuilder builder = new StringBuilder();
            builder.append(subjectText(safeSubject)).append("未掌握错题共 ").append(result.total()).append(" 道，最近样本：");
            Map<String, Integer> categoryCount = new LinkedHashMap<>();
            for (ErrorQuestion item : result.list()) {
                Question question = item.question();
                String category = categoryOf(question);
                categoryCount.merge(category, 1, Integer::sum);
                builder.append("\n")
                    .append("- 标签：").append(category)
                    .append("；题目：").append(limit(question.title(), 90))
                    .append("；正确答案：").append(question.answer())
                    .append("；最近错选：").append(blankToDefault(item.latestWrongAnswer(), "未记录"))
                    .append("；错误次数：").append(item.errorCount())
                    .append("；解析：").append(limit(question.description(), 90));
            }
            builder.append("\n错题标签分布：").append(formatCategoryCount(categoryCount));
            return builder.toString();
        } catch (ApiException ex) {
            return "错题查询失败：" + ex.getMessage();
        }
    }

    @Tool("按知识点标签统计用户错题，用于判断主要薄弱点。")
    public String wrongQuestionTags(
        @P("当前登录用户ID") Long userId,
        @P("科目编号，只支持1或4") Integer subject
    ) {
        if (userId == null || userId <= 0) {
            return "用户未登录，无法统计错题标签。";
        }
        try {
            int safeSubject = normalizeSubject(subject);
            PageResult<ErrorQuestion> result = drivingExamService.listErrors(userId, safeSubject, 0, "all", 1, 50);
            if (result.total() == 0 || result.list().isEmpty()) {
                return subjectText(safeSubject) + "暂无错题标签数据。";
            }
            Map<String, Integer> categoryCount = new LinkedHashMap<>();
            for (ErrorQuestion item : result.list()) {
                categoryCount.merge(categoryOf(item.question()), 1, Integer::sum);
            }
            return subjectText(safeSubject) + "错题标签统计：" + formatCategoryCount(categoryCount);
        } catch (ApiException ex) {
            return "错题标签统计失败：" + ex.getMessage();
        }
    }

    @Tool("查询用户收藏题样本，用于分析重点关注内容和考前回顾建议。")
    public String favoriteQuestions(
        @P("当前登录用户ID") Long userId,
        @P("科目编号，只支持1或4") Integer subject
    ) {
        if (userId == null || userId <= 0) {
            return "用户未登录，无法查询收藏题。";
        }
        try {
            int safeSubject = normalizeSubject(subject);
            PageResult<FavoriteQuestion> result = drivingExamService.listFavorites(userId, safeSubject, 1, 5);
            if (result.total() == 0 || result.list().isEmpty()) {
                return subjectText(safeSubject) + "暂无收藏题。";
            }
            StringBuilder builder = new StringBuilder();
            builder.append(subjectText(safeSubject)).append("收藏题共 ").append(result.total()).append(" 道，最近样本：");
            for (FavoriteQuestion item : result.list()) {
                Question question = item.question();
                builder.append("\n")
                    .append("- 题目：").append(limit(question.title(), 90))
                    .append("；答案：").append(question.answer())
                    .append("；解析：").append(limit(question.description(), 90));
            }
            return builder.toString();
        } catch (ApiException ex) {
            return "收藏题查询失败：" + ex.getMessage();
        }
    }

    @Tool("查询用户最近一次模拟考试报告，用于分析本次模考表现、错题原因和下一步复习计划。")
    public String latestMockExamReport(
        @P("当前登录用户ID") Long userId,
        @P("科目编号，只支持1或4") Integer subject
    ) {
        if (userId == null || userId <= 0) {
            return "用户未登录，无法查询最近模拟考试报告。";
        }
        try {
            int safeSubject = normalizeSubject(subject);
            List<ExamHistory> histories = drivingExamService.examHistory(userId, safeSubject);
            if (histories.isEmpty()) {
                return subjectText(safeSubject) + "暂无模拟考试记录。";
            }

            ExamHistory latest = histories.get(0);
            int totalCount = latest.questionIds() == null || latest.questionIds().isEmpty()
                ? (safeSubject == 1 ? 100 : 50)
                : latest.questionIds().size();
            int wrongCount = latest.wrongQuestionIds() == null ? 0 : latest.wrongQuestionIds().size();
            int correctCount = Math.max(0, Math.round(latest.score() * totalCount / 100.0f));
            String accuracy = String.format("%.1f%%", correctCount * 100.0 / totalCount);
            Map<Long, String> selectedAnswerMap = selectedAnswerMap(latest);

            StringBuilder builder = new StringBuilder();
            builder.append(subjectText(safeSubject)).append("最近一次模拟考试：")
                .append("分数 ").append(latest.score()).append(" 分")
                .append("，正确率 ").append(accuracy)
                .append("，答对 ").append(correctCount).append(" 题")
                .append("，答错 ").append(wrongCount).append(" 题")
                .append("，结果 ").append(latest.isPassed() == 1 ? "合格" : "未合格")
                .append("，用时 ").append(formatDuration(latest.timeUsed())).append("。");

            builder.append("\n最近模考趋势：").append(mockTrend(histories));

            if (wrongCount == 0) {
                builder.append("\n本次没有记录错题。");
                return builder.toString();
            }

            List<Long> wrongIds = latest.wrongQuestionIds().stream().limit(6).toList();
            String ids = wrongIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            Map<Long, Question> questions = drivingExamService.batchQuestions(ids)
                .stream()
                .collect(Collectors.toMap(Question::id, question -> question));

            builder.append("\n本次错题样本：");
            Map<String, Integer> categoryCount = new LinkedHashMap<>();
            for (Long questionId : wrongIds) {
                Question question = questions.get(questionId);
                if (question == null) {
                    continue;
                }
                String category = categoryOf(question);
                categoryCount.merge(category, 1, Integer::sum);
                builder.append("\n- 题目：").append(limit(question.title(), 90))
                    .append("；标签：").append(category)
                    .append("；题型：").append(questionTypeText(question.type()))
                    .append("；你的选择：").append(answerWithContent(question, selectedAnswerMap.get(questionId)))
                    .append("；正确答案：").append(question.answer())
                    .append("；选项：").append(optionSummary(question))
                    .append("；解析：").append(limit(question.description(), 100));
            }
            builder.append("\n本次错题标签分布：").append(formatCategoryCount(categoryCount));
            return builder.toString();
        } catch (ApiException ex) {
            return "模拟考试报告查询失败：" + ex.getMessage();
        }
    }

    private String formatSubjectOverview(SubjectOverview overview) {
        return "已答 " + overview.totalAnswered()
            + " 题，正确 " + overview.totalCorrect()
            + " 题，错题 " + overview.totalWrong()
            + " 题，正确率 " + overview.accuracy();
    }

    private int normalizeSubject(Integer subject) {
        return subject != null && subject == 4 ? 4 : 1;
    }

    private String subjectText(int subject) {
        return subject == 4 ? "科目四" : "科目一";
    }

    private String mockTrend(List<ExamHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return "暂无历史模考数据。";
        }
        List<ExamHistory> latest = histories.stream().limit(3).toList();
        String scores = latest.stream()
            .map(item -> item.score() + "分")
            .collect(Collectors.joining("、"));
        if (latest.size() < 2) {
            return "仅有 1 次记录：" + scores + "，建议再做 1-2 套确认稳定性。";
        }
        int newest = latest.get(0).score();
        int previous = latest.get(1).score();
        String direction = newest > previous ? "上升" : newest < previous ? "下降" : "持平";
        long passedCount = latest.stream().filter(item -> item.isPassed() == 1).count();
        return "近" + latest.size() + "次分数：" + scores + "，最近一次较上次" + direction + "，合格 " + passedCount + "/" + latest.size() + " 次。";
    }

    private String categoryOf(Question question) {
        String text = ((question.title() == null ? "" : question.title()) + " " + (question.description() == null ? "" : question.description())).toLowerCase();
        if (containsAny(text, "标志", "标线", "指示", "警告", "禁令", "路面", "车道")) {
            return "标志标线";
        }
        if (containsAny(text, "让行", "先行", "行人", "非机动车", "交叉路口", "会车")) {
            return "让行规则";
        }
        if (containsAny(text, "速度", "限速", "公里", "减速", "加速", "车速")) {
            return "速度控制";
        }
        if (containsAny(text, "扣", "罚款", "违法", "处罚", "记分", "驾驶证")) {
            return "违法处罚";
        }
        if (containsAny(text, "灯", "信号", "照明", "远光", "近光", "雾灯")) {
            return "灯光信号";
        }
        if (containsAny(text, "事故", "伤员", "急救", "爆胎", "侧滑", "雨", "雪", "雾", "安全")) {
            return "安全文明";
        }
        return "综合判断";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String formatCategoryCount(Map<String, Integer> categoryCount) {
        if (categoryCount == null || categoryCount.isEmpty()) {
            return "暂无明显集中标签";
        }
        return categoryCount.entrySet().stream()
            .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
            .map(entry -> entry.getKey() + entry.getValue() + "题")
            .collect(Collectors.joining("、"));
    }

    private String questionTypeText(String type) {
        String normalized = type == null ? "" : type.toLowerCase();
        if ("2".equals(normalized) || "judge".equals(normalized)) {
            return "判断题";
        }
        if ("3".equals(normalized) || "multiple".equals(normalized)) {
            return "多选题";
        }
        return "单选题";
    }

    private String optionSummary(Question question) {
        if ("2".equals(question.type()) || "judge".equalsIgnoreCase(question.type())) {
            return "A.正确；B.错误";
        }
        StringBuilder builder = new StringBuilder();
        appendOption(builder, "A", question.optionA());
        appendOption(builder, "B", question.optionB());
        appendOption(builder, "C", question.optionC());
        appendOption(builder, "D", question.optionD());
        return builder.isEmpty() ? "无选项信息" : builder.toString();
    }

    private Map<Long, String> selectedAnswerMap(ExamHistory history) {
        if (history.questionIds() == null || history.selectedAnswers() == null) {
            return Map.of();
        }
        java.util.HashMap<Long, String> result = new java.util.HashMap<>();
        int size = Math.min(history.questionIds().size(), history.selectedAnswers().size());
        for (int index = 0; index < size; index++) {
            result.put(history.questionIds().get(index), history.selectedAnswers().get(index));
        }
        return result;
    }

    private String answerWithContent(Question question, String answer) {
        if (answer == null || answer.isBlank()) {
            return "未作答";
        }
        String normalized = normalizeAnswer(answer);
        if (normalized.isBlank()) {
            return "未作答";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < normalized.length(); index++) {
            String key = String.valueOf(normalized.charAt(index));
            String content = optionContent(question, key);
            if (!builder.isEmpty()) {
                builder.append("；");
            }
            builder.append(key);
            if (content != null && !content.isBlank()) {
                builder.append(".").append(limit(content, 45));
            }
        }
        return builder.toString();
    }

    private String optionContent(Question question, String key) {
        if (("2".equals(question.type()) || "judge".equalsIgnoreCase(question.type()))) {
            if ("A".equals(key)) {
                return "正确";
            }
            if ("B".equals(key)) {
                return "错误";
            }
            return "";
        }
        return switch (key) {
            case "A" -> question.optionA();
            case "B" -> question.optionB();
            case "C" -> question.optionC();
            case "D" -> question.optionD();
            default -> "";
        };
    }

    private String normalizeAnswer(String answer) {
        java.util.TreeSet<Character> answerSet = new java.util.TreeSet<>();
        String upperAnswer = answer == null ? "" : answer.toUpperCase();
        for (int index = 0; index < upperAnswer.length(); index++) {
            char current = upperAnswer.charAt(index);
            if (current >= 'A' && current <= 'D') {
                answerSet.add(current);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (Character item : answerSet) {
            builder.append(item);
        }
        return builder.toString();
    }

    private void appendOption(StringBuilder builder, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("；");
        }
        builder.append(key).append(".").append(limit(value, 45));
    }

    private String formatDuration(int seconds) {
        int minutes = Math.max(0, seconds) / 60;
        int remainSeconds = Math.max(0, seconds) % 60;
        if (minutes == 0) {
            return remainSeconds + " 秒";
        }
        return minutes + " 分 " + remainSeconds + " 秒";
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "无";
        }
        String clean = value.replace("\n", " ").trim();
        return clean.length() > maxLength ? clean.substring(0, maxLength) + "..." : clean;
    }
}
