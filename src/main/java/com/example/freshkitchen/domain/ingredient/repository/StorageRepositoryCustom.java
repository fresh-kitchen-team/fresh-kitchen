package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;

import java.util.List;
import java.util.Optional;

public interface StorageRepositoryCustom {

    Optional<Storage> findByIdAndUserId(Long storageId, Long userId);

    Optional<Storage> findByIdAndUserIdWithIngredients(Long storageId, Long userId);

    List<Storage> findAllByUserId(Long userId);

    List<StorageType> findStorageTypesByUserId(Long userId);
}
