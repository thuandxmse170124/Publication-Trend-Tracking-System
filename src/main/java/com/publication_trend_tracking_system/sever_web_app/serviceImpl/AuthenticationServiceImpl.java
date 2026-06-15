package com.publication_trend_tracking_system.sever_web_app.serviceImpl;



import com.publication_trend_tracking_system.sever_web_app.dto.request.AuthenticationRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.ForgotPasswordRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.RegisterRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.ResetPasswordRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthenticationResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.PasswordResetToken;
import com.publication_trend_tracking_system.sever_web_app.entity.Role;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.enums.RoleName;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.PasswordResetTokenRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.RoleRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.security.JwtService;

import com.publication_trend_tracking_system.sever_web_app.service.AuthenticationService;
import com.publication_trend_tracking_system.sever_web_app.service.EmailService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl
        implements AuthenticationService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final EmailService emailService;


    @Override
    public void register(RegisterRequest request) {

        if(userRepository.existsByEmail(
                request.getEmail())) {

            throw new AppException(
                    ErrorCode.EMAIL_EXISTED);

        }

        Role memberRole =
                roleRepository
                        .findByRoleName(
                                RoleName.MEMBER.name())
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.ROLE_NOT_FOUND));

        User user = User.builder()
                .fullName(
                        request.getFullName())
                .email(
                        request.getEmail())
                .passwordHash(
                        passwordEncoder.encode(
                                request.getPassword()))
                .role(memberRole)
                .build();

        userRepository.save(user);
    }

    @Override
    public AuthenticationResponse authenticate(
            AuthenticationRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user =
                userRepository
                        .findByEmail(
                                request.getEmail())
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        String token =
                jwtService.generateToken(user);

        return AuthenticationResponse
                .builder()
                .token(token)
                .role(
                        user.getRole()
                                .getRoleName())
                .premium(false)
                .build();
    }

    @Override
    public void forgotPassword(
            ForgotPasswordRequest request) {

        userRepository
                .findByEmail(request.getEmail())
                .ifPresent(user -> {

                    String token =
                            UUID.randomUUID().toString();

                    PasswordResetToken resetToken =
                            PasswordResetToken.builder()
                                    .token(token)
                                    .expiryTime(
                                            LocalDateTime.now()
                                                    .plusMinutes(15))
                                    .user(user)
                                    .build();

                    passwordResetTokenRepository
                            .save(resetToken);

                    String resetLink =
                            "http://localhost:3000/reset-password?token="
                                    + token;

                    emailService.sendEmail(
                            user.getEmail(),
                            "Reset Password",
                            "Click the link below to reset your password:\n"
                                    + resetLink);
                });
    }

    @Override
    public void resetPassword(
            ResetPasswordRequest request) {

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(
                                request.getToken())
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.INVALID_TOKEN));

        if (resetToken.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new AppException(
                    ErrorCode.TOKEN_EXPIRED);
        }

        User user =
                resetToken.getUser();

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()));

        userRepository.save(user);

        passwordResetTokenRepository
                .delete(resetToken);
    }


}