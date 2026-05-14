package com.example.freshkitchen.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 연결 설정.
 * <p>Spring Boot auto-configuration 대신 직접 정의하는 이유:
 * {@code application.yml}의 {@code spring.data.redis.password: ${REDIS_PASSWORD:}} 에서
 * 환경변수 미설정 시 빈 문자열("")이 바인딩되는데, auto-config는 빈 문자열도 password로 인식해
 * 비밀번호 없는 Redis 인스턴스에 {@code AUTH ""} 를 시도하여 연결 실패가 발생한다.
 * 이를 방지하기 위해 password가 blank일 때는 설정을 생략한다.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isBlank()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}