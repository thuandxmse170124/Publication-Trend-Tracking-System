package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.*;
import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthenticationResponse;

public interface AuthenticationService {

    void register(
            RegisterRequest request);
    void verifyEmail(
            VerifyEmailRequest request);

    AuthenticationResponse authenticate(
            AuthenticationRequest request);

    void forgotPassword(
            ForgotPasswordRequest request);

    void resetPassword(
            ResetPasswordRequest request);
}
