package com.honmen.drivingexam.subject3;

public record ScoreRule(
    String id,
    String projectId,
    String condition,
    int deduction,
    boolean isFail,
    String message
) {
}
