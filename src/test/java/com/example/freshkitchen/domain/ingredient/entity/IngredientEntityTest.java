package com.example.freshkitchen.domain.ingredient.entity;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.image.exception.ImageException;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientEntityTest {

    @Test
    void markStatus_changesStatusAndKeepsConsumedDiscardedDatesExclusive() {
        User user = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));
        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, "Main fridge"));
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                "Tomato",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                ExpirySourceType.MANUAL,
                "salad",
                IngredientSourceType.MANUAL
        ));

        ingredient.markConsumed(LocalDate.of(2026, 4, 3));

        assertEquals(IngredientStatus.CONSUMED, ingredient.getStatus());
        assertEquals(LocalDate.of(2026, 4, 3), ingredient.getConsumedAt());
        assertNull(ingredient.getDiscardedAt());

        ingredient.markDiscarded(LocalDate.of(2026, 4, 4));

        assertEquals(IngredientStatus.DISCARDED, ingredient.getStatus());
        assertNull(ingredient.getConsumedAt());
        assertEquals(LocalDate.of(2026, 4, 4), ingredient.getDiscardedAt());
    }

    @Test
    void ingredientImage_primaryOwnershipStaysInsideIngredient() {
        User user = User.create(new User.CreateCommand("provider-user", Provider.KAKAO));
        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.PANTRY, "Dry shelf"));
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                "Pasta",
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.PHOTO
        ));
        ImageAsset firstAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                "https://cdn.example/1.png",
                100,
                100
        ));
        ImageAsset secondAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                "https://cdn.example/2.png",
                200,
                200
        ));

        IngredientImage primary = IngredientImage.create(new IngredientImage.CreateCommand(
                ingredient,
                firstAsset,
                true,
                IngredientImageSourceType.PHOTO
        ));
        IngredientImage secondary = IngredientImage.create(new IngredientImage.CreateCommand(
                ingredient,
                secondAsset,
                false,
                IngredientImageSourceType.DEFAULT
        ));

        secondary.apply(new IngredientImage.UpdateCommand(null, true, null));

        assertEquals(2, ingredient.getIngredientImages().size());
        assertNotNull(primary.getIngredient());
        assertEquals(ingredient, secondary.getIngredient());
        assertEquals(secondAsset, secondary.getImageAsset());
        assertFalse(primary.isPrimary());
        assertTrue(secondary.isPrimary());
    }

    @Test
    void ingredientImage_firstImageMustBePrimary() {
        User user = User.create(new User.CreateCommand("provider-user", Provider.KAKAO));
        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.PANTRY, "Dry shelf"));
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                "Onion",
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.PHOTO
        ));
        ImageAsset asset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                "https://cdn.example/onion.png",
                120,
                120
        ));

        IngredientException exception = assertThrows(IngredientException.class, () ->
                IngredientImage.create(new IngredientImage.CreateCommand(
                        ingredient,
                        asset,
                        false,
                        IngredientImageSourceType.PHOTO
                ))
        );

        assertEquals("first ingredient image must be primary", exception.getMessage());
    }

    @Test
    void ingredientImage_cannotUnsetOnlyPrimary() {
        User user = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));
        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, "Main fridge"));
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                "Milk",
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.PHOTO
        ));
        ImageAsset asset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                "https://cdn.example/milk.png",
                300,
                300
        ));

        IngredientImage image = IngredientImage.create(new IngredientImage.CreateCommand(
                ingredient,
                asset,
                true,
                IngredientImageSourceType.PHOTO
        ));

        IngredientException exception = assertThrows(IngredientException.class, () ->
                image.apply(new IngredientImage.UpdateCommand(null, false, null))
        );

        assertEquals("ingredient must have one primary image", exception.getMessage());
        assertTrue(image.isPrimary());
    }

    @Test
    void ingredientImage_cannotBeReattachedToAnotherIngredient() {
        User user = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));
        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, "Main fridge"));
        Ingredient firstIngredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                "Milk",
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.PHOTO
        ));
        Ingredient secondIngredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                "Cheese",
                null,
                null,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.PHOTO
        ));
        ImageAsset asset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                "https://cdn.example/milk.png",
                300,
                300
        ));

        IngredientImage image = IngredientImage.create(new IngredientImage.CreateCommand(
                firstIngredient,
                asset,
                true,
                IngredientImageSourceType.PHOTO
        ));

        ImageException exception = assertThrows(ImageException.class, () ->
                secondIngredient.addImage(image)
        );

        assertEquals("ingredient image is already attached to another ingredient", exception.getMessage());
        assertEquals(firstIngredient, image.getIngredient());
        assertTrue(firstIngredient.getIngredientImages().contains(image));
        assertTrue(secondIngredient.getIngredientImages().isEmpty());
    }

    @Test
    void create_acceptsStorageOwnedByUserWithSameIdDifferentReference() {
        User storageOwner = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));
        User ingredientOwner = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));
        ReflectionTestUtils.setField(storageOwner, "id", 1L);
        ReflectionTestUtils.setField(ingredientOwner, "id", 1L);

        Storage storage = Storage.create(new Storage.CreateCommand(storageOwner, StorageType.FRIDGE, "Main fridge"));

        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                ingredientOwner,
                storage,
                null,
                "Apple",
                null,
                null,
                ExpirySourceType.MANUAL,
                null,
                IngredientSourceType.MANUAL
        ));

        assertEquals(ingredientOwner, ingredient.getUser());
        assertEquals(storage, ingredient.getStorage());
    }
}
