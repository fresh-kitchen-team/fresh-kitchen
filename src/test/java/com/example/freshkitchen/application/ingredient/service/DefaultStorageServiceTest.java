package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.repository.StorageRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultStorageServiceTest {

    @Mock
    private StorageRepository storageRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DefaultStorageService defaultStorageService;

    @Test
    void ensureDefaultStorages_doesNotLockUserWhenAllDefaultTypesExist() {
        Long userId = 1L;
        User user = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));
        List<Storage> storages = List.of(
                Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, "Fridge")),
                Storage.create(new Storage.CreateCommand(user, StorageType.FREEZER, "Freezer")),
                Storage.create(new Storage.CreateCommand(user, StorageType.PANTRY, "Pantry"))
        );
        when(storageRepository.findStorageTypesByUserId(userId)).thenReturn(DefaultStoragePolicy.orderedTypes());
        when(storageRepository.findAllByUserId(userId)).thenReturn(storages);

        List<Storage> result = defaultStorageService.ensureDefaultStorages(userId);

        assertEquals(List.of(StorageType.FRIDGE, StorageType.FREEZER, StorageType.PANTRY),
                result.stream().map(Storage::getStorageType).toList());
        verify(entityManager, never()).find(eq(User.class), eq(userId), eq(LockModeType.PESSIMISTIC_WRITE));
        verify(storageRepository, never()).save(any(Storage.class));
    }

    @Test
    void ensureDefaultStorages_locksUserAndCreatesOnlyMissingDefaultTypes() {
        Long userId = 1L;
        User user = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));
        Storage pantry = Storage.create(new Storage.CreateCommand(user, StorageType.PANTRY, "Custom pantry"));
        when(storageRepository.findStorageTypesByUserId(userId)).thenReturn(List.of(StorageType.PANTRY));
        when(entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(storageRepository.findAllByUserId(userId)).thenReturn(List.of(pantry));
        when(storageRepository.save(any(Storage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Storage> result = defaultStorageService.ensureDefaultStorages(userId);

        assertEquals(List.of(StorageType.FRIDGE, StorageType.FREEZER, StorageType.PANTRY),
                result.stream().map(Storage::getStorageType).toList());
        verify(entityManager).find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        verify(storageRepository, times(2)).save(any(Storage.class));
    }
}
