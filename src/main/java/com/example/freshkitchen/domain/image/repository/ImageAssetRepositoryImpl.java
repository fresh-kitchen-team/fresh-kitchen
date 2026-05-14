package com.example.freshkitchen.domain.image.repository;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
class ImageAssetRepositoryImpl implements ImageAssetRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<ImageAsset> findByIdAndUserId(Long imageAssetId, Long userId) {
        return entityManager.createQuery("""
                select imageAsset
                from ImageAsset imageAsset
                where imageAsset.id = :imageAssetId
                  and imageAsset.user.id = :userId
                """, ImageAsset.class)
                .setParameter("imageAssetId", imageAssetId)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImageAsset> findAttachableByIdAndUserId(Long imageAssetId, Long userId) {
        return entityManager.createQuery("""
                select imageAsset
                from ImageAsset imageAsset
                left join imageAsset.user user
                where imageAsset.id = :imageAssetId
                  and (
                      (imageAsset.assetType = :userUpload and user.id = :userId)
                      or imageAsset.assetType = :systemDefault
                  )
                """, ImageAsset.class)
                .setParameter("imageAssetId", imageAssetId)
                .setParameter("userId", userId)
                .setParameter("userUpload", AssetType.USER_UPLOAD)
                .setParameter("systemDefault", AssetType.SYSTEM_DEFAULT)
                .getResultList()
                .stream()
                .findFirst();
    }
}
