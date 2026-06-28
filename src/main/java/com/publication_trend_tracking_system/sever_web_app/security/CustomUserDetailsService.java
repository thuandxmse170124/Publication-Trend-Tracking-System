package com.publication_trend_tracking_system.sever_web_app.security;

import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        return userRepository
                .findByEmail(email)
                .map(UserPrincipal::new)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"));
    }
}