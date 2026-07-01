package com.publication_trend_tracking_system.sever_web_app.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "payos")
public class PayOSProperties {

    private String clientId;

    private String apiKey;

    private String checksumKey;

}