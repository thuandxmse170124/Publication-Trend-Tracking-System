package com.publication_trend_tracking_system.sever_web_app.controller;


import com.publication_trend_tracking_system.sever_web_app.dto.request.AuthenticationRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.RegisterRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthenticationResponse;


import com.publication_trend_tracking_system.sever_web_app.service.AuthenticationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService
            authenticationService;

    @PostMapping("/register")
    public ApiResponse<?> register(
            @RequestBody @Valid RegisterRequest request) {

        authenticationService.register(request);

        return ApiResponse.builder()
                .code(1000)
                .message("Register Success")
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse>
    login(
            @RequestBody @Valid
            AuthenticationRequest request) {

        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.authenticate(request))
                .build();
    }
}