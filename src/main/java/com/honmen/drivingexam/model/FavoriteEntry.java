package com.honmen.drivingexam.model;

import java.time.LocalDateTime;

public record FavoriteEntry(long userId, long questionId, int subject, LocalDateTime createdAt) {
}
