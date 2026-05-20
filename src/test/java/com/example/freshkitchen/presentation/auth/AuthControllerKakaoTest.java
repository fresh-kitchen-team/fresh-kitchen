package com.example.freshkitchen.presentation.auth;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.KakaoLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.LogoutUseCase;
import com.example.freshkitchen.application.auth.usecase.RefreshTokenUseCase;
import com.example.freshkitchen.global.exception.handler.GlobalExceptionHandler;
import com.example.freshkitchen.global.security.exception.OAuthErrorCode;
import com.example.freshkitchen.global.security.exception.OAuthException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerKakaoTest {

    private final GoogleLoginUseCase googleLoginUseCase = mock(GoogleLoginUseCase.class);
    private final KakaoLoginUseCase kakaoLoginUseCase = mock(KakaoLoginUseCase.class);
    private final LogoutUseCase logoutUseCase = mock(LogoutUseCase.class);
    private final RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(googleLoginUseCase, kakaoLoginUseCase, logoutUseCase, refreshTokenUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void kakaoLogin_returnsTokens_whenIdTokenIsValid() throws Exception {
        KakaoLoginUseCase.Command command = new KakaoLoginUseCase.Command("valid-id-token");
        given(kakaoLoginUseCase.login(command))
                .willReturn(new AuthTokenResult("access-token", "refresh-token", false));

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "idToken": "valid-id-token" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.newUser").value(false));
    }

    @Test
    void kakaoLogin_returnsTokensWithNewUserTrue_whenFirstLogin() throws Exception {
        KakaoLoginUseCase.Command command = new KakaoLoginUseCase.Command("new-user-token");
        given(kakaoLoginUseCase.login(command))
                .willReturn(new AuthTokenResult("access-token", "refresh-token", true));

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "idToken": "new-user-token" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newUser").value(true));
    }

    @Test
    void kakaoLogin_returns401_whenIdTokenIsInvalid() throws Exception {
        KakaoLoginUseCase.Command command = new KakaoLoginUseCase.Command("invalid-token");
        given(kakaoLoginUseCase.login(command))
                .willThrow(new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN));

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "idToken": "invalid-token" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH-401-7"))
                .andExpect(jsonPath("$.message").value("invalid id token"));
    }

    @Test
    void kakaoLogin_returnsBadRequest_whenIdTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "idToken": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/kakao"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(kakaoLoginUseCase, googleLoginUseCase, refreshTokenUseCase);
    }

    @Test
    void kakaoLogin_returnsBadRequest_whenIdTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/kakao"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(kakaoLoginUseCase, googleLoginUseCase, refreshTokenUseCase);
    }

    @Test
    void kakaoLogin_returnsBadRequest_whenIdTokenIsTooLong() throws Exception {
        String longToken = "a".repeat(4097);
        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "idToken": "%s" }
                                """.formatted(longToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(kakaoLoginUseCase, googleLoginUseCase, refreshTokenUseCase);
    }
}
