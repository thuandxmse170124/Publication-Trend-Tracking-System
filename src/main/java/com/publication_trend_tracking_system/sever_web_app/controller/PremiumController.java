package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PremiumResponse;
import com.publication_trend_tracking_system.sever_web_app.service.PremiumService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/premiums")
@RequiredArgsConstructor
@SecurityRequirement(name = "api")

public class PremiumController {

    private final PremiumService premiumService;

    @GetMapping
    public ApiResponse<List<PremiumResponse>>
    getAllPremiums() {

        return ApiResponse
                .<List<PremiumResponse>>builder()

                .code(1000)

                .message("Get Premium Packages Success")

                .result(
                        premiumService
                                .getAllPremiums()
                )

                .build();
    }
}