package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.KakaoLoginUseCase;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import com.example.freshkitchen.infrastructure.oauth.KakaoTokenVerifier;
import com.example.freshkitchen.infrastructure.oauth.KakaoTokenVerifier.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLoginService implements KakaoLoginUseCase {

    private final KakaoTokenVerifier kakaoTokenVerifier;
    private final OAuthUserResolver oAuthUserResolver;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Override
    public AuthTokenResult login(Command command) {
        KakaoUserInfo userInfo = kakaoTokenVerifier.verify(command.idToken());

        OAuthUserResolver.Result result = oAuthUserResolver.resolve(userInfo.sub(), Provider.KAKAO);

        Long userId = result.user().getId();
        String accessToken = jwtTokenProvider.generateAccessToken(userId, Role.USER);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        refreshTokenRepository.save(userId, refreshToken, Duration.ofDays(refreshExpirationDays));

        return new AuthTokenResult(accessToken, refreshToken, result.isNew());
    }
}
