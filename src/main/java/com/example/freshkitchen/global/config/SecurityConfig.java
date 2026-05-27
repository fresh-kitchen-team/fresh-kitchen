package com.example.freshkitchen.global.config;

import com.example.freshkitchen.global.security.infrastructure.JwtAuthenticationEntryPoint;
import com.example.freshkitchen.global.security.infrastructure.JwtAuthenticationFilter;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.AccessTokenBlacklistRepository;
import com.example.freshkitchen.infrastructure.image.LocalImageStorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            "/add-vector-store",

            "/api/v1/auth/dev-login",
            "/api/v1/tips/storage",
            "/api/v1/tips/recycling",
    };

    private static final String[] PUBLIC_GET_ONLY_ENDPOINTS = {
            "/api/v1/app/version",
            "/api/v1/legal",
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    private final ObjectMapper objectMapper;
    private final LocalImageStorageProperties localImageStorageProperties;

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
        return new JwtAuthenticationFilter(jwtTokenProvider, accessTokenBlacklistRepository);
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    private String[] publicEndpoints() {
        String[] endpoints = Arrays.copyOf(PUBLIC_ENDPOINTS, PUBLIC_ENDPOINTS.length + 1);
        endpoints[PUBLIC_ENDPOINTS.length] = localImageStorageProperties.publicResourcePattern();
        return endpoints;
    }

    private RequestMatcher[] publicEndpointMatchers() {
        List<RequestMatcher> matchers = new ArrayList<>(Arrays.stream(publicEndpoints())
                .map(AntPathRequestMatcher::new)
                .map(RequestMatcher.class::cast)
                .toList());

        Arrays.stream(PUBLIC_GET_ONLY_ENDPOINTS)
                .map(pattern -> new AntPathRequestMatcher(pattern, HttpMethod.GET.name()))
                .forEach(matchers::add);

        return matchers.toArray(RequestMatcher[]::new);
    }
}
