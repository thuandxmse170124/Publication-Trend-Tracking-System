package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateDiscountRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.DiscountResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Discount;
import com.publication_trend_tracking_system.sever_web_app.repository.DiscountRepository;
import com.publication_trend_tracking_system.sever_web_app.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl
        implements DiscountService {

    private final DiscountRepository discountRepository;

    @Override
    public DiscountResponse createDiscount(
            CreateDiscountRequest request
    ) {

        Discount discount =
                Discount.builder()
                        .discountName(
                                request.getDiscountName())
                        .discountPercent(
                                request.getDiscountPercent())
                        .fromDate(
                                request.getFromDate())
                        .toDate(
                                request.getToDate())
                        .isActive(true)
                        .build();

        discount =
                discountRepository.save(
                        discount);

        return mapToResponse(discount);
    }

    @Override
    public List<DiscountResponse>
    getAllDiscounts() {

        return discountRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private DiscountResponse mapToResponse(
            Discount discount
    ) {

        return DiscountResponse.builder()
                .discountId(
                        discount.getDiscountId())
                .discountName(
                        discount.getDiscountName())
                .discountPercent(
                        discount.getDiscountPercent())
                .fromDate(
                        discount.getFromDate())
                .toDate(
                        discount.getToDate())
                .isActive(
                        discount.getIsActive())
                .build();
    }
}