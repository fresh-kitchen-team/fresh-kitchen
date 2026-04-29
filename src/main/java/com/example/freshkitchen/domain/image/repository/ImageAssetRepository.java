package com.example.freshkitchen.domain.image.repository;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long>, ImageAssetRepositoryCustom {
}
