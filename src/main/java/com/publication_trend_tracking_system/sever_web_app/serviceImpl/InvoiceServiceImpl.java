package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateInvoiceRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.InvoiceResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Discount;
import com.publication_trend_tracking_system.sever_web_app.entity.Invoice;
import com.publication_trend_tracking_system.sever_web_app.entity.Premium;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.enums.InvoiceStatus;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl
        implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    private final PremiumRepository premiumRepository;

    private final UserRepository userRepository;

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
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND
                                ));
        Optional<Invoice> pendingInvoice =
                invoiceRepository
                        .findFirstByUserAndStatusOrderByCreatedAtDesc(
                                user,
                                InvoiceStatus.PENDING
                        );

        if (pendingInvoice.isPresent()) {

            throw new AppException(
                    ErrorCode.PENDING_INVOICE_EXISTS
            );
        }



        Premium premium =
                premiumRepository
                        .findById(request.getPremiumId())
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.PREMIUM_NOT_FOUND
                                ));

        if (!premium.getIsActive()) {

            throw new AppException(
                    ErrorCode.PREMIUM_INACTIVE
            );
        }

        Discount discount = premium.getDiscount();

        Double discountPercent = 0.0;

        if (discount != null
                && Boolean.TRUE.equals(discount.getIsActive())
                && !LocalDateTime.now().isBefore(discount.getFromDate())
                && !LocalDateTime.now().isAfter(discount.getToDate())) {

            discountPercent = discount.getDiscountPercent();
        }

        BigDecimal originalAmount =
                premium.getAmount();

        BigDecimal discountAmount =
                originalAmount
                        .multiply(BigDecimal.valueOf(discountPercent))
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
                        .discount(discountPercent == 0 ? null : discount)

                        // Snapshot

                        .packageName(
                                premium.getPackageName()
                        )

                        .durationDays(
                                premium.getDurationDays()
                        )

                        .originalAmount(
                                originalAmount
                        )

                        .discountPercent(
                                discountPercent
                        )

                        .discountAmount(
                                discountAmount
                        )

                        .finalAmount(
                                finalAmount
                        )

                        .status(
                                InvoiceStatus.PENDING
                        )

                        .build();

        invoice =
                invoiceRepository.save(
                        invoice
                );

        return mapToResponse(
                invoice
        );
    }
    @Override
    public void cancelInvoice(
            Long invoiceId
    ){

        Invoice invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.INVOICE_NOT_FOUND
                                ));

        if(invoice.getStatus() != InvoiceStatus.PENDING){

            throw new AppException(
                    ErrorCode.INVALID_INVOICE_STATUS
            );
        }

        invoice.setStatus(
                InvoiceStatus.CANCELLED
        );

        invoiceRepository.save(invoice);
    }

    @Override
    public List<InvoiceResponse> getMyInvoices() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND
                                ));

        return invoiceRepository
                .findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public InvoiceResponse getInvoice(
            Long invoiceId
    ) {

        Invoice invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.INVOICE_NOT_FOUND
                                ));

        return mapToResponse(
                invoice
        );
    }

    private InvoiceResponse mapToResponse(
            Invoice invoice
    ) {

        return InvoiceResponse.builder()

                .invoiceId(
                        invoice.getInvoiceId()
                )

                .packageName(
                        invoice.getPackageName()
                )

                .durationDays(
                        invoice.getDurationDays()
                )
                .orderCode(
                        invoice.getOrderCode()
                )
                .originalAmount(
                        invoice.getOriginalAmount()
                )

                .discountPercent(
                        invoice.getDiscountPercent()
                )

                .discountAmount(
                        invoice.getDiscountAmount()
                )

                .finalAmount(
                        invoice.getFinalAmount()
                )

                .status(
                        invoice.getStatus()
                )

                .createdAt(
                        invoice.getCreatedAt()
                )

                .paidAt(
                        invoice.getPaidAt()
                )

                .build();
    }

}