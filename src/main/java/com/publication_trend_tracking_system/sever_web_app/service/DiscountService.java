package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateDiscountRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.DiscountResponse;

import java.util.List;

public interface DiscountService {

    DiscountResponse createDiscount(
            CreateDiscountRequest request
    );

    List<DiscountResponse> getAllDiscounts();
}