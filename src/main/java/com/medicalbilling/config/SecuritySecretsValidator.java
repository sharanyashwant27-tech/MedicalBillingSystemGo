package com.medicalbilling.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SecuritySecretsValidator implements InitializingBean {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Override
    public void afterPropertiesSet() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET is required. Set it in the environment or .env file — never in URLs or source code.");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must be at least 32 characters.");
        }
    }
}
