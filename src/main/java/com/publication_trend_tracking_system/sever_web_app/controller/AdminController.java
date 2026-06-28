package com.publication_trend_tracking_system.sever_web_app.controller;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.UserResponse;
import com.publication_trend_tracking_system.sever_web_app.service.AdminUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;

    @GetMapping("/users")
    public ApiResponse<List<UserResponse>>
    getAllUsers() {

        return ApiResponse
                .<List<UserResponse>>builder()
                .code(1000)
                .message(
                        "Get Users Success")
                .result(
                        adminUserService
                                .getAllUsers())
                .build();
    }

    @PutMapping("/users/{id}/activate")
    public ApiResponse<Void>
    activateUser(
            @PathVariable Long id
    ) {

        adminUserService
                .activateUser(id);

        return ApiResponse
                .<Void>builder()
                .code(1000)
                .message(
                        "User Activated")
                .build();
    }

    @PutMapping("/users/{id}/deactivate")
    public ApiResponse<Void>
    deactivateUser(
            @PathVariable Long id
    ) {

        adminUserService
                .deactivateUser(id);

        return ApiResponse
                .<Void>builder()
                .code(1000)
                .message(
                        "User Deactivated")
                .build();
    }

    @PutMapping("/users/{id}/ban")
    public ApiResponse<Void>
    banUser(
            @PathVariable Long id
    ) {

        adminUserService
                .banUser(id);

        return ApiResponse
                .<Void>builder()
                .code(1000)
                .message(
                        "User Banned")
                .build();
    }
}