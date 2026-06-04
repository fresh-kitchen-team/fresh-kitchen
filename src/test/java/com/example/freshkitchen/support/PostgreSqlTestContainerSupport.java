package com.example.freshkitchen.support;

import org.opentest4j.TestAbortedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/*
 * Container lifecycle is managed manually instead of @Container.
 * This avoids class-level container stop while Spring context cache may still hold DataSource.
 */

public abstract class PostgreSqlTestContainerSupport {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("freshkitchen_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        startContainerIfNeeded();
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRESQL_CONTAINER::getDriverClassName);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 2);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 0);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("jwt.secret", () -> "test-secret-key-must-be-at-least-32-bytes-long-for-hmac-sha256");
        registry.add("jwt.access-expiration-minutes", () -> 30L);
        registry.add("jwt.refresh-expiration-days", () -> 14L);
    }

    private static synchronized void startContainerIfNeeded() {
        if (!POSTGRESQL_CONTAINER.isRunning()) {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                throw new TestAbortedException("Docker is not available");
            }
            POSTGRESQL_CONTAINER.start();
        }
    }
}
