package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ResearchFieldResponse;
import com.publication_trend_tracking_system.sever_web_app.service.ResearchFieldService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/member/fields")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class ResearchFieldController {

    private final ResearchFieldService fieldService;

    @GetMapping
    public ApiResponse<org.springframework.data.domain.Page<ResearchFieldResponse>> getAllFields(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ApiResponse.<org.springframework.data.domain.Page<ResearchFieldResponse>>builder()
                .code(1000)
                .message("Get all fields success")
                .result(fieldService.getAllFields(pageable))
                .build();
    }
}
