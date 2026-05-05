package com.example.freshkitchen.global.exception.dto;

import java.util.Map;

public record PresignedUrlResponse(
        String key,
        String putUrl,
        String method,
        Map<String, String> headers
) {}