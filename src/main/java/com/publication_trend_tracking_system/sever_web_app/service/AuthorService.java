package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthorResponse;

public interface AuthorService {
    AuthorResponse getAuthorById(Long authorId);
}
