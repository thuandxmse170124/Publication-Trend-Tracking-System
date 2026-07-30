package com.publication_trend_tracking_system.sever_web_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Cross-origin policy for the browser client.
 *
 * <p>Nothing configured this before. It worked in development only because Vite proxies /api to
 * this server, so the browser saw one origin. The moment the frontend is served from its own
 * domain — any real deployment — every request would fail preflight.
 *
 * <p>Origins come from configuration and are listed explicitly. A wildcard is not an option here:
 * {@code allowCredentials} is on so the Authorization header survives the round trip, and the CORS
 * spec forbids pairing that with {@code *} — Spring throws at startup rather than let it through.
 * Listing origins is also the safer default, since a wildcard lets any site on the internet call
 * this API with a logged-in user's token.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.max-age-seconds:3600}")
    private long maxAgeSeconds;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList());

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        // The client reads these to show a "slow down" message instead of a bare failure.
        config.setExposedHeaders(List.of("Retry-After", "X-RateLimit-Limit", "X-RateLimit-Remaining"));
        config.setAllowCredentials(true);
        // Cache the preflight so the browser stops re-asking before every mutating request.
        config.setMaxAge(maxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
