package com.honmen.drivingexam.model;

import java.time.LocalDateTime;

public class PracticeRecord {
    private final long id;
    private final long userId;
    private final long questionId;
    private final int subject;
    private final String latestAnswer;
    private final int latestResult;
    private final LocalDateTime answeredAt;
    private final LocalDateTime updatedAt;

    public PracticeRecord(
        long id,
        long userId,
        long questionId,
        int subject,
        String latestAnswer,
        int latestResult,
        LocalDateTime answeredAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.questionId = questionId;
        this.subject = subject;
        this.latestAnswer = latestAnswer;
        this.latestResult = latestResult;
        this.answeredAt = answeredAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public long getQuestionId() {
        return questionId;
    }

    public int getSubject() {
        return subject;
    }

    public String getLatestAnswer() {
        return latestAnswer;
    }

    public int getLatestResult() {
        return latestResult;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
