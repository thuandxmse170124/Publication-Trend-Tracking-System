package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateInvoiceRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.InvoiceResponse;

import java.util.List;

public interface InvoiceService {

    InvoiceResponse createInvoice(
            CreateInvoiceRequest request
    );

    List<InvoiceResponse> getMyInvoices();

    InvoiceResponse getInvoice(
            Long invoiceId
    );
    void cancelInvoice(
            Long invoiceId
    );
}