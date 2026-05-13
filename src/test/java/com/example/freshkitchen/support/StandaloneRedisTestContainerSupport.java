package com.example.freshkitchen.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class StandaloneRedisTestContainerSupport {

    @DynamicPropertySource
    static void configureRedisProperties(DynamicPropertyRegistry registry) {
        RedisContainerHolder.registerProperties(registry);
    }
}
