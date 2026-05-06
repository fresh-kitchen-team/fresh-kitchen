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
                "images/1/ingredient/upload.jpg",
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
        assertEquals("images/1/ingredient/upload.jpg", imageAsset.getObjectKey());
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
                        "images/negative.png",
                        -1,
                        null
                ))
        );
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
    }
}
