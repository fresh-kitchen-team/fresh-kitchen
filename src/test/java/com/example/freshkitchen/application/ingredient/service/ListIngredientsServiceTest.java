package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(ListIngredientsService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ListIngredientsServiceTest extends PostgreSqlTestContainerSupport {

    private final ListIngredientsUseCase listIngredientsUseCase;

    @PersistenceContext
    private EntityManager entityManager;

    ListIngredientsServiceTest(ListIngredientsUseCase listIngredientsUseCase) {
        this.listIngredientsUseCase = listIngredientsUseCase;
    }

    @Test
    void list_returnsOnlyOwnedIngredients() {
        User owner = persistUser("list-user", Provider.GOOGLE);
        User otherUser = persistUser("other-list-user", Provider.KAKAO);
        Storage ownerStorage = persistStorage(owner, StorageType.FRIDGE, "Fridge");
        Storage otherStorage = persistStorage(otherUser, StorageType.FRIDGE, "Other fridge");
        Ingredient firstIngredient = persistIngredient(owner, ownerStorage, "Tomato");
        Ingredient secondIngredient = persistIngredient(owner, ownerStorage, "Milk");
        persistIngredient(otherUser, otherStorage, "Onion");

        entityManager.flush();
        entityManager.clear();

        List<Long> ingredientIds = listIngredientsUseCase.list(new ListIngredientsUseCase.Query(owner.getId())).stream()
                .map(IngredientDto.SummaryResponse::ingredientId)
                .toList();

        assertEquals(List.of(firstIngredient.getId(), secondIngredient.getId()), ingredientIds);
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
