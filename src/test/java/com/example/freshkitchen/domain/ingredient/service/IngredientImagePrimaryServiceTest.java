package com.example.freshkitchen.domain.ingredient.service;

import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
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
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(IngredientImagePrimaryService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class IngredientImagePrimaryServiceTest extends PostgreSqlTestContainerSupport {

    private final IngredientImagePrimaryService ingredientImagePrimaryService;
    private final IngredientRepository ingredientRepository;

    @PersistenceContext
    private EntityManager entityManager;

    IngredientImagePrimaryServiceTest(
            IngredientImagePrimaryService ingredientImagePrimaryService,
            IngredientRepository ingredientRepository
    ) {
        this.ingredientImagePrimaryService = ingredientImagePrimaryService;
        this.ingredientRepository = ingredientRepository;
    }

    @Test
    void changePrimaryImage_replacesPrimaryInsideSingleTransaction() {
        User user = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));
        entityManager.persist(user);

        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, "Main fridge"));
        entityManager.persist(storage);

        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                "Tomato",
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.PHOTO
        ));
        entityManager.persist(ingredient);

        ImageAsset firstAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                "https://cdn.example/1.png",
                100,
                100
        ));
        entityManager.persist(firstAsset);

        ImageAsset secondAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                "https://cdn.example/2.png",
                200,
                200
        ));
        entityManager.persist(secondAsset);

        IngredientImage primary = IngredientImage.create(new IngredientImage.CreateCommand(
                ingredient,
                firstAsset,
                true,
                IngredientImageSourceType.PHOTO
        ));
        entityManager.persist(primary);

        IngredientImage secondary = IngredientImage.create(new IngredientImage.CreateCommand(
                ingredient,
                secondAsset,
                false,
                IngredientImageSourceType.DEFAULT
        ));
        entityManager.persist(secondary);

        entityManager.flush();
        entityManager.clear();

        ingredientImagePrimaryService.changePrimaryImage(ingredient.getId(), secondary.getId());

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
    void changePrimaryImage_rejectsNullIngredientImageId() {
        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> ingredientImagePrimaryService.changePrimaryImage(1L, null)
        );

        assertEquals("ingredientImageId must not be null", exception.getMessage());
    }

    @Test
    void changePrimaryImage_rejectsNullIngredientId() {
        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> ingredientImagePrimaryService.changePrimaryImage(null, 1L)
        );

        assertEquals("ingredientId must not be null", exception.getMessage());
    }
}
