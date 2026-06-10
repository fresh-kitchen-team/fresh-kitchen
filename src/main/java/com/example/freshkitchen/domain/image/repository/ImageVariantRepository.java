package com.example.freshkitchen.domain.image.repository;

import com.example.freshkitchen.domain.image.entity.ImageVariant;
import com.example.freshkitchen.domain.image.enums.ImageVariantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImageVariantRepository extends JpaRepository<ImageVariant, Long> {

    Optional<ImageVariant> findByImageAssetIdAndVariantType(Long imageAssetId, ImageVariantType variantType);

    List<ImageVariant> findAllByImageAssetIdOrderByIdAsc(Long imageAssetId);

    boolean existsByImageAssetIdAndVariantType(Long imageAssetId, ImageVariantType variantType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ImageVariant v where v.imageAsset.id = :imageAssetId")
    int deleteByImageAssetId(@Param("imageAssetId") Long imageAssetId);
}
