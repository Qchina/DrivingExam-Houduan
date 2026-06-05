package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.subject3.EvaluateActionRequest;
import com.honmen.drivingexam.subject3.EvaluateTextRequest;
import com.honmen.drivingexam.subject3.EvaluationResult;
import com.honmen.drivingexam.subject3.ProjectConfig;
import com.honmen.drivingexam.subject3.Subject3TrainingService;
import com.honmen.drivingexam.subject3.TextEvaluationResult;
import com.honmen.drivingexam.subject3.VehicleType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/subject3", produces = "application/json;charset=UTF-8")
public class Subject3Controller {
    private final Subject3TrainingService service;

    public Subject3Controller(Subject3TrainingService service) {
        this.service = service;
    }

    @GetMapping("/projects")
    public ApiResponse<List<ProjectConfig>> projects(
        @RequestParam(required = false) VehicleType vehicleType,
        @RequestParam(defaultValue = "default") String region
    ) {
        return ApiResponse.success(service.listProjects(vehicleType, region));
    }

    @GetMapping("/projects/{projectId}")
    public ApiResponse<ProjectConfig> project(
        @PathVariable String projectId,
        @RequestParam(required = false) VehicleType vehicleType,
        @RequestParam(defaultValue = "default") String region
    ) {
        return ApiResponse.success(service.getProject(projectId, vehicleType, region));
    }

    @PostMapping("/evaluate")
    public ApiResponse<EvaluationResult> evaluate(@Valid @RequestBody EvaluateActionRequest request) {
        return ApiResponse.success(service.evaluate(request));
    }

    @PostMapping("/evaluate-text")
    public ApiResponse<TextEvaluationResult> evaluateText(@Valid @RequestBody EvaluateTextRequest request) {
        return ApiResponse.success(service.evaluateText(request));
    }
}
