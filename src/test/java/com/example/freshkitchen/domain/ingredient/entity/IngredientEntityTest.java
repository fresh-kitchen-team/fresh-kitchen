package com.example.freshkitchen.domain.ingredient.entity;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class IngredientEntityTest {

    @Test
    void apply_changesStatusAndKeepsConsumedDiscardedDatesExclusive() {
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

        ingredient.apply(new Ingredient.UpdateCommand(
                null,
                null,
                false,
                null,
                null,
                false,
                null,
                false,
                null,
                IngredientStatus.CONSUMED,
                LocalDate.of(2026, 4, 3),
                false,
                null,
                false,
                null,
                false,
                null
        ));

        assertEquals(IngredientStatus.CONSUMED, ingredient.getStatus());
        assertEquals(LocalDate.of(2026, 4, 3), ingredient.getConsumedAt());
        assertNull(ingredient.getDiscardedAt());

        ingredient.apply(new Ingredient.UpdateCommand(
                null,
                null,
                false,
                null,
                null,
                false,
                null,
                false,
                null,
                IngredientStatus.DISCARDED,
                null,
                false,
                LocalDate.of(2026, 4, 4),
                false,
                null,
                false,
                null
        ));

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
        assertEquals(false, primary.isPrimary());
        assertEquals(true, secondary.isPrimary());
    }
}
