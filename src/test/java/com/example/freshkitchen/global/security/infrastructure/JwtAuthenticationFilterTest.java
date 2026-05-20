package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.security.JwtAuthentication;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.infrastructure.auth.AccessTokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import org.springframework.data.redis.RedisConnectionFailureException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider, accessTokenBlacklistRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_setsAuthentication_whenValidBearerTokenProvided() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        given(jwtTokenProvider.validateAccessToken("valid-token"))
                .willReturn(new TokenPayload(1L, Role.USER));

        filter.doFilterInternal(request, response, filterChain);

        JwtAuthentication auth = (JwtAuthentication) SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getUserId()).isEqualTo(1L);
        assertThat(auth.getRole()).isEqualTo(Role.USER);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_doesNotSetAuthentication_whenNoAuthorizationHeader() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_doesNotSetAuthentication_whenHeaderIsNotBearer() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic some-credentials");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_setsJwtExceptionAttribute_whenTokenValidationFails() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer expired-token");
        given(jwtTokenProvider.validateAccessToken("expired-token"))
                .willThrow(new JwtTokenException(JwtErrorCode.EXPIRED_TOKEN));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        JwtTokenException stored = (JwtTokenException) request.getAttribute(
                JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE
        );
        assertThat(stored).isNotNull();
        assertThat(stored.getErrorCode()).isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_setsEmptyClaimsException_whenBearerPrefixOnly() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer ");
        given(jwtTokenProvider.validateAccessToken(""))
                .willThrow(new JwtTokenException(JwtErrorCode.EMPTY_CLAIMS));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        JwtTokenException stored = (JwtTokenException) request.getAttribute(
                JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE
        );
        assertThat(stored).isNotNull();
        assertThat(stored.getErrorCode()).isEqualTo(JwtErrorCode.EMPTY_CLAIMS);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_setsBlacklistedTokenException_whenTokenIsBlacklisted() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer blacklisted-token");
        given(jwtTokenProvider.validateAccessToken("blacklisted-token"))
                .willReturn(new TokenPayload(1L, Role.USER));
        given(accessTokenBlacklistRepository.isBlacklisted("blacklisted-token"))
                .willReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        JwtTokenException stored = (JwtTokenException) request.getAttribute(
                JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE
        );
        assertThat(stored).isNotNull();
        assertThat(stored.getErrorCode()).isEqualTo(JwtErrorCode.BLACKLISTED_TOKEN);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_setsAuthentication_whenBlacklistCheckFailsWithRedisError() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token-redis-down");
        given(jwtTokenProvider.validateAccessToken("valid-token-redis-down"))
                .willReturn(new TokenPayload(1L, Role.USER));
        given(accessTokenBlacklistRepository.isBlacklisted("valid-token-redis-down"))
                .willThrow(new RedisConnectionFailureException("Redis connection refused"));

        filter.doFilterInternal(request, response, filterChain);

        // fail-open: Redis 장애 시 인증 허용
        JwtAuthentication auth = (JwtAuthentication) SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getUserId()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }
}
