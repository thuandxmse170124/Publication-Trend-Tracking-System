package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.PaperRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PaperResponse;
import com.publication_trend_tracking_system.sever_web_app.service.PaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @PostMapping
    public ApiResponse<PaperResponse> createPaper(@Valid @RequestBody PaperRequest request) {
        return ApiResponse.<PaperResponse>builder()
                .code(1000)
                .message("Create paper success")
                .result(paperService.createPaper(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PaperResponse>> getAllPapers(
            @RequestParam(required = false) String keyword) {

        return ApiResponse.<List<PaperResponse>>builder()
                .code(1000)
                .message("Get papers success")
                .result(paperService.getAllPapers(keyword))
                .build();
    }

    @GetMapping("/{paperId}")
    public ApiResponse<PaperResponse> getPaperById(@PathVariable Long paperId) {
        return ApiResponse.<PaperResponse>builder()
                .code(1000)
                .message("Get paper success")
                .result(paperService.getPaperById(paperId))
                .build();
    }

    @PutMapping("/{paperId}")
    public ApiResponse<PaperResponse> updatePaper(
            @PathVariable Long paperId,
            @Valid @RequestBody PaperRequest request) {

        return ApiResponse.<PaperResponse>builder()
                .code(1000)
                .message("Update paper success")
                .result(paperService.updatePaper(paperId, request))
                .build();
    }

    @DeleteMapping("/{paperId}")
    public ApiResponse<?> deletePaper(@PathVariable Long paperId) {
        paperService.deletePaper(paperId);

        return ApiResponse.builder()
                .code(1000)
                .message("Delete paper success")
                .build();
    }
}
