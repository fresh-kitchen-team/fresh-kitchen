package com.example.freshkitchen.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class RedisTestContainerSupport extends PostgreSqlTestContainerSupport {

    @DynamicPropertySource
    static void configureRedisProperties(DynamicPropertyRegistry registry) {
        RedisContainerHolder.registerProperties(registry);
    }
}
