package com.honmen.drivingexam.subject3;

import com.honmen.drivingexam.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class Subject3RuleEngine {
    public EvaluationResult evaluateAction(EvaluateActionRequest userAction, ExamProject currentProject, ExamStep currentStep) {
        List<ScoreRule> triggeredRules = new ArrayList<>();
        Set<String> completedStepIds = new HashSet<>(Optional.ofNullable(userAction.completedStepIds()).orElse(List.of()));
        Map<String, Object> context = Optional.ofNullable(userAction.context()).orElse(Map.of());
        int deduction = 0;
        boolean fail = false;

        if (!currentProject.appliesTo(userAction.vehicleType(), userAction.normalizedRegion())) {
            throw new ApiException(400, "Project does not apply to this vehicle type or region");
        }
        if (!currentStep.appliesTo(userAction.vehicleType(), userAction.roadScene())) {
            return result(false, "当前步骤不适用于所选车辆类型或道路场景", 0, false, currentStep.id(), userAction.currentScore(), List.of());
        }

        if (currentStep.timeWindowSeconds() > 0 && userAction.elapsedSeconds() > currentStep.timeWindowSeconds()) {
            ScoreRule timeoutRule = timeoutRule(currentProject);
            triggeredRules.add(timeoutRule);
            deduction += timeoutRule.deduction();
            fail = fail || timeoutRule.isFail();
        }

        List<ExamStep> applicableSteps = applicableSteps(currentProject, userAction.vehicleType(), userAction.roadScene());
        Optional<ExamStep> missingPrevious = applicableSteps.stream()
            .filter(step -> step.required() && step.orderIndex() < currentStep.orderIndex())
            .filter(step -> !completedStepIds.contains(step.id()))
            .findFirst();
        if (missingPrevious.isPresent()) {
            ScoreRule orderRule = findRule(currentProject, "MISSING_STEP:" + missingPrevious.get().id())
                .orElse(new ScoreRule("order_" + missingPrevious.get().id(), currentProject.id(), "ORDER_ERROR", 5, false,
                    "顺序错误：请先完成「" + missingPrevious.get().title() + "」"));
            triggeredRules.add(orderRule);
            deduction += orderRule.deduction();
            fail = fail || orderRule.isFail();
        }

        if (userAction.actionType() != currentStep.actionType()) {
            ScoreRule mismatchRule = findRule(currentProject, "ACTION_MISMATCH")
                .orElse(new ScoreRule("action_mismatch", currentProject.id(), "ACTION_MISMATCH", 0, false, currentStep.wrongFeedback()));
            triggeredRules.add(mismatchRule);
            return result(false, mismatchRule.message(), deduction + mismatchRule.deduction(), fail || mismatchRule.isFail(),
                currentStep.id(), scoreAfter(userAction.currentScore(), deduction + mismatchRule.deduction()), ruleIds(triggeredRules));
        }

        for (ScoreRule rule : allRules(currentProject)) {
            if (matches(rule.condition(), context)) {
                triggeredRules.add(rule);
                deduction += rule.deduction();
                fail = fail || rule.isFail();
            }
        }

        String nextStepId = nextStepId(applicableSteps, currentStep, completedStepIds);
        String message = triggeredRules.isEmpty()
            ? currentStep.correctFeedback()
            : triggeredRules.get(triggeredRules.size() - 1).message();
        return result(!fail, message, deduction, fail, nextStepId, scoreAfter(userAction.currentScore(), deduction), ruleIds(triggeredRules));
    }

    private List<ExamStep> applicableSteps(ExamProject project, VehicleType vehicleType, RoadScene roadScene) {
        return project.steps().stream()
            .filter(step -> step.appliesTo(vehicleType, roadScene))
            .sorted(Comparator.comparingInt(ExamStep::orderIndex))
            .toList();
    }

    private Optional<ScoreRule> findRule(ExamProject project, String condition) {
        return allRules(project).stream()
            .filter(rule -> rule.condition().equals(condition))
            .findFirst();
    }

    private List<ScoreRule> allRules(ExamProject project) {
        List<ScoreRule> rules = new ArrayList<>();
        rules.addAll(project.scoreRules());
        rules.addAll(project.failRules());
        return rules;
    }

    private ScoreRule timeoutRule(ExamProject project) {
        return findRule(project, "TIME_WINDOW_EXCEEDED")
            .orElse(new ScoreRule("timeout", project.id(), "TIME_WINDOW_EXCEEDED", 5, false, "操作超出建议时间窗口，扣 5 分"));
    }

    private boolean matches(String condition, Map<String, Object> context) {
        String[] parts = condition.split(":");
        if (parts.length < 2) {
            return false;
        }
        return switch (parts[0]) {
            case "CONTEXT_TRUE" -> asBoolean(context.get(parts[1]));
            case "CONTEXT_FALSE" -> context.containsKey(parts[1]) && !asBoolean(context.get(parts[1]));
            case "CONTEXT_EQUALS" -> parts.length >= 3 && String.valueOf(context.get(parts[1])).equals(parts[2]);
            case "CONTEXT_GT" -> parts.length >= 3 && asDouble(context.get(parts[1])) > Double.parseDouble(parts[2]);
            case "CONTEXT_BETWEEN" -> parts.length >= 4
                && asDouble(context.get(parts[1])) > Double.parseDouble(parts[2])
                && asDouble(context.get(parts[1])) <= Double.parseDouble(parts[3]);
            default -> false;
        };
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private String nextStepId(List<ExamStep> applicableSteps, ExamStep currentStep, Set<String> completedStepIds) {
        return applicableSteps.stream()
            .filter(step -> step.orderIndex() > currentStep.orderIndex())
            .filter(step -> !completedStepIds.contains(step.id()))
            .map(ExamStep::id)
            .findFirst()
            .orElse(null);
    }

    private int scoreAfter(int currentScore, int deduction) {
        int startingScore = currentScore <= 0 ? 100 : currentScore;
        return Math.max(startingScore - deduction, 0);
    }

    private List<String> ruleIds(List<ScoreRule> rules) {
        return rules.stream().map(ScoreRule::id).distinct().toList();
    }

    private EvaluationResult result(
        boolean success,
        String message,
        int deduction,
        boolean isFail,
        String nextStepId,
        int currentScore,
        List<String> triggeredRuleIds
    ) {
        return new EvaluationResult(success, message, deduction, isFail, nextStepId, currentScore, triggeredRuleIds);
    }
}
