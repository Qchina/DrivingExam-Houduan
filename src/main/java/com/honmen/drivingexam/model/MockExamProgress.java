package com.honmen.drivingexam.model;

import java.time.LocalDateTime;
import java.util.List;

public record MockExamProgress(
    long id,
    long userId,
    int subject,
    List<Long> questionIds,
    List<String> selectedAnswers,
    List<Boolean> revealedAnswers,
    int currentIndex,
    int remainingSeconds,
    LocalDateTime updatedAt
) {
}
