package com.honmen.drivingexam.subject3;

import java.util.List;

public record EvaluationResult(
    boolean success,
    String message,
    int deduction,
    boolean isFail,
    String nextStepId,
    int currentScore,
    List<String> triggeredRuleIds
) {
}
