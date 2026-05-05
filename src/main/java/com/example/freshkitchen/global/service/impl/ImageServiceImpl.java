package com.example.freshkitchen.global.service.impl;

import com.example.freshkitchen.global.exception.dto.ImageDto;
import com.example.freshkitchen.global.exception.dto.PresignedUrlRequest;
import com.example.freshkitchen.global.exception.dto.PresignedUrlResponse;
import com.example.freshkitchen.global.service.ImageService;

import java.util.List;

public class ImageServiceImpl implements ImageService {
    @Override
    public List<PresignedUrlResponse> generatePresignedUrls(PresignedUrlRequest request) {
        return List.of();
    }

    @Override
    public void deleteObject(String keyOrUrl) {

    }

    @Override
    public void deleteFolder(String fileLocation) {

    }

    @Override
    public String upsertUserProfileImage(Long userId, String requestedKeyOrUrl) {
        return "";
    }

    @Override
    public void deleteUserProfileImage(Long userId) {

    }

    @Override
    public String getUserProfileKey(Long userId) {
        return "";
    }

    @Override
    public String normalizeKey(String keyOrUrl) {
        return "";
    }

    @Override
    public String toPublicUrl(String keyOrNull) {
        return "";
    }

    @Override
    public List<ImageDto> findImagesForChatRooms(List<Long> roomIds) {
        return List.of();
    }
}
