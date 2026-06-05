package com.honmen.drivingexam.subject3;

import java.util.List;

public record TextEvaluationResult(
    int score,
    boolean passed,
    List<StandardStep> missingSteps,
    List<String> orderErrors,
    List<String> extraSteps,
    List<MatchedStep> matchedSteps,
    List<String> feedback
) {
    public record MatchedStep(String stepId, String title, String matchedAlias, int order) {
    }
}
