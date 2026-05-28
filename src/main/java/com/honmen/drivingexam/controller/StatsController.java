package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.dto.BusinessDtos.StatsOverview;
import com.honmen.drivingexam.service.DrivingExamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {
    private final DrivingExamService service;

    public StatsController(DrivingExamService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public ApiResponse<StatsOverview> overview(@RequestHeader(value = "Authorization", required = false) String authorization) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.statsOverview(userId));
    }
}
