package com.publication_trend_tracking_system.sever_web_app.serviceImpl;



import com.publication_trend_tracking_system.sever_web_app.dto.request.*;
import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthenticationResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.PasswordResetToken;
import com.publication_trend_tracking_system.sever_web_app.entity.PendingRegistration;
import com.publication_trend_tracking_system.sever_web_app.entity.Role;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.enums.RoleName;
import com.publication_trend_tracking_system.sever_web_app.enums.UserStatus;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.PasswordResetTokenRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PendingRegistrationRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.RoleRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.security.JwtService;

import com.publication_trend_tracking_system.sever_web_app.service.AuthenticationService;
import com.publication_trend_tracking_system.sever_web_app.service.EmailService;
import lombok.RequiredArgsConstructor;


import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationServiceImpl
        implements AuthenticationService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PendingRegistrationRepository
            pendingRegistrationRepository;

    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final PasswordResetTokenRepository passwordResetTokenRepository;


    @Override
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(
                request.getEmail())) {

            throw new AppException(
                    ErrorCode.EMAIL_EXISTED);
        }

        pendingRegistrationRepository
                .findByEmail(request.getEmail())
                .ifPresent(
                        pendingRegistrationRepository::delete
                );

        String otp =
                String.valueOf(
                        ThreadLocalRandom.current()
                                .nextInt(100000, 999999));

        PendingRegistration pending =
                PendingRegistration.builder()
                        .fullName(
                                request.getFullName())
                        .email(
                                request.getEmail())
                        .passwordHash(
                                passwordEncoder.encode(
                                        request.getPassword()))
                        .otpCode(otp)
                        .otpExpiredAt(
                                LocalDateTime.now()
                                        .plusMinutes(5))
                        .build();

        pendingRegistrationRepository.save(
                pending);

        emailService.sendEmail(
                request.getEmail(),
                "Email Verification",
                "Your OTP code is: " + otp
        );
    }

    @Override
    public void verifyEmail(
            VerifyEmailRequest request) {

        PendingRegistration pending =
                pendingRegistrationRepository
                        .findByEmailAndOtpCode(
                                request.getEmail(),
                                request.getOtp())
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.OTP_INVALID));

        if(LocalDateTime.now()
                .isAfter(
                        pending.getOtpExpiredAt())) {

            throw new AppException(
                    ErrorCode.OTP_EXPIRED);
        }

        Role memberRole =
                roleRepository
                        .findByRoleName(
                                RoleName.MEMBER.name())
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.ROLE_NOT_FOUND));

        User user =
                User.builder()
                        .fullName(
                                pending.getFullName())
                        .email(
                                pending.getEmail())
                        .passwordHash(
                                pending.getPasswordHash())
                        .role(memberRole)
                        .build();

        userRepository.save(user);

        pendingRegistrationRepository.delete(
                pending);
    }


    @Override
    public AuthenticationResponse authenticate(
            AuthenticationRequest request) {


        User user =
                userRepository
                        .findByEmail(
                                request.getEmail())
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.UNAUTHENTICATED));
        if (user.getStatus()
                == UserStatus.INACTIVE) {

            throw new AppException(
                    ErrorCode.USER_INACTIVE
            );
        }

        if (user.getStatus()
                == UserStatus.BANNED) {

            throw new AppException(
                    ErrorCode.USER_BANNED
            );
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );



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

        User user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        // Xóa OTP cũ
        passwordResetTokenRepository
                .findByUser(user)
                .ifPresent(
                        passwordResetTokenRepository::delete
                );

        String otp =
                String.valueOf(
                        ThreadLocalRandom.current()
                                .nextInt(100000, 999999));

        PasswordResetToken resetToken =
                PasswordResetToken.builder()
                        .otpCode(otp)
                        .expiryTime(
                                LocalDateTime.now()
                                        .plusMinutes(5))
                        .user(user)
                        .build();

        // Lưu OTP mới
        passwordResetTokenRepository
                .save(resetToken);

        // Gửi email
        emailService.sendEmail(
                user.getEmail(),
                "Password Reset OTP",
                "Your OTP code is: "
                        + otp
                        + "\nOTP expires in 5 minutes."
        );
    }
    @Override
    public void resetPassword(
            ResetPasswordRequest request) {

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new AppException(
                    ErrorCode.PASSWORD_NOT_MATCH);
        }

        PasswordResetToken otpRecord =
                passwordResetTokenRepository
                        .findByOtpCode(
                                request.getOtp())
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.OTP_INVALID));

        if (otpRecord.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new AppException(
                    ErrorCode.OTP_EXPIRED);
        }

        User user = otpRecord.getUser();

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()));

        userRepository.save(user);

        passwordResetTokenRepository
                .delete(otpRecord);
    }


}