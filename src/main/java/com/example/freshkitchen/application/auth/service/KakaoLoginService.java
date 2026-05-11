package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.KakaoLoginUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import com.example.freshkitchen.infrastructure.oauth.KakaoTokenVerifier;
import com.example.freshkitchen.infrastructure.oauth.KakaoTokenVerifier.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLoginService implements KakaoLoginUseCase {

    private final KakaoTokenVerifier kakaoTokenVerifier;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Override
    public AuthTokenResult login(Command command) {
        KakaoUserInfo userInfo = kakaoTokenVerifier.verify(command.idToken());

        Optional<User> optionalUser = userRepository.findByProviderAndProviderUserId(Provider.KAKAO, userInfo.sub());
        boolean isNew = optionalUser.isEmpty();
        User user;

        if (isNew) {
            try {
                user = userRepository.save(User.create(new User.CreateCommand(userInfo.sub(), Provider.KAKAO)));
            } catch (DataIntegrityViolationException e) {
                log.warn("동시 회원가입 충돌 감지, 기존 유저 재조회 로직 실행");
                user = userRepository.findByProviderAndProviderUserId(Provider.KAKAO, userInfo.sub())
                        .orElseThrow(() -> e);
                isNew = false;
            }
        } else {
            user = optionalUser.get();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), Role.USER);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken, Duration.ofDays(refreshExpirationDays));

        return new AuthTokenResult(accessToken, refreshToken, isNew);
    }
}
