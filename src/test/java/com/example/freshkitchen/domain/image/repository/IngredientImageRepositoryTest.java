package com.example.freshkitchen.domain.image.repository;

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
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class IngredientImageRepositoryTest extends PostgreSqlTestContainerSupport {

    private final IngredientImageRepository ingredientImageRepository;

    @PersistenceContext
    private EntityManager entityManager;

    IngredientImageRepositoryTest(IngredientImageRepository ingredientImageRepository) {
        this.ingredientImageRepository = ingredientImageRepository;
    }

    @Test
    void findAllByIngredientIdOrderByIdAsc_returnsOnlyIngredientImagesInCreationOrder() {
        User user = persistUser("ingredient-image-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Main fridge");
        Ingredient targetIngredient = persistIngredient(user, storage, "Milk");
        Ingredient otherIngredient = persistIngredient(user, storage, "Tomato");
        ImageAsset primaryAsset = persistImageAsset(user, "images/milk-primary.png");
        ImageAsset subAsset = persistImageAsset(user, "images/milk-sub.png");
        ImageAsset otherAsset = persistImageAsset(user, "images/tomato.png");
        IngredientImage primaryImage = persistIngredientImage(targetIngredient, primaryAsset, true);
        IngredientImage subImage = persistIngredientImage(targetIngredient, subAsset, false);
        persistIngredientImage(otherIngredient, otherAsset, true);

        entityManager.flush();
        entityManager.clear();

        List<Long> ingredientImageIds = ingredientImageRepository.findAllByIngredientIdOrderByIdAsc(targetIngredient.getId())
                .stream()
                .map(IngredientImage::getId)
                .toList();

        assertEquals(List.of(primaryImage.getId(), subImage.getId()), ingredientImageIds);
    }

    @Test
    void findByIngredientIdAndPrimaryTrue_returnsPrimaryImage() {
        User user = persistUser("primary-image-user", Provider.KAKAO);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Main fridge");
        Ingredient ingredient = persistIngredient(user, storage, "Milk");
        ImageAsset primaryAsset = persistImageAsset(user, "images/primary.png");
        ImageAsset subAsset = persistImageAsset(user, "images/sub.png");
        IngredientImage primaryImage = persistIngredientImage(ingredient, primaryAsset, true);
        persistIngredientImage(ingredient, subAsset, false);

        entityManager.flush();
        entityManager.clear();

        IngredientImage foundImage = ingredientImageRepository.findByIngredientIdAndPrimaryTrue(ingredient.getId())
                .orElseThrow();

        assertEquals(primaryImage.getId(), foundImage.getId());
        assertTrue(foundImage.isPrimary());
    }

    @Test
    void findByIngredientIdAndImageAssetId_returnsMatchingConnection() {
        User user = persistUser("connection-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Main fridge");
        Ingredient ingredient = persistIngredient(user, storage, "Milk");
        ImageAsset primaryAsset = persistImageAsset(user, "images/primary.png");
        ImageAsset subAsset = persistImageAsset(user, "images/sub.png");
        persistIngredientImage(ingredient, primaryAsset, true);
        IngredientImage subImage = persistIngredientImage(ingredient, subAsset, false);

        entityManager.flush();
        entityManager.clear();

        IngredientImage foundImage = ingredientImageRepository.findByIngredientIdAndImageAssetId(
                ingredient.getId(),
                subAsset.getId()
        ).orElseThrow();

        assertEquals(subImage.getId(), foundImage.getId());
        assertTrue(ingredientImageRepository.existsByIngredientIdAndImageAssetId(
                ingredient.getId(),
                subAsset.getId()
        ));
        assertFalse(ingredientImageRepository.existsByIngredientIdAndImageAssetId(
                ingredient.getId(),
                Long.MAX_VALUE
        ));
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
