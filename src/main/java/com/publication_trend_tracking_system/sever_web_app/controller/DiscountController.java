package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateDiscountRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.DiscountResponse;
import com.publication_trend_tracking_system.sever_web_app.service.AdminDiscountService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/discounts")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class DiscountController {


    private final AdminDiscountService adminDiscountService;

    @PostMapping
    public ApiResponse<DiscountResponse> createDiscount(
            @RequestBody CreateDiscountRequest request
    ) {

        return ApiResponse
                .<DiscountResponse>builder()
                .code(1000)
                .message("Create Discount Success")
                .result(adminDiscountService.createDiscount(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<DiscountResponse>> getAllDiscounts() {

        return ApiResponse
                .<List<DiscountResponse>>builder()
                .code(1000)
                .message("Get Discounts Success")
                .result(adminDiscountService.getAllDiscounts())
                .build();
    }

    @PostMapping("/{discountId}/premiums/{premiumId}")
    public ApiResponse<Void> assignDiscountToPremium(
            @PathVariable Long discountId,
            @PathVariable Long premiumId
    ) {

        adminDiscountService.assignDiscount(
                premiumId,
                discountId
        );

        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Assign Discount Success")
                .build();
    }
    @PutMapping("/{discountId}")
    public ApiResponse<DiscountResponse> updateDiscount(

            @PathVariable Long discountId,

            @RequestBody
            CreateDiscountRequest request
    ) {

        return ApiResponse
                .<DiscountResponse>builder()
                .code(1000)
                .message("Update Discount Success")
                .result(
                        adminDiscountService.updateDiscount(
                                discountId,
                                request
                        )
                )
                .build();
    }
    @DeleteMapping("/{discountId}")
    public ApiResponse<Void> deleteDiscount(

            @PathVariable Long discountId
    ) {

        adminDiscountService.deleteDiscount(
                discountId
        );

        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Delete Discount Success")
                .build();
    }
    @DeleteMapping("/{discountId}/premiums/{premiumId}")
    public ApiResponse<Void> removeDiscountFromPremium(

            @PathVariable Long discountId,

            @PathVariable Long premiumId
    ) {

        adminDiscountService.removeDiscountFromPremium(
                premiumId,
                discountId
        );

        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Remove Discount Success")
                .build();
    }
}