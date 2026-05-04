package com.example.freshkitchen.global.config;

import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.global.security.exception.SecurityErrorCode;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.global.security.infrastructure.TokenPayload;
import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, SecurityFilterChainIntegrationTest.TestController.class})
class SecurityFilterChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private GetUserProfileUseCase getUserProfileUseCase;

    @MockitoBean
    private UpdateUserProfileUseCase updateUserProfileUseCase;

    @MockitoBean
    private DeleteUserProfileUseCase deleteUserProfileUseCase;

    @Test
    void protectedEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()))
                .andExpect(jsonPath("$.message").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.message()))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/test/protected"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void protectedEndpoint_returns401_whenTokenIsExpired() throws Exception {
        given(jwtTokenProvider.validateAccessToken("expired-token"))
                .willThrow(new JwtTokenException(JwtErrorCode.EXPIRED_TOKEN));

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(JwtErrorCode.EXPIRED_TOKEN.code()))
                .andExpect(jsonPath("$.message").value(JwtErrorCode.EXPIRED_TOKEN.message()));
    }

    @Test
    void protectedEndpoint_returns401_whenSignatureIsInvalid() throws Exception {
        given(jwtTokenProvider.validateAccessToken("tampered-token"))
                .willThrow(new JwtTokenException(JwtErrorCode.INVALID_SIGNATURE));

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer tampered-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(JwtErrorCode.INVALID_SIGNATURE.code()));
    }

    @Test
    void protectedEndpoint_allowsAccess_whenValidTokenProvided() throws Exception {
        given(jwtTokenProvider.validateAccessToken("valid-token"))
                .willReturn(new TokenPayload(1L, Role.USER));

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerEndpoint_isNotBlockedBySecurityFilter() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    @RestController
    static class TestController {

        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "ok";
        }
    }
}
