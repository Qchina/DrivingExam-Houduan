package com.honmen.drivingexam.model;

import java.time.LocalDateTime;

public class ErrorEntry {
    private final long userId;
    private final long questionId;
    private final int subject;
    private int errorCount;
    private String latestWrongAnswer;
    private boolean mastered;
    private LocalDateTime updatedAt;

    public ErrorEntry(long userId, long questionId, int subject, String latestWrongAnswer) {
        this.userId = userId;
        this.questionId = questionId;
        this.subject = subject;
        this.errorCount = 1;
        this.latestWrongAnswer = latestWrongAnswer;
        this.mastered = false;
        this.updatedAt = LocalDateTime.now();
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

    public int getErrorCount() {
        return errorCount;
    }

    public String getLatestWrongAnswer() {
        return latestWrongAnswer;
    }

    public boolean isMastered() {
        return mastered;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void recordWrongAnswer(String wrongAnswer) {
        this.errorCount++;
        this.latestWrongAnswer = wrongAnswer;
        this.mastered = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void markMastered() {
        this.mastered = true;
        this.updatedAt = LocalDateTime.now();
    }
}
