package com.example.library.gateway.config;

import com.example.library.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class JwtConfiguration {
    @Bean
    JwtService jwtService(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.expiration:PT1H}") Duration expiration) {
        return new JwtService(secret, expiration);
    }
}
