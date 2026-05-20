package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.security.JwtAuthentication;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.infrastructure.auth.AccessTokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String JWT_EXCEPTION_ATTRIBUTE =
            "com.example.freshkitchen.security.jwtException";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                TokenPayload payload = jwtTokenProvider.validateAccessToken(token);
                try {
                    if (accessTokenBlacklistRepository.isBlacklisted(token)) {
                        throw new JwtTokenException(JwtErrorCode.BLACKLISTED_TOKEN);
                    }
                } catch (RedisConnectionFailureException | RedisSystemException e) {
                    log.warn("블랙리스트 조회 실패 (fail-open). Redis 상태를 확인하세요.", e);
                    // 가용성 우선: Redis 장애 시 요청을 차단하지 않음
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new JwtAuthentication(payload.userId(), payload.role())
                );
            } catch (JwtTokenException e) {
                request.setAttribute(JWT_EXCEPTION_ATTRIBUTE, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
