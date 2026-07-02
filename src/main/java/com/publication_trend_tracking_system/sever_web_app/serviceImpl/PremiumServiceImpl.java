package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreatePremiumRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdatePremiumRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.UpdatePremiumStatusRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.PremiumResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.InvoiceRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PremiumRepository;
import com.publication_trend_tracking_system.sever_web_app.service.PremiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PremiumServiceImpl
        implements PremiumService {

    private final PremiumRepository premiumRepository;

    private final InvoiceRepository invoiceRepository;

    @Override
    public PremiumResponse createPremium(
            CreatePremiumRequest request
    ) {

        if (premiumRepository.existsByPackageName(request.getPackageName())) {

            throw new AppException(
                    ErrorCode.PREMIUM_ALREADY_EXISTS
            );
        }

        Premium premium =
                Premium.builder()
                        .packageName(request.getPackageName())
                        .amount(request.getAmount())
                        .durationDays(request.getDurationDays())
                        .description(request.getDescription())
                        .isActive(true)
                        .build();

        premium =
                premiumRepository.save(
                        premium
                );

        return mapToResponse(
                premium
        );
    }

    @Override
    public List<PremiumResponse> getAllPremiums() {

        return premiumRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PremiumResponse getPremium(
            Long premiumId
    ) {

        Premium premium =
                premiumRepository
                        .findById(premiumId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.PREMIUM_NOT_FOUND
                                ));

        return mapToResponse(
                premium
        );
    }

    @Override
    public PremiumResponse updatePremium(
            Long premiumId,
            UpdatePremiumRequest request
    ) {

        Premium premium =
                premiumRepository
                        .findById(premiumId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.PREMIUM_NOT_FOUND
                                ));

        if (!premium.getPackageName().equals(request.getPackageName())
                && premiumRepository.existsByPackageName(request.getPackageName())) {

            throw new AppException(
                    ErrorCode.PREMIUM_ALREADY_EXISTS
            );
        }

        premium.setPackageName(
                request.getPackageName()
        );

        premium.setAmount(
                request.getAmount()
        );

        premium.setDurationDays(
                request.getDurationDays()
        );

        premium.setDescription(
                request.getDescription()
        );

        premium =
                premiumRepository.save(
                        premium
                );

        return mapToResponse(
                premium
        );
    }

    @Override
    public PremiumResponse updateStatus(
            Long premiumId,
            UpdatePremiumStatusRequest request
    ) {

        Premium premium =
                premiumRepository
                        .findById(premiumId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.PREMIUM_NOT_FOUND
                                ));

        premium.setIsActive(
                request.getIsActive()
        );

        premium =
                premiumRepository.save(
                        premium
                );

        return mapToResponse(
                premium
        );
    }

    @Override
    public void deletePremium(
            Long premiumId
    ) {

        Premium premium =
                premiumRepository
                        .findById(premiumId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.PREMIUM_NOT_FOUND
                                ));

        if (invoiceRepository.existsByPremium(premium)) {

            throw new AppException(
                    ErrorCode.PREMIUM_ALREADY_USED
            );
        }

        premium.setIsActive(false);

        premiumRepository.save(premium);
    }

    private PremiumResponse mapToResponse(
            Premium premium
    ) {

        return PremiumResponse.builder()
                .premiumId(
                        premium.getPremiumId()
                )
                .packageName(
                        premium.getPackageName()
                )
                .amount(
                        premium.getAmount()
                )
                .durationDays(
                        premium.getDurationDays()
                )
                .description(
                        premium.getDescription()
                )
                .isActive(
                        premium.getIsActive()
                )
                .createdAt(
                        premium.getCreatedAt()
                )
                .updatedAt(
                        premium.getUpdatedAt()
                )
                .build();
    }
}