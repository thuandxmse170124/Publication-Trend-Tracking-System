package com.publication_trend_tracking_system.sever_web_app.controller;


import com.publication_trend_tracking_system.sever_web_app.dto.request.AuthenticationRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.RegisterRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthenticationResponse;


import com.publication_trend_tracking_system.sever_web_app.serviceImpl.AuthenticationService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService
            authenticationService;

    @PostMapping("/register")
    public ApiResponse<?> register(
            @RequestBody RegisterRequest request) {

        authenticationService.register(request);

        return ApiResponse.builder()
                .code(1000)
                .message("Register Success")
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse>
    login(
            @RequestBody
            AuthenticationRequest request) {

        return ApiResponse
                .<AuthenticationResponse>builder()

                .code(1000)

                .message("Login Success")

                .result(
                        authenticationService
                                .authenticate(request))

                .build();
    }



}