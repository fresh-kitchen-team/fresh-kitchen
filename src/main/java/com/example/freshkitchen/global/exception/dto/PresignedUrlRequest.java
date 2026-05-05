package com.example.freshkitchen.global.exception.dto;

import com.example.freshkitchen.global.ImageType;

import java.util.List;



public record PresignedUrlRequest(
        ImageType imageType,
        String uploadSessionId,
        List<FileSpec> files
) {
    public record FileSpec(
            String filename,
            String contentType
    ) {}
}
