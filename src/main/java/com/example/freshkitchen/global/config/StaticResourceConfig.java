package com.example.freshkitchen.global.config;

import com.example.freshkitchen.infrastructure.image.LocalImageStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(LocalImageStorageProperties.class)
@ConditionalOnProperty(name = "image.storage.type", havingValue = "local", matchIfMissing = true)
public class StaticResourceConfig implements WebMvcConfigurer {

    private final LocalImageStorageProperties localImageStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseUrl = localImageStorageProperties.getPublicBaseUrl();
        String pattern = baseUrl.endsWith("/") ? baseUrl + "**" : baseUrl + "/**";
        String location = Path.of(localImageStorageProperties.getRootDir())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}
