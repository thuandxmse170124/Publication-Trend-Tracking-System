package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.response.UserResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.enums.UserStatus;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl
        implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    public org.springframework.data.domain.Page<UserResponse> getAllUsers(org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    public void activateUser(Long userId) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND
                                ));

        user.setStatus(
                UserStatus.ACTIVE
        );

        userRepository.save(user);
    }

    @Override
    public void deactivateUser(Long userId) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND
                                ));
        if(user.getRole().getRoleName()
                .equals("ADMIN")) {

            throw new AppException(
                    ErrorCode.CANNOT_MODIFY_ADMIN
            );
        }
        user.setStatus(
                UserStatus.INACTIVE
        );

        userRepository.save(user);
    }

    @Override
    public void banUser(Long userId) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND
                                ));
        if(user.getRole().getRoleName()
                .equals("ADMIN")) {

            throw new AppException(
                    ErrorCode.CANNOT_MODIFY_ADMIN
            );
        }
        user.setStatus(
                UserStatus.BANNED
        );

        userRepository.save(user);
    }

    private UserResponse toResponse(
            User user
    ) {

        return UserResponse.builder()
                .userId(
                        user.getUserId())
                .fullName(
                        user.getFullName())
                .email(
                        user.getEmail())
                .role(
                        user.getRole()
                                .getRoleName())
                .status(
                        user.getStatus())
                .build();
    }
}