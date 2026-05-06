package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.usecase.AttachIngredientImageUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.image.exception.ImageErrorCode;
import com.example.freshkitchen.domain.image.exception.ImageException;
import com.example.freshkitchen.domain.image.repository.IngredientImageRepository;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(AttachIngredientImageService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AttachIngredientImageServiceTest extends PostgreSqlTestContainerSupport {

    private final AttachIngredientImageUseCase attachIngredientImageUseCase;
    private final IngredientImageRepository ingredientImageRepository;

    @PersistenceContext
    private EntityManager entityManager;

    AttachIngredientImageServiceTest(
            AttachIngredientImageUseCase attachIngredientImageUseCase,
            IngredientImageRepository ingredientImageRepository
    ) {
        this.attachIngredientImageUseCase = attachIngredientImageUseCase;
        this.ingredientImageRepository = ingredientImageRepository;
    }

    @Test
    void attach_allowsOwnerUploadImage() {
        User user = persistUser("attach-owner", Provider.GOOGLE);
        Ingredient ingredient = persistIngredient(user, "Tomato");
        ImageAsset imageAsset = persistImageAsset(user, AssetType.USER_UPLOAD, "images/tomato.png");

        Long ingredientImageId = attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                user.getId(),
                ingredient.getId(),
                imageAsset.getId(),
                true,
                IngredientImageSourceType.PHOTO
        ));

        entityManager.flush();
        entityManager.clear();

        IngredientImage ingredientImage = ingredientImageRepository.findById(ingredientImageId)
                .orElseThrow();

        assertEquals(ingredient.getId(), ingredientImage.getIngredient().getId());
        assertEquals(imageAsset.getId(), ingredientImage.getImageAsset().getId());
        assertTrue(ingredientImage.isPrimary());
    }

    @Test
    void attach_allowsSystemDefaultImage() {
        User user = persistUser("attach-system-user", Provider.KAKAO);
        Ingredient ingredient = persistIngredient(user, "Milk");
        ImageAsset imageAsset = persistImageAsset(null, AssetType.SYSTEM_DEFAULT, "images/default-milk.png");

        Long ingredientImageId = attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                user.getId(),
                ingredient.getId(),
                imageAsset.getId(),
                true,
                IngredientImageSourceType.DEFAULT
        ));

        entityManager.flush();
        entityManager.clear();

        assertEquals(imageAsset.getId(), ingredientImageRepository.findById(ingredientImageId)
                .orElseThrow()
                .getImageAsset()
                .getId());
    }

    @Test
    void attach_rejectsOtherUserUploadImage() {
        User owner = persistUser("attach-owner-only", Provider.GOOGLE);
        User otherUser = persistUser("attach-other", Provider.KAKAO);
        Ingredient ingredient = persistIngredient(owner, "Onion");
        ImageAsset otherImageAsset = persistImageAsset(otherUser, AssetType.USER_UPLOAD, "images/other.png");

        ImageException exception = assertThrows(
                ImageException.class,
                () -> attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                        owner.getId(),
                        ingredient.getId(),
                        otherImageAsset.getId(),
                        true,
                        IngredientImageSourceType.PHOTO
                ))
        );

        assertEquals(ImageErrorCode.IMAGE_ASSET_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void attach_rejectsFirstImageWhenNotPrimary() {
        User user = persistUser("first-image-user", Provider.GOOGLE);
        Ingredient ingredient = persistIngredient(user, "Pepper");
        ImageAsset imageAsset = persistImageAsset(user, AssetType.USER_UPLOAD, "images/pepper.png");

        IngredientException exception = assertThrows(
                IngredientException.class,
                () -> attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                        user.getId(),
                        ingredient.getId(),
                        imageAsset.getId(),
                        false,
                        IngredientImageSourceType.PHOTO
                ))
        );

        assertEquals(IngredientErrorCode.FIRST_IMAGE_MUST_BE_PRIMARY, exception.getErrorCode());
    }

    @Test
    void attach_rejectsDuplicateImageConnection() {
        User user = persistUser("duplicate-image-user", Provider.KAKAO);
        Ingredient ingredient = persistIngredient(user, "Sauce");
        ImageAsset imageAsset = persistImageAsset(user, AssetType.USER_UPLOAD, "images/sauce.png");

        attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                user.getId(),
                ingredient.getId(),
                imageAsset.getId(),
                true,
                IngredientImageSourceType.PHOTO
        ));
        entityManager.flush();
        entityManager.clear();

        ImageException exception = assertThrows(
                ImageException.class,
                () -> attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                        user.getId(),
                        ingredient.getId(),
                        imageAsset.getId(),
                        true,
                        IngredientImageSourceType.PHOTO
                ))
        );

        assertEquals(ImageErrorCode.IMAGE_ASSET_ALREADY_ATTACHED, exception.getErrorCode());
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
                IngredientSourceType.MANUAL
        ));
        entityManager.persist(ingredient);
        return ingredient;
    }

    private ImageAsset persistImageAsset(User user, AssetType assetType, String objectKey) {
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                assetType,
                ImageKind.INGREDIENT,
                StorageProvider.S3,
                objectKey,
                null,
                null
        ));
        entityManager.persist(imageAsset);
        return imageAsset;
    }
}
