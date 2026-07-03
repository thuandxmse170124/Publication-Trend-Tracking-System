package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateInvoiceRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.InvoiceResponse;
import com.publication_trend_tracking_system.sever_web_app.service.InvoiceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/invoices")
@RequiredArgsConstructor
@SecurityRequirement(name = "api")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ApiResponse<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request
    ) {

        return ApiResponse.<InvoiceResponse>builder()
                .code(1000)
                .message("Create Invoice Successfully")
                .result(
                        invoiceService.createInvoice(request)
                )
                .build();
    }

    @GetMapping
    public ApiResponse<List<InvoiceResponse>> getMyInvoices() {

        return ApiResponse.<List<InvoiceResponse>>builder()
                .code(1000)
                .message("Get My Invoices Successfully")
                .result(
                        invoiceService.getMyInvoices()
                )
                .build();
    }

    @GetMapping("/{invoiceId}")
    public ApiResponse<InvoiceResponse> getInvoice(
            @PathVariable Long invoiceId
    ) {

        return ApiResponse.<InvoiceResponse>builder()
                .code(1000)
                .message("Get Invoice Successfully")
                .result(
                        invoiceService.getInvoice(invoiceId)
                )
                .build();
    }

    @PatchMapping("/{invoiceId}/cancel")
    public ApiResponse<Void> cancelInvoice(
            @PathVariable Long invoiceId
    ){

        invoiceService.cancelInvoice(invoiceId);

        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Cancel Invoice Successfully")
                .build();
    }
}