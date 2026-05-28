package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.dto.PageResult;
import com.honmen.drivingexam.model.Question;
import com.honmen.drivingexam.service.DrivingExamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {
    private final DrivingExamService service;

    public QuestionController(DrivingExamService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<Question>> list(
        @RequestParam int subject,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(service.listQuestions(subject, type, page, limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<Question> detail(@PathVariable long id) {
        return ApiResponse.success(service.getQuestion(id));
    }

    @GetMapping("/random")
    public ApiResponse<List<Question>> random(
        @RequestParam int subject,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(service.randomQuestions(subject, limit));
    }

    @GetMapping("/batch")
    public ApiResponse<List<Question>> batch(@RequestParam String ids) {
        return ApiResponse.success(service.batchQuestions(ids));
    }
}
