package com.honmen.drivingexam.model;

import java.time.LocalDateTime;
import java.util.List;

public record ExamHistory(
    long id,
    long userId,
    int subject,
    int score,
    int timeUsed,
    int isPassed,
    List<Long> wrongQuestionIds,
    List<Long> questionIds,
    List<String> selectedAnswers,
    LocalDateTime createdAt
) {
}
