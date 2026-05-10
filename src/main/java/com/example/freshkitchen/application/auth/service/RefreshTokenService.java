package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.RefreshTokenUseCase;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Override
    public AuthTokenResult refresh(Command command) {
        Long userId = jwtTokenProvider.validateRefreshToken(command.refreshToken());

        String stored = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new JwtTokenException(JwtErrorCode.INVALID_REFRESH_TOKEN));

        if (!stored.equals(command.refreshToken())) {
            refreshTokenRepository.deleteByUserId(userId);
            throw new JwtTokenException(JwtErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, Role.USER);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        refreshTokenRepository.save(userId, newRefreshToken, Duration.ofDays(refreshExpirationDays));

        return new AuthTokenResult(newAccessToken, newRefreshToken, false);
    }
}
