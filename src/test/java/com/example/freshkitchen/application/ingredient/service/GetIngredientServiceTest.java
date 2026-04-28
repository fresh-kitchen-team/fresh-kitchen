package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.response.IngredientDetailResponse;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(GetIngredientService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class GetIngredientServiceTest extends PostgreSqlTestContainerSupport {

    private final GetIngredientUseCase getIngredientUseCase;

    @PersistenceContext
    private EntityManager entityManager;

    GetIngredientServiceTest(GetIngredientUseCase getIngredientUseCase) {
        this.getIngredientUseCase = getIngredientUseCase;
    }

    @Test
    void get_returnsOwnedIngredientDetail() {
        User user = persistUser("detail-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Main fridge");
        Ingredient ingredient = persistIngredient(user, storage, "Milk");

        entityManager.flush();
        entityManager.clear();

        IngredientDetailResponse result = getIngredientUseCase.get(new GetIngredientUseCase.Query(
                ingredient.getId(),
                user.getId()
        ));

        assertEquals(ingredient.getId(), result.ingredientId());
        assertEquals("Milk", result.name());
        assertEquals(storage.getId(), result.storageId());
        assertEquals("Main fridge", result.storageName());
    }

    @Test
    void get_rejectsIngredientOfAnotherUser() {
        User owner = persistUser("owner-user", Provider.GOOGLE);
        User otherUser = persistUser("other-user", Provider.KAKAO);
        Storage storage = persistStorage(owner, StorageType.FRIDGE, "Fridge");
        Ingredient ingredient = persistIngredient(owner, storage, "Butter");

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> getIngredientUseCase.get(new GetIngredientUseCase.Query(ingredient.getId(), otherUser.getId()))
        );

        assertEquals("ingredient not found", exception.getMessage());
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

    private Ingredient persistIngredient(User user, Storage storage, String name) {
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                name,
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.MANUAL
        ));
        entityManager.persist(ingredient);
        return ingredient;
    }
}
