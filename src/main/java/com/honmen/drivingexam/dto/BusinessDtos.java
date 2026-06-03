package com.honmen.drivingexam.dto;

import com.honmen.drivingexam.model.Question;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class BusinessDtos {
    private BusinessDtos() {
    }

    public record ErrorRequest(@NotNull Long questionId, @NotNull Integer subject, @NotNull String wrongAnswer) {
    }

    public record FavoriteRequest(@NotNull Long questionId, @NotNull Integer subject) {
    }

    public record ExamSubmitRequest(
        @NotNull Integer subject,
        @NotNull @Min(0) Integer score,
        @NotNull @Min(0) Integer timeUsed,
        @NotNull Integer isPassed,
        List<Long> wrongQuestionIds
    ) {
    }

    public record ProgressRequest(
        @NotNull Integer subject,
        @NotNull Long lastQuestionId,
        @NotNull @Min(0) Integer answeredDelta,
        @NotNull @Min(0) Integer correctDelta,
        @NotNull @Min(0) Integer wrongDelta,
        Long questionId,
        @Size(max = 10) String latestAnswer,
        Integer latestResult
    ) {
    }

    public record ErrorQuestion(
        Question question,
        int errorCount,
        String latestWrongAnswer,
        int isMastered,
        LocalDateTime updatedAt
    ) {
    }

    public record FavoriteQuestion(Question question, LocalDateTime createdAt) {
    }

    public record PracticeQuestionStatus(long questionId, boolean answered, boolean correct) {
    }

    public record SubjectOverview(int totalAnswered, int totalCorrect, int totalWrong, String accuracy, long lastQuestionId) {
    }

    public record StatsOverview(SubjectOverview subject1, SubjectOverview subject4) {
    }
}
