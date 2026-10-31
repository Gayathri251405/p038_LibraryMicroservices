package com.example.library.auth.config;

import com.example.library.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Duration;

@Configuration
public class SecurityConfig {
    @Bean
    JwtService jwtService(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.expiration:PT1H}") Duration expiration) {
        return new JwtService(secret, expiration);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain authSecurity(HttpSecurity http, JwtService jwtService) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/users/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .addFilterBefore(new AuthJwtFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
