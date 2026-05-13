package com.example.freshkitchen.global.config;

import com.example.freshkitchen.global.security.infrastructure.JwtAuthenticationEntryPoint;
import com.example.freshkitchen.global.security.infrastructure.JwtAuthenticationFilter;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/actuator/health",
            "/error",
            "/api/v1/auth/google",
            "/api/v1/auth/kakao",
            "/api/v1/auth/refresh",
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Value("${image.storage.local.public-base-url:/uploads}")
    private String localImagePublicBaseUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicEndpointMatchers()).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    private String[] publicEndpoints() {
        String[] endpoints = Arrays.copyOf(PUBLIC_ENDPOINTS, PUBLIC_ENDPOINTS.length + 1);
        endpoints[PUBLIC_ENDPOINTS.length] = localImagePublicBaseUrl.endsWith("/")
                ? localImagePublicBaseUrl + "**"
                : localImagePublicBaseUrl + "/**";
        return endpoints;
    }

    private RequestMatcher[] publicEndpointMatchers() {
        return Arrays.stream(publicEndpoints())
                .map(AntPathRequestMatcher::new)
                .toArray(RequestMatcher[]::new);
    }
}
