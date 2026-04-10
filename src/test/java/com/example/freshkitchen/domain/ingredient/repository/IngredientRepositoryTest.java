package com.example.freshkitchen.domain.ingredient.repository;

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
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
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
    void findByIdWithImagesForUpdate_fetchesIngredientImages() {
        User user = persistUser("provider-user", Provider.GOOGLE);
        Storage storage = persistStorage(user, StorageType.FRIDGE, "Main fridge");
        Ingredient ingredient = persistIngredient(user, storage, "Milk");
        ImageAsset imageAsset = persistImageAsset(user, "https://cdn.example/milk.png");
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
        persistIngredient(otherUser, otherStorage, "Onion");

        entityManager.flush();
        entityManager.clear();

        List<Long> ingredientIds = ingredientRepository.findAllByUserId(owner.getId()).stream()
                .map(Ingredient::getId)
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

    private ImageAsset persistImageAsset(User user, String imageUrl) {
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                imageUrl,
                300,
                300
        ));
        entityManager.persist(imageAsset);
        return imageAsset;
    }
}
