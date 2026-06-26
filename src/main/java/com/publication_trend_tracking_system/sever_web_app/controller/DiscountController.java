package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateDiscountRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.DiscountResponse;
import com.publication_trend_tracking_system.sever_web_app.service.DiscountService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/discounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "api")

public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    public ApiResponse<DiscountResponse>
    createDiscount(
            @RequestBody
            CreateDiscountRequest request
    ) {

        return ApiResponse
                .<DiscountResponse>builder()
                .code(1000)
                .message(
                        "Create Discount Success")
                .result(
                        discountService
                                .createDiscount(
                                        request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<DiscountResponse>>
    getAllDiscounts() {

        return ApiResponse
                .<List<DiscountResponse>>builder()
                .code(1000)
                .message(
                        "Get Discounts Success")
                .result(
                        discountService
                                .getAllDiscounts())
                .build();
    }
}