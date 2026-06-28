package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.PremiumResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
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

    @Override
    public List<PremiumResponse> getAllPremiums() {

        return premiumRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PremiumResponse toResponse(
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
                .build();
    }
}