package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(GetIngredientService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class GetIngredientServiceTest extends PostgreSqlTestContainerSupport {

    private final GetIngredientUseCase getIngredientUseCase;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private ImageAssetUrlResolver imageAssetUrlResolver;

    GetIngredientServiceTest(GetIngredientUseCase getIngredientUseCase) {
        this.getIngredientUseCase = getIngredientUseCase;
    }

    @Test
    void get_returnsOwnedIngredientDetail() {
        User user = persistUser("detail-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Main fridge");
        Ingredient ingredient = persistIngredient(user, storage, "Milk");
        ImageAsset imageAsset = persistImageAsset(user, "images/1/ingredient/milk.jpg");
        persistIngredientImage(ingredient, imageAsset, true);
        when(imageAssetUrlResolver.resolve(any(ImageAsset.class)))
                .thenReturn("https://cdn.example.com/images/1/ingredient/milk.jpg");

        entityManager.flush();
        entityManager.clear();

        IngredientDto.DetailResponse result = getIngredientUseCase.get(new GetIngredientUseCase.Query(
                ingredient.getId(),
                user.getId()
        ));

        assertEquals(ingredient.getId(), result.ingredientId());
        assertEquals("Milk", result.name());
        assertEquals(storage.getId(), result.storageId());
        assertEquals("Main fridge", result.storageName());
        assertEquals("https://cdn.example.com/images/1/ingredient/milk.jpg", result.primaryImage().imageUrl());
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
