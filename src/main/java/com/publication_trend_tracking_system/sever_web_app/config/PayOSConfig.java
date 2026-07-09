package com.publication_trend_tracking_system.sever_web_app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(PayOSProperties.class)
public class PayOSConfig {

    private final PayOSProperties properties;

    @Bean
    public PayOS payOS() {

        System.out.println("ClientId = " + properties.getClientId());
        System.out.println("ApiKey = " + properties.getApiKey());
        System.out.println("Checksum = " + properties.getChecksumKey());

        return new PayOS(
                properties.getClientId(),
                properties.getApiKey(),
                properties.getChecksumKey()
        );
    }
}