package com.example.freshkitchen.presentation.auth;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.KakaoLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.LogoutUseCase;
import com.example.freshkitchen.application.auth.usecase.RefreshTokenUseCase;
import com.example.freshkitchen.global.exception.handler.GlobalExceptionHandler;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
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

class AuthControllerRefreshTest {

    private final GoogleLoginUseCase googleLoginUseCase = mock(GoogleLoginUseCase.class);
    private final KakaoLoginUseCase kakaoLoginUseCase = mock(KakaoLoginUseCase.class);
    private final LogoutUseCase logoutUseCase = mock(LogoutUseCase.class);
    private final RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(googleLoginUseCase, kakaoLoginUseCase, logoutUseCase, refreshTokenUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void refresh_returnsNewTokens_whenRefreshTokenIsValid() throws Exception {
        RefreshTokenUseCase.Command command = new RefreshTokenUseCase.Command("valid-refresh-token");
        given(refreshTokenUseCase.refresh(command))
                .willReturn(new AuthTokenResult("new-access", "new-refresh", false));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "valid-refresh-token" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_returns401_whenRefreshTokenIsInvalid() throws Exception {
        RefreshTokenUseCase.Command command = new RefreshTokenUseCase.Command("expired-token");
        given(refreshTokenUseCase.refresh(command))
                .willThrow(new JwtTokenException(JwtErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "expired-token" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH-401-8"))
                .andExpect(jsonPath("$.message").value("invalid or expired refresh token"));
    }

    @Test
    void refresh_returnsBadRequest_whenRefreshTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"));

        verifyNoInteractions(refreshTokenUseCase);
    }

    @Test
    void refresh_returnsBadRequest_whenRefreshTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"));

        verifyNoInteractions(refreshTokenUseCase);
    }
}
