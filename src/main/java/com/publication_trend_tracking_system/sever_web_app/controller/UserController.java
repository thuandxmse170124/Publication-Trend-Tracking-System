package com.publication_trend_tracking_system.sever_web_app.controller;


import com.publication_trend_tracking_system.sever_web_app.dto.request.ChangePasswordRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.UserResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;

import com.publication_trend_tracking_system.sever_web_app.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    private final UserService userService;


    @GetMapping("/me")
    public UserResponse getMyInfo(
            Authentication authentication
    ) {

        String email =
                authentication.getName();
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(
                        user.getRole()
                                .getRoleName()
                )
                .build();
    }

    @PutMapping("/change-password")
    public ApiResponse<?> changePassword(
            @RequestBody @Valid
            ChangePasswordRequest request) {

        userService.changePassword(request);

        return ApiResponse.<Void>builder()
                .message("Change password success")
                .build();
    }


}
