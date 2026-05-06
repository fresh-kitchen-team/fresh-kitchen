package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.usecase.ChangeIngredientPrimaryImageUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(ChangeIngredientPrimaryImageService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ChangeIngredientPrimaryImageServiceTest extends PostgreSqlTestContainerSupport {

    private final ChangeIngredientPrimaryImageUseCase changeIngredientPrimaryImageUseCase;
    private final IngredientRepository ingredientRepository;

    @PersistenceContext
    private EntityManager entityManager;

    ChangeIngredientPrimaryImageServiceTest(
            ChangeIngredientPrimaryImageUseCase changeIngredientPrimaryImageUseCase,
            IngredientRepository ingredientRepository
    ) {
        this.changeIngredientPrimaryImageUseCase = changeIngredientPrimaryImageUseCase;
        this.ingredientRepository = ingredientRepository;
    }

    @Test
    void change_replacesPrimaryInsideSingleTransaction() {
        User user = persistUser("provider-user", Provider.GOOGLE);
        Ingredient ingredient = persistIngredient(user, "Tomato");
        IngredientImage primary = persistIngredientImage(ingredient, persistImageAsset(user, "images/1.png"), true);
        IngredientImage secondary = persistIngredientImage(ingredient, persistImageAsset(user, "images/2.png"), false);

        entityManager.flush();
        entityManager.clear();

        changeIngredientPrimaryImageUseCase.change(new ChangeIngredientPrimaryImageUseCase.Command(
                user.getId(),
                ingredient.getId(),
                secondary.getId()
        ));

        entityManager.flush();
        entityManager.clear();

        Ingredient persistedIngredient = ingredientRepository.findByIdWithImagesForUpdate(ingredient.getId())
                .orElseThrow();

        long primaryCount = persistedIngredient.getIngredientImages().stream()
                .filter(IngredientImage::isPrimary)
                .count();

        IngredientImage persistedPrimary = persistedIngredient.getIngredientImages().stream()
                .filter(IngredientImage::isPrimary)
                .min(Comparator.comparing(IngredientImage::getId))
                .orElseThrow();

        assertEquals(1, primaryCount);
        assertEquals(secondary.getId(), persistedPrimary.getId());
        assertTrue(persistedIngredient.getIngredientImages().stream()
                .anyMatch(ingredientImage -> ingredientImage.getId().equals(primary.getId()) && !ingredientImage.isPrimary()));
    }

    @Test
    void change_rejectsNullIngredientImageId() {
        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> changeIngredientPrimaryImageUseCase.change(new ChangeIngredientPrimaryImageUseCase.Command(
                        1L,
                        1L,
                        null
                ))
        );

        assertEquals(IngredientErrorCode.INGREDIENT_IMAGE_ID_REQUIRED, exception.getErrorCode());
    }

    @Test
    void change_rejectsNullIngredientId() {
        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> changeIngredientPrimaryImageUseCase.change(new ChangeIngredientPrimaryImageUseCase.Command(
                        1L,
                        null,
                        1L
                ))
        );

        assertEquals(IngredientErrorCode.INGREDIENT_ID_REQUIRED, exception.getErrorCode());
    }

    @Test
    void change_rejectsOtherUserIngredient() {
        User owner = persistUser("primary-owner", Provider.GOOGLE);
        User otherUser = persistUser("primary-other", Provider.KAKAO);
        Ingredient ingredient = persistIngredient(owner, "Milk");
        IngredientImage primary = persistIngredientImage(ingredient, persistImageAsset(owner, "images/milk.png"), true);

        entityManager.flush();
        entityManager.clear();

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> changeIngredientPrimaryImageUseCase.change(new ChangeIngredientPrimaryImageUseCase.Command(
                        otherUser.getId(),
                        ingredient.getId(),
                        primary.getId()
                ))
        );

        assertEquals(IngredientErrorCode.INGREDIENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void change_rejectsImageNotBelongingToIngredient() {
        User user = persistUser("not-belong-user", Provider.GOOGLE);
        Ingredient ingredient = persistIngredient(user, "Milk");
        persistIngredientImage(ingredient, persistImageAsset(user, "images/milk.png"), true);

        entityManager.flush();
        entityManager.clear();

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> changeIngredientPrimaryImageUseCase.change(new ChangeIngredientPrimaryImageUseCase.Command(
                        user.getId(),
                        ingredient.getId(),
                        Long.MAX_VALUE
                ))
        );

        assertEquals(IngredientErrorCode.INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT, exception.getErrorCode());
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
    }

    private Ingredient persistIngredient(User user, String name) {
        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, name + " fridge"));
        entityManager.persist(storage);
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                name,
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.PHOTO
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
                100,
                100
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
