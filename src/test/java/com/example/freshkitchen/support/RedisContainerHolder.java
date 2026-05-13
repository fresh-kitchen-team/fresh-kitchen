package com.example.freshkitchen.support;

import org.opentest4j.TestAbortedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

/**
 * Redis TestContainer 싱글턴.
 * RedisTestContainerSupport와 StandaloneRedisTestContainerSupport에서 공유.
 */
public final class RedisContainerHolder {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>("redis:7.2-alpine")
                    .withExposedPorts(6379);

    private RedisContainerHolder() {
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        startIfNeeded();
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    private static synchronized void startIfNeeded() {
        if (!REDIS_CONTAINER.isRunning()) {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                throw new TestAbortedException("Docker is not available");
            }
            REDIS_CONTAINER.start();
        }
    }
}
