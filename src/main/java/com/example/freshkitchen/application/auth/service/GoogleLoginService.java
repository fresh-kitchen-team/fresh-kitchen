package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier.GoogleUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleLoginService implements GoogleLoginUseCase {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthTokenResult login(Command command) {
        GoogleUserInfo userInfo = googleTokenVerifier.verify(command.idToken());

        Optional<User> optionalUser = userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, userInfo.sub());
        boolean isNew = optionalUser.isEmpty();
        User user;

        if (isNew) {
            try {
                user = userRepository.save(User.create(new User.CreateCommand(userInfo.sub(), Provider.GOOGLE)));
            } catch (DataIntegrityViolationException e) {
                String maskedSub = userInfo.sub() != null && userInfo.sub().length() > 4 
                        ? userInfo.sub().substring(0, 4) + "****" : "****";
                log.warn("동시 회원가입 충돌 감지, 기존 유저 재조회. sub={}", maskedSub);
                user = userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, userInfo.sub())
                        .orElseThrow(() -> e);
                isNew = false;
            }
        } else {
            user = optionalUser.get();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), Role.USER);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return new AuthTokenResult(accessToken, refreshToken, isNew);
    }
}
