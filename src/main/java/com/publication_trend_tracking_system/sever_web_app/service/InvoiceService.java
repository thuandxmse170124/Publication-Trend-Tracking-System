package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.CreateInvoiceRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.InvoiceResponse;

public interface InvoiceService {

    InvoiceResponse createInvoice(
            CreateInvoiceRequest request
    );
}