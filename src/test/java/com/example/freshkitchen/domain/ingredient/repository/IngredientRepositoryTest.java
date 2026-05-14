package com.example.freshkitchen.domain.ingredient.repository;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class IngredientRepositoryTest extends PostgreSqlTestContainerSupport {

    private final IngredientRepository ingredientRepository;

    @PersistenceContext
    private EntityManager entityManager;

    IngredientRepositoryTest(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @Test
    void findByIdAndUserId_returnsMatchingIngredientOnlyForOwner() {
        User owner = persistUser("owner-user", Provider.GOOGLE);
        User otherUser = persistUser("other-user", Provider.KAKAO);
        Storage storage = persistStorage(owner, StorageType.FRIDGE, "Owner fridge");
        Ingredient ingredient = persistIngredient(owner, storage, "Tomato");

        entityManager.flush();
        entityManager.clear();

        Optional<Ingredient> foundIngredient = ingredientRepository.findByIdAndUserId(ingredient.getId(), owner.getId());
        Optional<Ingredient> notFoundIngredient = ingredientRepository.findByIdAndUserId(ingredient.getId(), otherUser.getId());

        assertTrue(foundIngredient.isPresent());
        assertEquals(ingredient.getId(), foundIngredient.orElseThrow().getId());
        assertTrue(notFoundIngredient.isEmpty());
    }

    @Test
    void findDetailByIdAndUserId_fetchesDetailAssociations() {
        User owner = persistUser("detail-owner", Provider.GOOGLE);
        User otherUser = persistUser("detail-other", Provider.KAKAO);
        Storage storage = persistStorage(owner, StorageType.FRIDGE, "Main fridge");
        IngredientCatalog catalog = persistCatalog("Milk", CatalogCategory.DAIRY, StorageType.FRIDGE);
        Ingredient ingredient = persistIngredient(owner, storage, catalog, "Milk");
        ImageAsset primaryImageAsset = persistImageAsset(owner, "images/milk-primary.png");
        ImageAsset secondaryImageAsset = persistImageAsset(owner, "images/milk-secondary.png");
        persistIngredientImage(ingredient, primaryImageAsset, true);
        persistIngredientImage(ingredient, secondaryImageAsset, false);

        entityManager.flush();
        entityManager.clear();

        Optional<Ingredient> foundIngredient = ingredientRepository.findDetailByIdAndUserId(ingredient.getId(), owner.getId());
        Optional<Ingredient> notFoundIngredient = ingredientRepository.findDetailByIdAndUserId(ingredient.getId(), otherUser.getId());

        Ingredient detail = foundIngredient.orElseThrow();
        assertEquals(ingredient.getId(), detail.getId());
        assertTrue(Hibernate.isInitialized(detail.getUser()));
        assertTrue(Hibernate.isInitialized(detail.getStorage()));
        assertTrue(Hibernate.isInitialized(detail.getCatalog()));
        assertTrue(Hibernate.isInitialized(detail.getIngredientImages()));
        assertEquals(2, detail.getIngredientImages().size());
        assertTrue(notFoundIngredient.isEmpty());
    }

    @Test
    void findByIdWithImagesForUpdate_fetchesIngredientImages() {
        User user = persistUser("provider-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Main fridge");
        Ingredient ingredient = persistIngredient(user, storage, "Milk");
        ImageAsset imageAsset = persistImageAsset(user, "images/milk.png");
        IngredientImage ingredientImage = IngredientImage.create(new IngredientImage.CreateCommand(
                ingredient,
                imageAsset,
                true,
                IngredientImageSourceType.PHOTO
        ));
        entityManager.persist(ingredientImage);

        entityManager.flush();
        entityManager.clear();

        Ingredient foundIngredient = ingredientRepository.findByIdWithImagesForUpdate(ingredient.getId())
                .orElseThrow();

        assertTrue(Hibernate.isInitialized(foundIngredient.getIngredientImages()));
        assertEquals(1, foundIngredient.getIngredientImages().size());
        assertTrue(foundIngredient.getIngredientImages().stream()
                .anyMatch(image -> image.getId().equals(ingredientImage.getId()) && image.isPrimary()));
    }

    @Test
    void findByIdWithImagesForUpdate_returnsEmptyWhenIngredientDoesNotExist() {
        assertFalse(ingredientRepository.findByIdWithImagesForUpdate(Long.MAX_VALUE).isPresent());
    }

    @Test
    void findAllByUserId_returnsOnlyUserIngredients() {
        User owner = persistUser("list-owner", Provider.GOOGLE);
        User otherUser = persistUser("list-other", Provider.KAKAO);
        Storage ownerStorage = persistStorage(owner, StorageType.FRIDGE, "Owner fridge");
        Storage otherStorage = persistStorage(otherUser, StorageType.FRIDGE, "Other fridge");
        Ingredient firstIngredient = persistIngredient(owner, ownerStorage, "Tomato");
        Ingredient secondIngredient = persistIngredient(owner, ownerStorage, "Milk");
        persistIngredientImage(firstIngredient, persistImageAsset(owner, "images/tomato-primary.png"), true);
        persistIngredientImage(firstIngredient, persistImageAsset(owner, "images/tomato-secondary.png"), false);
        persistIngredient(otherUser, otherStorage, "Onion");

        entityManager.flush();
        entityManager.clear();

        List<Ingredient> ingredients = ingredientRepository.findAllByUserId(owner.getId());
        List<Long> ingredientIds = ingredients.stream()
                .map(Ingredient::getId)
                .toList();

        assertEquals(List.of(firstIngredient.getId(), secondIngredient.getId()), ingredientIds);
        assertFalse(Hibernate.isInitialized(ingredients.get(0).getIngredientImages()));
    }

    @Test
    void findAllByUserIdAndStatus_returnsOnlyMatchingUserActiveIngredients() {
        User owner = persistUser("active-owner", Provider.GOOGLE);
        User otherUser = persistUser("active-other", Provider.KAKAO);
        Storage ownerStorage = persistStorage(owner, StorageType.FRIDGE, "Owner fridge");
        Storage otherStorage = persistStorage(otherUser, StorageType.FRIDGE, "Other fridge");
        Ingredient activeIngredient = persistIngredient(owner, ownerStorage, "Tomato");
        Ingredient consumedIngredient = persistIngredient(owner, ownerStorage, "Milk");
        Ingredient discardedIngredient = persistIngredient(owner, ownerStorage, "Onion");
        persistIngredientImage(activeIngredient, persistImageAsset(owner, "images/tomato-primary.png"), true);
        persistIngredientImage(activeIngredient, persistImageAsset(owner, "images/tomato-secondary.png"), false);
        persistIngredient(otherUser, otherStorage, "Other tomato");

        consumedIngredient.markConsumed(null);
        discardedIngredient.markDiscarded(null);

        entityManager.flush();
        entityManager.clear();

        List<Ingredient> ingredients = ingredientRepository
                .findAllByUserIdAndStatus(owner.getId(), IngredientStatus.ACTIVE);
        List<Long> ingredientIds = ingredients.stream()
                .map(Ingredient::getId)
                .toList();

        assertEquals(List.of(activeIngredient.getId()), ingredientIds);
        assertFalse(Hibernate.isInitialized(ingredients.get(0).getIngredientImages()));
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
        return persistIngredient(user, storage, null, name);
    }

    private Ingredient persistIngredient(User user, Storage storage, IngredientCatalog catalog, String name) {
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                catalog,
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

    private IngredientCatalog persistCatalog(String name, CatalogCategory category, StorageType defaultStorageType) {
        IngredientCatalog catalog = IngredientCatalog.create(new IngredientCatalog.CreateCommand(
                null,
                name,
                category,
                defaultStorageType,
                null
        ));
        entityManager.persist(catalog);
        return catalog;
    }
}
