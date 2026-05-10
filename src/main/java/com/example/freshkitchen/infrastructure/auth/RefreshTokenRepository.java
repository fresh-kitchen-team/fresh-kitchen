package com.example.freshkitchen.infrastructure.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    /**
     * 원자적 Compare-And-Swap.
     * <ul>
     *   <li>1 : 교체 성공</li>
     *   <li>0 : key 자체가 없음 (missing)</li>
     *   <li>-1 : 값 불일치 (mismatch, 토큰 탈취 가능성)</li>
     * </ul>
     */
    private static final DefaultRedisScript<Long> CAS_SCRIPT = new DefaultRedisScript<>(
            """
            local cur = redis.call('get', KEYS[1])
            if cur == false then return 0 end
            if cur ~= ARGV[1] then return -1 end
            redis.call('set', KEYS[1], ARGV[2], 'EX', ARGV[3])
            return 1
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, ttl);
    }

    /**
     * 현재 저장된 토큰이 expectedToken과 일치할 때만 newToken으로 원자적 교체.
     *
     * @return 1(성공), 0(key 없음), -1(값 불일치)
     */
    public long compareAndSwap(Long userId, String expectedToken, String newToken, Duration ttl) {
        Long result = redisTemplate.execute(
                CAS_SCRIPT,
                List.of(key(userId)),
                expectedToken,
                newToken,
                String.valueOf(ttl.getSeconds())
        );
        return result != null ? result : 0L;
    }

    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
    }

    public void deleteByUserId(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
