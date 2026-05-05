package com.example.freshkitchen.global.exception.dto;

public record ImageDto(
        Long imageId,
        Long relatedId,
        String imageUrl
) {}