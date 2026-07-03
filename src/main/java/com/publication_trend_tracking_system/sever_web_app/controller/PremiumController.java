package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreatePremiumRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdatePremiumRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdatePremiumStatusRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PremiumResponse;
import com.publication_trend_tracking_system.sever_web_app.service.PremiumService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/premiums")
@RequiredArgsConstructor
@SecurityRequirement(name = "api")
public class PremiumController {

    private final PremiumService premiumService;

    @PostMapping
    public ApiResponse<PremiumResponse> createPremium(
            @Valid @RequestBody CreatePremiumRequest request
    ) {

        return ApiResponse.<PremiumResponse>builder()
                .code(1000)
                .message("Create Premium Successfully")
                .result(premiumService.createPremium(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PremiumResponse>> getAllPremiums() {

        return ApiResponse.<List<PremiumResponse>>builder()
                .code(1000)
                .message("Get Premium Successfully")
                .result(premiumService.getAllPremiums())
                .build();
    }

    @GetMapping("/{premiumId}")
    public ApiResponse<PremiumResponse> getPremium(
            @PathVariable Long premiumId
    ) {

        return ApiResponse.<PremiumResponse>builder()
                .code(1000)
                .message("Get Premium Detail Successfully")
                .result(premiumService.getPremium(premiumId))
                .build();
    }

    @PutMapping("/{premiumId}")
    public ApiResponse<PremiumResponse> updatePremium(
            @PathVariable Long premiumId,
            @Valid @RequestBody UpdatePremiumRequest request
    ) {

        return ApiResponse.<PremiumResponse>builder()
                .code(1000)
                .message("Update Premium Successfully")
                .result(
                        premiumService.updatePremium(
                                premiumId,
                                request
                        )
                )
                .build();
    }

    @PatchMapping("/{premiumId}")
    public ApiResponse<PremiumResponse> updateStatus(
            @PathVariable Long premiumId,
            @RequestBody UpdatePremiumStatusRequest request
    ) {

        return ApiResponse.<PremiumResponse>builder()
                .code(1000)
                .message("Update Premium Status Successfully")
                .result(
                        premiumService.updateStatus(
                                premiumId,
                                request
                        )
                )
                .build();
    }

    @DeleteMapping("/{premiumId}")
    public ApiResponse<Void> deletePremium(
            @PathVariable Long premiumId
    ) {

        premiumService.deletePremium(premiumId);

        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Delete Premium Successfully")
                .build();
    }

}