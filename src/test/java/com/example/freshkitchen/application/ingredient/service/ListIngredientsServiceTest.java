package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(ListIngredientsService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ListIngredientsServiceTest extends PostgreSqlTestContainerSupport {

    private final ListIngredientsUseCase listIngredientsUseCase;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private ImageAssetUrlResolver imageAssetUrlResolver;

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
        ImageAsset imageAsset = persistImageAsset(owner, "images/1/ingredient/tomato.jpg");
        persistIngredientImage(firstIngredient, imageAsset, true);
        when(imageAssetUrlResolver.resolve(any(ImageAsset.class)))
                .thenReturn("https://cdn.example.com/images/1/ingredient/tomato.jpg");
        persistIngredient(otherUser, otherStorage, "Onion");

        entityManager.flush();
        entityManager.clear();

        List<IngredientDto.SummaryResponse> results = listIngredientsUseCase.list(new ListIngredientsUseCase.Query(owner.getId()));
        List<Long> ingredientIds = results.stream()
                .map(IngredientDto.SummaryResponse::ingredientId)
                .toList();

        assertEquals(List.of(firstIngredient.getId(), secondIngredient.getId()), ingredientIds);
        assertEquals("https://cdn.example.com/images/1/ingredient/tomato.jpg",
                results.get(0).primaryImage().imageUrl());
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

    private ImageAsset persistImageAsset(User user, String objectKey) {
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                objectKey,
                300,
                300
        ));
        entityManager.persist(imageAsset);
        return imageAsset;
    }

    private IngredientImage persistIngredientImage(Ingredient ingredient, ImageAsset imageAsset, boolean primary) {
        IngredientImage ingredientImage = IngredientImage.create(new IngredientImage.CreateCommand(
                ingredient,
                imageAsset,
                primary,
                IngredientImageSourceType.PHOTO
        ));
        entityManager.persist(ingredientImage);
        return ingredientImage;
    }
}
