package com.example.freshkitchen.support;

import org.opentest4j.TestAbortedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

public abstract class RedisTestContainerSupport extends PostgreSqlTestContainerSupport {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>("redis:7.2-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedisProperties(DynamicPropertyRegistry registry) {
        startRedisIfNeeded();
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    private static synchronized void startRedisIfNeeded() {
        if (!REDIS_CONTAINER.isRunning()) {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                throw new TestAbortedException("Docker is not available");
            }
            REDIS_CONTAINER.start();
        }
    }
}
