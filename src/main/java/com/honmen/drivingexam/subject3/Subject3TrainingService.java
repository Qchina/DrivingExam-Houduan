package com.honmen.drivingexam.subject3;

import com.honmen.drivingexam.exception.ApiException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class Subject3TrainingService {
    private static final Pattern TEXT_SEPARATOR = Pattern.compile("[\\s,，。；;、：:（）()【】\\[\\]{}]+");
    private static final Pattern ANSWER_STEP_SEPARATOR = Pattern.compile("[\\n\\r,，。；;、]+");

    private final Subject3RuleEngine ruleEngine;
    private final List<ExamProject> projects = Subject3ProjectConfig.defaultProjects();

    public Subject3TrainingService(Subject3RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public List<ProjectConfig> listProjects(VehicleType vehicleType, String region) {
        String requestedRegion = normalizeRegion(region);
        return projects.stream()
            .filter(project -> vehicleType == null || project.appliesTo(vehicleType, requestedRegion))
            .sorted(Comparator.comparing(ExamProject::id))
            .map(project -> toProjectConfig(project, vehicleType))
            .toList();
    }

    public ProjectConfig getProject(String projectId, VehicleType vehicleType, String region) {
        return toProjectConfig(getExamProject(projectId, vehicleType, region), vehicleType);
    }

    public ExamProject getExamProject(String projectId, VehicleType vehicleType, String region) {
        String requestedRegion = normalizeRegion(region);
        return projects.stream()
            .filter(project -> project.id().equals(projectId))
            .filter(project -> vehicleType == null || project.appliesTo(vehicleType, requestedRegion))
            .findFirst()
            .orElseThrow(() -> new ApiException(400, "Subject 3 project not found"));
    }

    public EvaluationResult evaluate(EvaluateActionRequest request) {
        ExamProject project = getExamProject(request.projectId(), request.vehicleType(), request.normalizedRegion());
        ExamStep step = project.steps().stream()
            .filter(value -> value.id().equals(request.currentStepId()))
            .findFirst()
            .orElseThrow(() -> new ApiException(400, "Subject 3 step not found"));
        return ruleEngine.evaluateAction(request, project, step);
    }

    public TextEvaluationResult evaluateText(EvaluateTextRequest request) {
        ProjectConfig project = getProject(request.projectId(), request.vehicleType(), request.normalizedRegion());
        String normalizedAnswer = normalizeText(request.userAnswerText());
        List<StandardStep> applicableSteps = project.standardSteps().stream()
            .filter(step -> step.vehicleTypes() == null || step.vehicleTypes().isEmpty() || step.vehicleTypes().contains(request.vehicleType()))
            .sorted(Comparator.comparingInt(StandardStep::order))
            .toList();

        List<TextEvaluationResult.MatchedStep> matchedSteps = new ArrayList<>();
        Map<String, Integer> matchedPositionByStepId = new LinkedHashMap<>();
        for (StandardStep step : applicableSteps) {
            Optional<String> matchedAlias = step.aliases().stream()
                .filter(alias -> normalizedAnswer.contains(normalizeText(alias)))
                .findFirst();
            if (matchedAlias.isPresent()) {
                int position = normalizedAnswer.indexOf(normalizeText(matchedAlias.get()));
                matchedSteps.add(new TextEvaluationResult.MatchedStep(step.id(), step.title(), matchedAlias.get(), step.order()));
                matchedPositionByStepId.put(step.id(), position);
            }
        }

        List<StandardStep> missingSteps = applicableSteps.stream()
            .filter(StandardStep::required)
            .filter(step -> !matchedPositionByStepId.containsKey(step.id()))
            .toList();

        List<String> orderErrors = buildOrderErrors(matchedSteps, matchedPositionByStepId);
        List<String> extraSteps = splitAnswerSteps(request.userAnswerText()).stream()
            .filter(stepText -> applicableSteps.stream().noneMatch(step -> stepMatchesText(step, stepText)))
            .distinct()
            .toList();

        int missingDeduction = missingSteps.stream().mapToInt(StandardStep::score).sum();
        int orderDeduction = orderErrors.size() * 5;
        int extraDeduction = extraSteps.size() * 2;
        int score = Math.max(100 - missingDeduction - orderDeduction - extraDeduction, 0);
        boolean failMissing = missingSteps.stream().anyMatch(StandardStep::failIfMissing);
        boolean passed = score >= 90 && !failMissing && orderErrors.isEmpty();
        List<String> feedback = buildTextFeedback(passed, applicableSteps.size(), matchedSteps, missingSteps, orderErrors, extraSteps);

        return new TextEvaluationResult(score, passed, missingSteps, orderErrors, extraSteps, matchedSteps, feedback);
    }

    private ProjectConfig toProjectConfig(ExamProject project, VehicleType requestedVehicleType) {
        List<StandardStep> standardSteps = project.steps().stream()
            .filter(step -> requestedVehicleType == null || step.vehicleTypes() == null || step.vehicleTypes().isEmpty()
                || step.vehicleTypes().contains(requestedVehicleType))
            .sorted(Comparator.comparingInt(ExamStep::orderIndex))
            .map(step -> toStandardStep(project, step))
            .toList();
        List<String> keywords = buildKeywords(project, standardSteps);
        List<String> commonMistakes = project.failRules().stream().map(ScoreRule::message).distinct().toList();
        return new ProjectConfig(
            project.id(),
            project.name(),
            project.description(),
            "请完成：" + project.name(),
            project.vehicleTypes(),
            keywords,
            standardSteps,
            commonMistakes,
            project.region(),
            project.tips(),
            project.roadScenes(),
            project.steps(),
            project.scoreRules(),
            project.failRules()
        );
    }

    private StandardStep toStandardStep(ExamProject project, ExamStep step) {
        return new StandardStep(
            step.id(),
            step.title(),
            aliasesFor(project.id(), step),
            step.required(),
            step.orderIndex(),
            scoreFor(project, step),
            failIfMissing(project, step),
            step.vehicleTypes()
        );
    }

    private List<String> buildKeywords(ExamProject project, List<StandardStep> standardSteps) {
        List<String> keywords = new ArrayList<>();
        keywords.add(project.name());
        project.roadScenes().forEach(scene -> keywords.add(scene.name()));
        standardSteps.forEach(step -> {
            keywords.add(step.title());
            keywords.addAll(step.aliases());
        });
        return keywords.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
    }

    private List<String> aliasesFor(String projectId, ExamStep step) {
        Map<String, List<String>> configuredAliases = Map.ofEntries(
            Map.entry("boarding_preparation:walk_around", List.of("绕车检查", "环车检查", "检查车辆周围", "检查车身周围")),
            Map.entry("boarding_preparation:door_observe", List.of("打开车门前观察后方", "开门前观察后方", "开门前看后方", "观察后方交通")),
            Map.entry("boarding_preparation:seat_belt", List.of("系安全带", "系好安全带", "佩戴安全带", "使用安全带")),
            Map.entry("boarding_preparation:adjust_seat", List.of("调整座椅", "调座椅", "调整坐姿")),
            Map.entry("boarding_preparation:adjust_mirror", List.of("调整后视镜", "调后视镜", "调整内外后视镜")),
            Map.entry("bus_station:slow_down", List.of("提前减速", "减速慢行", "降低车速", "通过公交站减速")),
            Map.entry("bus_station:observe", List.of("观察左右交通情况", "左右观察", "观察行人", "观察非机动车")),
            Map.entry("bus_station:yield", List.of("停车礼让行人", "礼让行人", "有行人停车", "停车让行")),
            Map.entry("crosswalk:slow_down", List.of("提前减速", "减速慢行", "降低车速", "通过人行横道减速")),
            Map.entry("crosswalk:observe", List.of("观察左右交通情况", "左右观察", "观察行人", "观察非机动车")),
            Map.entry("crosswalk:yield", List.of("停车礼让行人", "礼让行人", "有行人停车", "停车让行"))
        );
        return configuredAliases.getOrDefault(projectId + ":" + step.id(), List.of(step.title()));
    }

    private List<String> buildOrderErrors(
        List<TextEvaluationResult.MatchedStep> matchedSteps,
        Map<String, Integer> matchedPositionByStepId
    ) {
        List<String> orderErrors = new ArrayList<>();
        int previousPosition = -1;
        String previousTitle = null;
        for (TextEvaluationResult.MatchedStep matchedStep : matchedSteps.stream().sorted(Comparator.comparingInt(TextEvaluationResult.MatchedStep::order)).toList()) {
            int position = matchedPositionByStepId.getOrDefault(matchedStep.stepId(), -1);
            if (position >= 0 && previousPosition > position) {
                orderErrors.add("“" + matchedStep.title() + "”应在“" + previousTitle + "”之后作答");
            }
            previousPosition = Math.max(previousPosition, position);
            previousTitle = matchedStep.title();
        }
        return orderErrors;
    }

    private List<String> buildTextFeedback(
        boolean passed,
        int totalSteps,
        List<TextEvaluationResult.MatchedStep> matchedSteps,
        List<StandardStep> missingSteps,
        List<String> orderErrors,
        List<String> extraSteps
    ) {
        List<String> feedback = new ArrayList<>();
        feedback.add("已匹配 " + matchedSteps.size() + "/" + totalSteps + " 个标准步骤。");
        feedback.add(passed ? "文本答题通过，关键步骤完整且顺序正确。" : "请补齐缺失步骤，并按训练流程顺序作答。");
        missingSteps.forEach(step -> feedback.add("缺少：" + step.title()));
        orderErrors.forEach(error -> feedback.add("顺序问题：" + error));
        extraSteps.forEach(step -> feedback.add("未识别：" + step));
        return feedback;
    }

    private List<String> splitAnswerSteps(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            return List.of();
        }
        return ANSWER_STEP_SEPARATOR.splitAsStream(answerText)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private boolean stepMatchesText(StandardStep step, String answerStepText) {
        String normalizedStepText = normalizeText(answerStepText);
        return step.aliases().stream()
            .map(this::normalizeText)
            .anyMatch(alias -> normalizedStepText.contains(alias) || alias.contains(normalizedStepText));
    }

    private int scoreFor(ExamProject project, ExamStep step) {
        return project.failRules().stream().anyMatch(rule -> rule.condition().equals("MISSING_STEP:" + step.id()))
            ? 100
            : 10;
    }

    private boolean failIfMissing(ExamProject project, ExamStep step) {
        return project.failRules().stream()
            .anyMatch(rule -> rule.condition().equals("MISSING_STEP:" + step.id()) && rule.isFail());
    }

    private String normalizeText(String text) {
        return TEXT_SEPARATOR.matcher(text == null ? "" : text)
            .replaceAll("")
            .toLowerCase(Locale.ROOT);
    }

    private String normalizeRegion(String region) {
        return region == null || region.isBlank() ? "default" : region;
    }
}
