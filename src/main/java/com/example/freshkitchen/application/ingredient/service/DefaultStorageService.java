package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.repository.StorageRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.global.exception.BusinessException;
import com.example.freshkitchen.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class DefaultStorageService {

    private final StorageRepository storageRepository;
    private final EntityManager entityManager;

    public List<Storage> ensureDefaultStorages(Long userId) {
        Set<StorageType> missingTypes = resolveMissingDefaultTypes(storageRepository.findStorageTypesByUserId(userId));
        if (missingTypes.isEmpty()) {
            return findSortedStorages(userId);
        }

        User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        List<Storage> storages = new ArrayList<>(storageRepository.findAllByUserId(userId));
        missingTypes = resolveMissingDefaultTypes(storages.stream()
                .map(Storage::getStorageType)
                .toList());

        for (StorageType storageType : DefaultStoragePolicy.orderedTypes()) {
            if (!missingTypes.contains(storageType)) {
                continue;
            }
            Storage createdStorage = storageRepository.save(Storage.create(new Storage.CreateCommand(
                    user,
                    storageType,
                    DefaultStoragePolicy.resolveName(storageType)
            )));
            storages.add(createdStorage);
        }

        storages.sort(DefaultStoragePolicy.comparator());
        return storages;
    }

    private List<Storage> findSortedStorages(Long userId) {
        List<Storage> storages = new ArrayList<>(storageRepository.findAllByUserId(userId));
        storages.sort(DefaultStoragePolicy.comparator());
        return storages;
    }

    private Set<StorageType> resolveMissingDefaultTypes(List<StorageType> existingTypes) {
        Set<StorageType> missingTypes = EnumSet.copyOf(DefaultStoragePolicy.orderedTypes());
        missingTypes.removeAll(existingTypes);
        return missingTypes;
    }
}
