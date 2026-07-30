package com.publication_trend_tracking_system.sever_web_app.config;

import com.publication_trend_tracking_system.sever_web_app.security.CustomUserDetailsService;
import com.publication_trend_tracking_system.sever_web_app.security.JwtAuthenticationFilter;
import com.publication_trend_tracking_system.sever_web_app.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final RateLimitFilter rateLimitFilter;

    private final org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(
                customUserDetailsService);

        provider.setPasswordEncoder(
                passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                // Without this the CorsConfigurationSource bean is ignored by the security chain and
                // preflight OPTIONS requests are rejected as unauthenticated before reaching it.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authenticationProvider(
                        authenticationProvider()
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/auth/**"
                        , "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/actuator/**")
                        .permitAll()

                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                "/api/payos/**",
                                "/api/payment/webhook")
                        .permitAll()

                        // Paper management (create/edit/delete) is an admin-only capability in the
                        // frontend (Admin > Paper Management), but the routes live under the shared
                        // /api/member/papers prefix — restrict the mutating verbs specifically so a
                        // regular MEMBER account can't call them directly. Reads (search, filters,
                        // get-by-id) stay open to MEMBER below.
                        .requestMatchers(HttpMethod.POST, "/api/member/papers/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/member/papers/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/member/papers/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/member/**")
                        .hasAnyRole(
                                "MEMBER",
                                "ADMIN"
                        )
                        .anyRequest()
                        .authenticated()


                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // After the JWT filter so an authenticated caller is counted by account rather than
                // by address — several users behind one NAT must not share a quota. Unauthenticated
                // requests, including every login attempt, still fall back to the client IP.
                .addFilterAfter(
                        rateLimitFilter,
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }
}