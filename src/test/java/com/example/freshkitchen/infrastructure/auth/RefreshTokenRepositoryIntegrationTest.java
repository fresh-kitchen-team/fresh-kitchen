package com.example.freshkitchen.infrastructure.auth;

import com.example.freshkitchen.global.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {RedisConfig.class, RefreshTokenRepository.class})
class RefreshTokenRepositoryIntegrationTest {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        startRedisIfNeeded();
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    private static synchronized void startRedisIfNeeded() {
        if (!REDIS.isRunning()) {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                throw new TestAbortedException("Docker is not available");
            }
            REDIS.start();
        }
    }

    @Autowired
    private RefreshTokenRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanUp() {
        redisTemplate.delete(redisTemplate.keys("refresh:*"));
    }

    @Test
    void save_andFindByUserId() {
        repository.save(1L, "token-abc", Duration.ofMinutes(1));

        Optional<String> found = repository.findByUserId(1L);

        assertThat(found).hasValue("token-abc");
    }

    @Test
    void save_setsTtl() {
        repository.save(1L, "token-abc", Duration.ofSeconds(30));

        Long ttl = redisTemplate.getExpire("refresh:1");

        assertThat(ttl).isNotNull().isGreaterThan(0).isLessThanOrEqualTo(30);
    }

    @Test
    void deleteByUserId_removesKey() {
        repository.save(1L, "token-abc", Duration.ofMinutes(1));

        repository.deleteByUserId(1L);

        assertThat(repository.findByUserId(1L)).isEmpty();
    }

    @Test
    void compareAndSwap_returnsOne_whenTokenMatches() {
        repository.save(1L, "old-token", Duration.ofMinutes(1));

        long result = repository.compareAndSwap(1L, "old-token", "new-token", Duration.ofMinutes(1));

        assertThat(result).isEqualTo(1L);
        assertThat(repository.findByUserId(1L)).hasValue("new-token");
    }

    @Test
    void compareAndSwap_setsTtlOnNewToken() {
        repository.save(1L, "old-token", Duration.ofMinutes(5));

        repository.compareAndSwap(1L, "old-token", "new-token", Duration.ofSeconds(60));

        Long ttl = redisTemplate.getExpire("refresh:1");
        assertThat(ttl).isNotNull().isGreaterThan(0).isLessThanOrEqualTo(60);
    }

    @Test
    void compareAndSwap_returnsNegativeOne_whenTokenMismatch() {
        repository.save(1L, "real-token", Duration.ofMinutes(1));

        long result = repository.compareAndSwap(1L, "wrong-token", "new-token", Duration.ofMinutes(1));

        assertThat(result).isEqualTo(-1L);
        assertThat(repository.findByUserId(1L)).hasValue("real-token");
    }

    @Test
    void compareAndSwap_returnsZero_whenKeyMissing() {
        long result = repository.compareAndSwap(99L, "any-token", "new-token", Duration.ofMinutes(1));

        assertThat(result).isEqualTo(0L);
        assertThat(repository.findByUserId(99L)).isEmpty();
    }

    @Test
    void compareAndSwap_secondCallFails_withSameOldToken() {
        repository.save(1L, "old-token", Duration.ofMinutes(1));

        long first = repository.compareAndSwap(1L, "old-token", "new-token-1", Duration.ofMinutes(1));
        long second = repository.compareAndSwap(1L, "old-token", "new-token-2", Duration.ofMinutes(1));

        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(-1L);
        assertThat(repository.findByUserId(1L)).hasValue("new-token-1");
    }
}
