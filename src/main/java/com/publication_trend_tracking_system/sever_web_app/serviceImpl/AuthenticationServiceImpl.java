package com.publication_trend_tracking_system.sever_web_app.serviceImpl;



import com.publication_trend_tracking_system.sever_web_app.dto.request.AuthenticationRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.request.RegisterRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.AuthenticationResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Role;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.enums.RoleName;
import com.publication_trend_tracking_system.sever_web_app.repository.RoleRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.security.JwtService;

import com.publication_trend_tracking_system.sever_web_app.service.AuthenticationService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl
        implements AuthenticationService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    @Override
    public void register(RegisterRequest request) {

        if(userRepository.existsByEmail(
                request.getEmail())) {

            throw new RuntimeException(
                    "Email already exists");
        }

        Role memberRole =
                roleRepository
                        .findByRoleName(
                                RoleName.MEMBER.name())
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Role not found")
                        );

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
                        .orElseThrow();

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
}