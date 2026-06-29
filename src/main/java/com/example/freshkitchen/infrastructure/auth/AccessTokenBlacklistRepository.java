package com.example.freshkitchen.infrastructure.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class AccessTokenBlacklistRepository {

    private static final String KEY_PREFIX = "blacklist:";
    private static final String PLACEHOLDER_VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    /**
     * 로그아웃된 Access Token을 블랙리스트에 등록합니다.
     * TTL은 토큰의 남은 유효시간으로 설정하여 메모리 누수를 방지합니다.
     *
     * @param token        원문 Access Token
     * @param remainingTtl 토큰의 남은 유효시간
     */
    public void add(String token, Duration remainingTtl) {
        if (token == null || token.isBlank() || remainingTtl == null) {
            return;
        }
        if (remainingTtl.isNegative() || remainingTtl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(key(token), PLACEHOLDER_VALUE, remainingTtl);
    }

    /**
     * 해당 Access Token이 블랙리스트에 등록되어 있는지 확인합니다.
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
    }

    private String key(String token) {
        return KEY_PREFIX + TokenHashUtil.hash(token);
    }
}
