package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.DevLoginUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Profile("dev")
@Service
@RequiredArgsConstructor
public class DevLoginService implements DevLoginUseCase {

    private static final long DEV_ACCESS_EXPIRATION_MINUTES = 60 * 24 * 7; // 7일

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Override
    public AuthTokenResult login(Command command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. userId=" + command.userId()));

        String accessToken = jwtTokenProvider.generateDevAccessToken(
                user.getId(), Role.USER, DEV_ACCESS_EXPIRATION_MINUTES
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken, Duration.ofDays(refreshExpirationDays));

        return new AuthTokenResult(accessToken, refreshToken, false);
    }
}
