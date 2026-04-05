package com.example.freshkitchen.domain;

import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import com.example.freshkitchen.domain.catalog.entity.CatalogExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.CategoryExpiryRule;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.ImageVariant;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.ImageVariantType;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;
import com.example.freshkitchen.domain.user.enums.Provider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class JpaMappingTest extends PostgreSqlTestContainerSupport {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistEntireV1Graph_withExplicitMappings() {
        User user = User.create(new User.CreateCommand("provider-1", Provider.GOOGLE));
        entityManager.persist(user);

        UserProfile profile = UserProfile.create(new UserProfile.CreateCommand(
                user,
                "fresh-user",
                "https://cdn.example/profile.png",
                "hello",
                Set.of("garlic"),
                Set.of(FoodStyle.KOREAN),
                Set.of(AllergyType.EGG),
                Set.of(CookingTool.PAN)
        ));
        entityManager.persist(profile);

        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, "Main fridge"));
        entityManager.persist(storage);

        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                "https://cdn.example/asset.png",
                320,
                240
        ));
        entityManager.persist(imageAsset);

        IngredientCatalog catalog = IngredientCatalog.create(new IngredientCatalog.CreateCommand(
                imageAsset,
                "Tomato",
                CatalogCategory.VEGETABLE,
                StorageType.FRIDGE,
                "https://cdn.example/icon.png"
        ));
        entityManager.persist(catalog);

        CatalogExpiryRule catalogExpiryRule = CatalogExpiryRule.create(new CatalogExpiryRule.CreateCommand(
                catalog,
                StorageType.FRIDGE,
                7,
                "catalog rule"
        ));
        entityManager.persist(catalogExpiryRule);

        CategoryExpiryRule categoryExpiryRule = CategoryExpiryRule.create(new CategoryExpiryRule.CreateCommand(
                CatalogCategory.VEGETABLE,
                StorageType.FRIDGE,
                5,
                "category rule"
        ));
        entityManager.persist(categoryExpiryRule);

        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                catalog,
                "Tomato",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 8),
                ExpirySourceType.POLICY,
                "ripe",
                IngredientSourceType.MANUAL
        ));
        entityManager.persist(ingredient);

        ImageVariant imageVariant = ImageVariant.create(new ImageVariant.CreateCommand(
                imageAsset,
                ImageVariantType.THUMBNAIL,
                "https://cdn.example/thumb.png",
                120,
                90
        ));
        entityManager.persist(imageVariant);

        IngredientImage ingredientImage = IngredientImage.create(new IngredientImage.CreateCommand(
                ingredient,
                imageAsset,
                true,
                IngredientImageSourceType.PHOTO
        ));
        entityManager.persist(ingredientImage);

        entityManager.flush();
        entityManager.clear();

        Ingredient persistedIngredient = entityManager.find(Ingredient.class, ingredient.getId());
        UserProfile persistedProfile = entityManager.find(UserProfile.class, user.getId());
        CategoryExpiryRule persistedCategoryRule = entityManager.find(CategoryExpiryRule.class, categoryExpiryRule.getId());

        assertNotNull(persistedIngredient);
        assertEquals("Tomato", persistedIngredient.getName());
        assertEquals(StorageType.FRIDGE, persistedIngredient.getStorage().getStorageType());
        assertEquals(CatalogCategory.VEGETABLE, persistedIngredient.getCatalog().getCategory());
        assertEquals("fresh-user", persistedProfile.getNickname());
        assertEquals(Set.of("garlic"), persistedProfile.getPreferredIngredients());
        assertEquals(5, persistedCategoryRule.getShelfLifeDays());
    }
}
