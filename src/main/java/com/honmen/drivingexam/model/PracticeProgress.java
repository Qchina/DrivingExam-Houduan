package com.honmen.drivingexam.model;

public class PracticeProgress {
    private final long userId;
    private final int subject;
    private long lastQuestionId;
    private int totalAnswered;
    private int totalCorrect;
    private int totalWrong;

    public PracticeProgress(long userId, int subject) {
        this.userId = userId;
        this.subject = subject;
    }

    public long getUserId() {
        return userId;
    }

    public int getSubject() {
        return subject;
    }

    public long getLastQuestionId() {
        return lastQuestionId;
    }

    public int getTotalAnswered() {
        return totalAnswered;
    }

    public int getTotalCorrect() {
        return totalCorrect;
    }

    public int getTotalWrong() {
        return totalWrong;
    }

    public void sync(long lastQuestionId, int answeredDelta, int correctDelta, int wrongDelta) {
        this.lastQuestionId = lastQuestionId;
        this.totalAnswered += Math.max(answeredDelta, 0);
        this.totalCorrect += Math.max(correctDelta, 0);
        this.totalWrong += Math.max(wrongDelta, 0);
    }
}
