package com.example.freshkitchen.domain.image.repository;

import com.example.freshkitchen.domain.image.entity.IngredientImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientImageRepository extends JpaRepository<IngredientImage, Long> {

    List<IngredientImage> findAllByIngredientIdOrderByIdAsc(Long ingredientId);

    Optional<IngredientImage> findByIngredientIdAndPrimaryTrue(Long ingredientId);

    Optional<IngredientImage> findByIngredientIdAndImageAssetId(Long ingredientId, Long imageAssetId);

    boolean existsByIngredientIdAndImageAssetId(Long ingredientId, Long imageAssetId);

    boolean existsByImageAssetId(Long imageAssetId);
}
