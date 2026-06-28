package com.publication_trend_tracking_system.sever_web_app;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "PuclicTracker API", version = "1.0", description = "Information"))
@SecurityScheme(
		name = "api", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT"
)
public class SeverWebAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				SeverWebAppApplication.class,
				args
		);
	}
}
