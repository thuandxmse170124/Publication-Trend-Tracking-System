package com.publication_trend_tracking_system.sever_web_app.serviceImpl;


import com.publication_trend_tracking_system.sever_web_app.dto.request.ChangePasswordRequest;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl
        implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void changePassword(
            ChangePasswordRequest request) {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPasswordHash())) {

            throw new AppException(
                    ErrorCode.OLD_PASSWORD_INCORRECT);
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()));

        userRepository.save(user);
    }
}
