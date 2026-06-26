package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateDiscountRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.DiscountResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Discount;
import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.DiscountRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PremiumRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.AdminDiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDiscountServiceImpl
        implements AdminDiscountService {

    private final PremiumRepository premiumRepository;

    private final DiscountRepository discountRepository;

    @Override
    public DiscountResponse createDiscount(
            CreateDiscountRequest request
    ) {

        Discount discount =
                Discount.builder()
                        .discountName(request.getDiscountName())
                        .discountPercent(request.getDiscountPercent())
                        .fromDate(request.getFromDate())
                        .toDate(request.getToDate())
                        .isActive(true)
                        .build();

        discount = discountRepository.save(discount);

        return mapToResponse(discount);
    }

    @Override
    public List<DiscountResponse> getAllDiscounts() {

        return discountRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void assignDiscount(
            Long premiumId,
            Long discountId
    ) {

        Premium premium =
                premiumRepository.findById(premiumId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.PREMIUM_NOT_FOUND));

        Discount discount =
                discountRepository.findById(discountId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.DISCOUNT_NOT_FOUND));

        if (premium.getDiscounts().contains(discount)) {

            throw new AppException(
                    ErrorCode.DISCOUNT_ALREADY_ASSIGNED
            );
        }

        premium.getDiscounts().add(discount);

        premiumRepository.save(premium);
    }

    @Override
    public DiscountResponse updateDiscount(
            Long discountId,
            CreateDiscountRequest request
    ) {

        Discount discount =
                discountRepository
                        .findById(discountId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.DISCOUNT_NOT_FOUND
                                ));

        discount.setDiscountName(
                request.getDiscountName()
        );

        discount.setDiscountPercent(
                request.getDiscountPercent()
        );

        discount.setFromDate(
                request.getFromDate()
        );

        discount.setToDate(
                request.getToDate()
        );

        discount =
                discountRepository.save(discount);

        return mapToResponse(discount);
    }

    @Override
    public void deleteDiscount(
            Long discountId
    ) {

        Discount discount =
                discountRepository
                        .findById(discountId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.DISCOUNT_NOT_FOUND
                                ));

        discount.setIsActive(false);

        discountRepository.save(discount);
    }

    @Override
    public void removeDiscountFromPremium(
            Long premiumId,
            Long discountId
    ) {

        Premium premium =
                premiumRepository
                        .findById(premiumId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.PREMIUM_NOT_FOUND
                                ));

        Discount discount =
                discountRepository
                        .findById(discountId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.DISCOUNT_NOT_FOUND
                                ));

        if (!premium.getDiscounts().contains(discount)) {

            throw new AppException(
                    ErrorCode.DISCOUNT_NOT_ASSIGNED
            );
        }

        premium.getDiscounts().remove(discount);

        premiumRepository.save(premium);
    }




    private DiscountResponse mapToResponse(
            Discount discount
    ) {

        return DiscountResponse.builder()
                .discountId(discount.getDiscountId())
                .discountName(discount.getDiscountName())
                .discountPercent(discount.getDiscountPercent())
                .fromDate(discount.getFromDate())
                .toDate(discount.getToDate())
                .isActive(discount.getIsActive())
                .build();
    }
}