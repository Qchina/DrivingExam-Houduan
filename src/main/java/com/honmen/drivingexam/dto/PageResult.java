package com.honmen.drivingexam.dto;

import java.util.List;

public record PageResult<T>(long total, int page, int limit, List<T> list) {
}
