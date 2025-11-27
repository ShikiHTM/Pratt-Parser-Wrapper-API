package org.shiki.prattparserrestfulapi.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF for simple stateless REST API interactions
                .csrf(csrf -> csrf.disable())

                // 2. Configure path authorization
                .authorizeHttpRequests(auth -> auth
                        // Allow ALL requests (GET and POST) to the expression/evaluate endpoint
                        .requestMatchers("/api/v1/expression/evaluate/**").permitAll()

                        // You should also allow the Actuator health checks
                        .requestMatchers("/actuator/**").permitAll()

                        // Require authentication for all other requests
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}