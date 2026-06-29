package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateInvoiceRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.InvoiceResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Discount;
import com.publication_trend_tracking_system.sever_web_app.entity.Invoice;
import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.repository.DiscountRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.InvoiceRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PremiumRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl
        implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    private final UserRepository userRepository;

    private final PremiumRepository premiumRepository;

    private final DiscountRepository discountRepository;

    @Override
    public InvoiceResponse createInvoice(
            CreateInvoiceRequest request
    ) {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow();

        Premium premium =
                premiumRepository
                        .findById(request.getPremiumId())
                        .orElseThrow();

        BigDecimal originalAmount =
                premium.getAmount();

        BigDecimal discountPercent =
                BigDecimal.ZERO;

//        Tạo invoice trước sau có admin thì sửa lại logic
        Discount discount =
                discountRepository
                        .findFirstByIsActiveTrue()
                        .orElse(null);

        if(discount != null) {
            discountPercent =
                    BigDecimal.valueOf(
                            discount.getDiscountPercent()
                    );
        }

        BigDecimal discountAmount =
                originalAmount
                        .multiply(discountPercent)
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal finalAmount =
                originalAmount.subtract(
                        discountAmount
                );

        Invoice invoice =
                Invoice.builder()
                        .user(user)
                        .premium(premium)
                        .discount(discount)
                        .originalAmount(
                                originalAmount)
                        .discountAmount(
                                discountAmount)
                        .finalAmount(
                                finalAmount)
                        .status("PENDING")
                        .createdAt(
                                LocalDateTime.now())
                        .build();

        invoice =
                invoiceRepository
                        .save(invoice);

        return InvoiceResponse.builder()
                .invoiceId(
                        invoice.getInvoiceId())
                .packageName(
                        premium.getPackageName())
                .originalAmount(
                        originalAmount)
                .discountAmount(
                        discountAmount)
                .finalAmount(
                        finalAmount)
                .status(
                        invoice.getStatus())
                .build();
    }

}
