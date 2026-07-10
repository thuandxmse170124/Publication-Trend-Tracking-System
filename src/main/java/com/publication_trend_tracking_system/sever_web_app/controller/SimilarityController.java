package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.SimilarityResponseDTO;
import com.publication_trend_tracking_system.sever_web_app.service.SimilarityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/premium/similarity")
@RequiredArgsConstructor
@Tag(name = "Premium Similarity Analysis", description = "APIs for analyzing research paper similarities")
public class SimilarityController {

    private final SimilarityService similarityService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF research paper to find similar papers in database")
    public ResponseEntity<ApiResponse<SimilarityResponseDTO>> analyzeSimilarity(@RequestParam("file") MultipartFile file) {
        SimilarityResponseDTO responseDTO = similarityService.analyzeSimilarity(file);
        
        ApiResponse<SimilarityResponseDTO> apiResponse = ApiResponse.<SimilarityResponseDTO>builder()
                .code(HttpStatus.OK.value())
                .message("Similarity analysis completed successfully")
                .result(responseDTO)
                .build();
                
        return ResponseEntity.ok(apiResponse);
    }
}
