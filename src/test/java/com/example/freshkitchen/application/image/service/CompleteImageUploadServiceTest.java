package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.usecase.CompleteImageUploadUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(CompleteImageUploadService.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CompleteImageUploadServiceTest extends PostgreSqlTestContainerSupport {

    private final CompleteImageUploadUseCase completeImageUploadUseCase;
    private final ImageAssetRepository imageAssetRepository;

    @PersistenceContext
    private EntityManager entityManager;

    CompleteImageUploadServiceTest(
            CompleteImageUploadUseCase completeImageUploadUseCase,
            ImageAssetRepository imageAssetRepository
    ) {
        this.completeImageUploadUseCase = completeImageUploadUseCase;
        this.imageAssetRepository = imageAssetRepository;
    }

    @Test
    void complete_persistsUserUploadImageAsset() {
        User user = persistUser("complete-user", Provider.GOOGLE);

        Long imageAssetId = completeImageUploadUseCase.complete(new CompleteImageUploadUseCase.Command(
                user.getId(),
                ImageKind.INGREDIENT,
                objectKey(user.getId(), ImageKind.INGREDIENT, ".jpg"),
                null,
                null
        ));

        entityManager.flush();
        entityManager.clear();

        ImageAsset imageAsset = imageAssetRepository.findByIdAndUserId(imageAssetId, user.getId())
                .orElseThrow();

        assertEquals(user.getId(), imageAsset.getUser().getId());
        assertEquals(AssetType.USER_UPLOAD, imageAsset.getAssetType());
        assertEquals(ImageKind.INGREDIENT, imageAsset.getKind());
        assertEquals(StorageProvider.S3, imageAsset.getStorageProvider());
        assertEquals("images/%d/ingredient/00000000-0000-0000-0000-000000000001.jpg".formatted(user.getId()), imageAsset.getObjectKey());
        assertNull(imageAsset.getWidth());
        assertNull(imageAsset.getHeight());
    }

    @Test
    void complete_rejectsBlankObjectKey() {
        User user = persistUser("blank-object-key-user", Provider.KAKAO);

        assertThrows(
                BusinessValidationException.class,
                () -> completeImageUploadUseCase.complete(new CompleteImageUploadUseCase.Command(
                        user.getId(),
                        ImageKind.INGREDIENT,
                        " ",
                        null,
                        null
                ))
        );
    }

    @Test
    void complete_rejectsNegativeWidth() {
        User user = persistUser("negative-width-user", Provider.GOOGLE);

        assertThrows(
                BusinessValidationException.class,
                () -> completeImageUploadUseCase.complete(new CompleteImageUploadUseCase.Command(
                        user.getId(),
                        ImageKind.INGREDIENT,
                        objectKey(user.getId(), ImageKind.INGREDIENT, ".png"),
                        -1,
                        null
                ))
        );
    }

    @Test
    void complete_rejectsOtherUserObjectKeyPrefix() {
        User user = persistUser("object-key-owner", Provider.GOOGLE);

        assertThrows(
                BusinessValidationException.class,
                () -> completeImageUploadUseCase.complete(new CompleteImageUploadUseCase.Command(
                        user.getId(),
                        ImageKind.INGREDIENT,
                        objectKey(user.getId() + 1, ImageKind.INGREDIENT, ".jpg"),
                        null,
                        null
                ))
        );
    }

    @Test
    void complete_rejectsDifferentKindObjectKeyPrefix() {
        User user = persistUser("object-key-kind-user", Provider.KAKAO);

        assertThrows(
                BusinessValidationException.class,
                () -> completeImageUploadUseCase.complete(new CompleteImageUploadUseCase.Command(
                        user.getId(),
                        ImageKind.INGREDIENT,
                        "images/%d/profile/00000000-0000-0000-0000-000000000001.jpg".formatted(user.getId()),
                        null,
                        null
                ))
        );
    }

    @Test
    void complete_rejectsObjectKeyWithNestedPath() {
        User user = persistUser("object-key-nested-user", Provider.GOOGLE);

        assertThrows(
                BusinessValidationException.class,
                () -> completeImageUploadUseCase.complete(new CompleteImageUploadUseCase.Command(
                        user.getId(),
                        ImageKind.INGREDIENT,
                        "images/%d/ingredient/00000000-0000-0000-0000-000000000001.jpg/evil".formatted(user.getId()),
                        null,
                        null
                ))
        );
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
    }

    private static String objectKey(Long userId, ImageKind kind, String extension) {
        return "images/%d/%s/%s%s".formatted(
                userId,
                kind.name().toLowerCase(),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                extension
        );
    }
}
