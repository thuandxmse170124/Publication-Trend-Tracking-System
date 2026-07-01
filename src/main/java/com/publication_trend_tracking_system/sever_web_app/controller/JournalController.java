package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.JournalResponse;
import com.publication_trend_tracking_system.sever_web_app.service.JournalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/journals")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @GetMapping
    public ApiResponse<org.springframework.data.domain.Page<JournalResponse>> getAllJournals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ApiResponse.<org.springframework.data.domain.Page<JournalResponse>>builder()
                .code(1000)
                .message("Get all journals success")
                .result(journalService.getAllJournals(pageable))
                .build();
    }

    @GetMapping("/{journalId}")
    public ApiResponse<JournalResponse> getJournalById(@PathVariable Integer journalId) {
        return ApiResponse.<JournalResponse>builder()
                .code(1000)
                .message("Get journal success")
                .result(journalService.getJournalById(journalId))
                .build();
    }
}
