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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoogleLoginService implements GoogleLoginUseCase {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthTokenResult login(Command command) {
        GoogleUserInfo userInfo = googleTokenVerifier.verify(command.idToken());

        boolean[] isNew = {false};
        User user = userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, userInfo.sub())
                .orElseGet(() -> {
                    isNew[0] = true;
                    return userRepository.save(
                            User.create(new User.CreateCommand(userInfo.sub(), Provider.GOOGLE))
                    );
                });

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), Role.USER);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return new AuthTokenResult(accessToken, refreshToken, isNew[0]);
    }
}
