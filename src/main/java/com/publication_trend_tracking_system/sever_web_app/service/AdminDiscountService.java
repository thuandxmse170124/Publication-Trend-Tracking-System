package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateDiscountRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.DiscountResponse;

import java.util.List;

public interface AdminDiscountService {
    DiscountResponse createDiscount(
            CreateDiscountRequest request
    );

    org.springframework.data.domain.Page<DiscountResponse> getAllDiscounts(org.springframework.data.domain.Pageable pageable);

    void assignDiscount(
            Long premiumId,
            Long discountId
    );
    DiscountResponse updateDiscount(
            Long discountId,
            CreateDiscountRequest request
    );

    void deleteDiscount(
            Long discountId
    );

    void removeDiscountFromPremium(
            Long premiumId,
            Long discountId
    );
}
