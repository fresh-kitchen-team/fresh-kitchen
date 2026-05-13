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

        // newRefreshToken은 CAS의 인자이므로 먼저 생성; newAccessToken은 CAS 성공 후에만 생성
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        long casResult = refreshTokenRepository.compareAndSwap(
                userId,
                command.refreshToken(),
                newRefreshToken,
                Duration.ofDays(refreshExpirationDays)
        );

        if (casResult == -1) {
            // 값 불일치 → 토큰 탈취 가능성, 전체 세션 무효화
            refreshTokenRepository.deleteByUserId(userId);
            throw new JwtTokenException(JwtErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (casResult == 0) {
            // key 없음 → 만료 또는 이미 로그아웃된 세션 (동시 요청 포함)
            throw new JwtTokenException(JwtErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, Role.USER);
        return new AuthTokenResult(newAccessToken, newRefreshToken, false);
    }
}
