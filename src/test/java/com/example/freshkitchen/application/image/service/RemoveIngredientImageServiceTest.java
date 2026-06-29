package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.usecase.RemoveIngredientImageUseCase;
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
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(RemoveIngredientImageService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RemoveIngredientImageServiceTest extends PostgreSqlTestContainerSupport {

    private final RemoveIngredientImageUseCase removeIngredientImageUseCase;
    private final IngredientRepository ingredientRepository;

    @PersistenceContext
    private EntityManager entityManager;

    RemoveIngredientImageServiceTest(
            RemoveIngredientImageUseCase removeIngredientImageUseCase,
            IngredientRepository ingredientRepository
    ) {
        this.removeIngredientImageUseCase = removeIngredientImageUseCase;
        this.ingredientRepository = ingredientRepository;
    }

    @Test
    void remove_promotesAnotherImageWhenPrimaryRemoved() {
        User user = persistUser("remove-promote", Provider.GOOGLE);
        Ingredient ingredient = persistIngredient(user, "Tomato");
        IngredientImage primary = persistIngredientImage(ingredient, persistImageAsset(user, "images/1.png"), true);
        IngredientImage secondary = persistIngredientImage(ingredient, persistImageAsset(user, "images/2.png"), false);

        entityManager.flush();
        entityManager.clear();

        removeIngredientImageUseCase.remove(new RemoveIngredientImageUseCase.Command(
                user.getId(),
                ingredient.getId(),
                primary.getId()
        ));

        entityManager.flush();
        entityManager.clear();

        Set<IngredientImage> images = ingredientRepository.findByIdAndUserIdAndStatusWithImagesForUpdate(
                        ingredient.getId(), user.getId(), IngredientStatus.ACTIVE)
                .orElseThrow()
                .getIngredientImages();

        assertEquals(1, images.size());
        IngredientImage remaining = images.iterator().next();
        assertEquals(secondary.getId(), remaining.getId());
        assertTrue(remaining.isPrimary());
    }

    @Test
    void remove_leavesNoImagesWhenLastImageRemoved() {
        User user = persistUser("remove-last", Provider.GOOGLE);
        Ingredient ingredient = persistIngredient(user, "Milk");
        IngredientImage primary = persistIngredientImage(ingredient, persistImageAsset(user, "images/milk.png"), true);

        entityManager.flush();
        entityManager.clear();

        removeIngredientImageUseCase.remove(new RemoveIngredientImageUseCase.Command(
                user.getId(),
                ingredient.getId(),
                primary.getId()
        ));

        entityManager.flush();
        entityManager.clear();

        Set<IngredientImage> images = ingredientRepository.findByIdAndUserIdAndStatusWithImagesForUpdate(
                        ingredient.getId(), user.getId(), IngredientStatus.ACTIVE)
                .orElseThrow()
                .getIngredientImages();

        assertTrue(images.isEmpty());
    }

    @Test
    void remove_keepsPrimaryWhenNonPrimaryRemoved() {
        User user = persistUser("remove-nonprimary", Provider.GOOGLE);
        Ingredient ingredient = persistIngredient(user, "Onion");
        IngredientImage primary = persistIngredientImage(ingredient, persistImageAsset(user, "images/a.png"), true);
        IngredientImage secondary = persistIngredientImage(ingredient, persistImageAsset(user, "images/b.png"), false);

        entityManager.flush();
        entityManager.clear();

        removeIngredientImageUseCase.remove(new RemoveIngredientImageUseCase.Command(
                user.getId(),
                ingredient.getId(),
                secondary.getId()
        ));

        entityManager.flush();
        entityManager.clear();

        Set<IngredientImage> images = ingredientRepository.findByIdAndUserIdAndStatusWithImagesForUpdate(
                        ingredient.getId(), user.getId(), IngredientStatus.ACTIVE)
                .orElseThrow()
                .getIngredientImages();

        assertEquals(1, images.size());
        IngredientImage remaining = images.iterator().next();
        assertEquals(primary.getId(), remaining.getId());
        assertTrue(remaining.isPrimary());
    }

    @Test
    void remove_rejectsImageNotBelongingToIngredient() {
        User user = persistUser("remove-not-belong", Provider.GOOGLE);
        Ingredient ingredient = persistIngredient(user, "Carrot");
        persistIngredientImage(ingredient, persistImageAsset(user, "images/carrot.png"), true);

        entityManager.flush();
        entityManager.clear();

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> removeIngredientImageUseCase.remove(new RemoveIngredientImageUseCase.Command(
                        user.getId(),
                        ingredient.getId(),
                        Long.MAX_VALUE
                ))
        );

        assertEquals(IngredientErrorCode.INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT, exception.getErrorCode());
    }

    @Test
    void remove_rejectsNullIngredientImageId() {
        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> removeIngredientImageUseCase.remove(new RemoveIngredientImageUseCase.Command(1L, 1L, null))
        );

        assertEquals(IngredientErrorCode.INGREDIENT_IMAGE_ID_REQUIRED, exception.getErrorCode());
    }

    @Test
    void remove_rejectsOtherUserIngredient() {
        User owner = persistUser("remove-owner", Provider.GOOGLE);
        User otherUser = persistUser("remove-other", Provider.KAKAO);
        Ingredient ingredient = persistIngredient(owner, "Cheese");
        IngredientImage primary = persistIngredientImage(ingredient, persistImageAsset(owner, "images/cheese.png"), true);

        entityManager.flush();
        entityManager.clear();

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> removeIngredientImageUseCase.remove(new RemoveIngredientImageUseCase.Command(
                        otherUser.getId(),
                        ingredient.getId(),
                        primary.getId()
                ))
        );

        assertEquals(IngredientErrorCode.INGREDIENT_NOT_FOUND, exception.getErrorCode());
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
    }

    private Ingredient persistIngredient(User user, String name) {
        Storage storage = resolveFridgeStorage(user, name);
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

    private Storage resolveFridgeStorage(User user, String name) {
        return entityManager.createQuery("""
                        select storage
                        from Storage storage
                        where storage.user = :user
                          and storage.storageType = :storageType
                        """, Storage.class)
                .setParameter("user", user)
                .setParameter("storageType", StorageType.FRIDGE)
                .getResultList()
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, name + " fridge"));
                    entityManager.persist(storage);
                    return storage;
                });
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
