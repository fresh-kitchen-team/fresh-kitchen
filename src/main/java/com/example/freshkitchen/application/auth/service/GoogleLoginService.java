package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier.GoogleUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleLoginService implements GoogleLoginUseCase {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Override
    public AuthTokenResult login(Command command) {
        GoogleUserInfo userInfo = googleTokenVerifier.verify(command.idToken());

        Optional<User> optionalUser = userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, userInfo.sub());
        boolean isNew = optionalUser.isEmpty();
        User user;

        if (isNew) {
            try {
                user = userRepository.saveAndFlush(User.create(new User.CreateCommand(userInfo.sub(), Provider.GOOGLE)));
            } catch (DataIntegrityViolationException e) {
                user = userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, userInfo.sub())
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
