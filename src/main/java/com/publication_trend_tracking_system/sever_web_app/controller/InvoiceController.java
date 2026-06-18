package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateInvoiceRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.InvoiceResponse;
import com.publication_trend_tracking_system.sever_web_app.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ApiResponse<InvoiceResponse>
    createInvoice(
            @RequestBody
            CreateInvoiceRequest request
    ) {

        return ApiResponse
                .<InvoiceResponse>builder()
                .code(1000)
                .message(
                        "Create Invoice Success")
                .result(
                        invoiceService
                                .createInvoice(
                                        request))
                .build();
    }
}