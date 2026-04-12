package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.StorageSummaryResult;
import com.example.freshkitchen.application.ingredient.usecase.ListStoragesUseCase;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.repository.StorageRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import({DefaultStorageService.class, ListStoragesService.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ListStoragesServiceTest extends PostgreSqlTestContainerSupport {

    private final ListStoragesUseCase listStoragesUseCase;
    private final StorageRepository storageRepository;

    @PersistenceContext
    private EntityManager entityManager;

    ListStoragesServiceTest(
            ListStoragesUseCase listStoragesUseCase,
            StorageRepository storageRepository
    ) {
        this.listStoragesUseCase = listStoragesUseCase;
        this.storageRepository = storageRepository;
    }

    @Test
    void list_bootstrapsThreeDefaultStoragesForNewUser() {
        User user = persistUser("storage-user", Provider.GOOGLE);

        entityManager.flush();
        entityManager.clear();

        List<StorageSummaryResult> storages = listStoragesUseCase.list(new ListStoragesUseCase.Query(user.getId()));

        assertEquals(List.of(StorageType.FRIDGE, StorageType.FREEZER, StorageType.PANTRY),
                storages.stream().map(StorageSummaryResult::storageType).toList());
        assertEquals(List.of("Fridge", "Freezer", "Pantry"),
                storages.stream().map(StorageSummaryResult::name).toList());
        assertEquals(3, storageRepository.findAllByUserId(user.getId()).size());
    }

    @Test
    void list_fillsMissingDefaultStoragesAndReturnsPolicyOrder() {
        User user = persistUser("partial-storage-user", Provider.KAKAO);
        persistStorage(user, StorageType.PANTRY, "Custom pantry");

        entityManager.flush();
        entityManager.clear();

        List<StorageSummaryResult> storages = listStoragesUseCase.list(new ListStoragesUseCase.Query(user.getId()));

        assertEquals(List.of(StorageType.FRIDGE, StorageType.FREEZER, StorageType.PANTRY),
                storages.stream().map(StorageSummaryResult::storageType).toList());
        assertEquals(List.of("Fridge", "Freezer", "Custom pantry"),
                storages.stream().map(StorageSummaryResult::name).toList());
        assertEquals(3, storageRepository.findAllByUserId(user.getId()).size());
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
    }

    private Storage persistStorage(User user, StorageType storageType, String name) {
        Storage storage = Storage.create(new Storage.CreateCommand(user, storageType, name));
        entityManager.persist(storage);
        return storage;
    }
}
