package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.OAuthErrorCode;
import com.example.freshkitchen.global.security.exception.OAuthException;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier.GoogleUserInfo;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GoogleLoginServiceTest {

    private final GoogleTokenVerifier googleTokenVerifier = mock(GoogleTokenVerifier.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);

    private final GoogleLoginService service = new GoogleLoginService(
            googleTokenVerifier, userRepository, jwtTokenProvider
    );

    @Test
    void login_returnsTokens_whenExistingUser() {
        GoogleUserInfo userInfo = new GoogleUserInfo("google-sub-123", "user@gmail.com");
        given(googleTokenVerifier.verify("valid-id-token")).willReturn(userInfo);

        User existingUser = User.create(new User.CreateCommand("google-sub-123", Provider.GOOGLE));
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "google-sub-123"))
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.generateAccessToken(existingUser.getId(), Role.USER))
                .willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(existingUser.getId()))
                .willReturn("refresh-token");

        AuthTokenResult result = service.login(new GoogleLoginUseCase.Command("valid-id-token"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.newUser()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_createsUserAndReturnsTokens_whenNewUser() {
        GoogleUserInfo userInfo = new GoogleUserInfo("new-google-sub", "new@gmail.com");
        given(googleTokenVerifier.verify("new-user-token")).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "new-google-sub"))
                .willReturn(Optional.empty());

        User savedUser = User.create(new User.CreateCommand("new-google-sub", Provider.GOOGLE));
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtTokenProvider.generateAccessToken(savedUser.getId(), Role.USER))
                .willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(savedUser.getId()))
                .willReturn("new-refresh-token");

        AuthTokenResult result = service.login(new GoogleLoginUseCase.Command("new-user-token"));

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.newUser()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_throwsOAuthException_whenIdTokenIsInvalid() {
        given(googleTokenVerifier.verify("invalid-token"))
                .willThrow(new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN));

        assertThatThrownBy(() -> service.login(new GoogleLoginUseCase.Command("invalid-token")))
                .isInstanceOf(OAuthException.class)
                .satisfies(ex -> assertThat(((OAuthException) ex).getErrorCode())
                        .isEqualTo(OAuthErrorCode.INVALID_ID_TOKEN));
    }
}
