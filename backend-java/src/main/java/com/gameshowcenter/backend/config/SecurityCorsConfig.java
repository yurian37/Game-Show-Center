package com.gameshowcenter.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Hardened CORS Configuration conforming to SOC 2 CC6.6 and ISO/IEC 27001:2022 A.8.20/A.8.22.
 * Replaces wildcard '*' origins with a declarative whitelist loaded from application properties
 * or environment variables.
 */
@Configuration
public class SecurityCorsConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SecurityCorsConfig.class);

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,https://gameshowcenter.com}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        log.info("Configuring hardened CORS policy with allowed origins: {}", Arrays.toString(origins));

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Requested-With")
                .exposedHeaders("Content-Disposition", "Content-Length")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
