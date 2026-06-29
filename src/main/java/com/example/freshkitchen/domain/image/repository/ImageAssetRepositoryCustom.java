package com.example.freshkitchen.domain.image.repository;

import com.example.freshkitchen.domain.image.entity.ImageAsset;

import java.util.Optional;

public interface ImageAssetRepositoryCustom {

    Optional<ImageAsset> findByIdAndUserId(Long imageAssetId, Long userId);

    Optional<ImageAsset> findAttachableByIdAndUserId(Long imageAssetId, Long userId);
}
