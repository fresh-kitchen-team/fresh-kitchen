package com.example.freshkitchen.domain.image.entity;

import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageAssetEntityTest {

    @Test
    void create_requiresUserForUserUpload() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ImageAsset.create(new ImageAsset.CreateCommand(
                        null,
                        AssetType.USER_UPLOAD,
                        ImageKind.INGREDIENT,
                        StorageProvider.LOCAL,
                        "https://cdn.example/upload.png",
                        120,
                        80
                ))
        );

        assertEquals("user must not be null when assetType is USER_UPLOAD", exception.getMessage());
    }

    @Test
    void create_rejectsUserForSystemDefault() {
        User user = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ImageAsset.create(new ImageAsset.CreateCommand(
                        user,
                        AssetType.SYSTEM_DEFAULT,
                        ImageKind.INGREDIENT,
                        StorageProvider.LOCAL,
                        "https://cdn.example/default.png",
                        120,
                        80
                ))
        );

        assertEquals("user must be null when assetType is SYSTEM_DEFAULT", exception.getMessage());
    }

    @Test
    void create_rejectsNonPositiveDimensions() {
        User user = User.create(new User.CreateCommand("provider-user", Provider.KAKAO));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ImageAsset.create(new ImageAsset.CreateCommand(
                        user,
                        AssetType.USER_UPLOAD,
                        ImageKind.INGREDIENT,
                        StorageProvider.LOCAL,
                        "https://cdn.example/upload.png",
                        0,
                        -1
                ))
        );

        assertEquals("width must be positive", exception.getMessage());
    }

    @Test
    void apply_validatesOwnerConsistencyAndPositiveDimensions() {
        User user = User.create(new User.CreateCommand("provider-user", Provider.GOOGLE));
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                "https://cdn.example/upload.png",
                120,
                80
        ));

        IllegalArgumentException ownerException = assertThrows(IllegalArgumentException.class, () ->
                imageAsset.apply(new ImageAsset.UpdateCommand(
                        null,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        false
                ))
        );
        assertEquals("user must not be null when assetType is USER_UPLOAD", ownerException.getMessage());

        IllegalArgumentException widthException = assertThrows(IllegalArgumentException.class, () ->
                imageAsset.apply(new ImageAsset.UpdateCommand(
                        null,
                        false,
                        null,
                        null,
                        null,
                        null,
                        -1,
                        true,
                        null,
                        false
                ))
        );
        assertEquals("width must be positive", widthException.getMessage());
    }
}
