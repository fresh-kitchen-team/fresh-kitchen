package com.example.freshkitchen.domain.image.repository;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ImageAssetRepositoryTest extends PostgreSqlTestContainerSupport {

    private final ImageAssetRepository imageAssetRepository;

    @PersistenceContext
    private EntityManager entityManager;

    ImageAssetRepositoryTest(ImageAssetRepository imageAssetRepository) {
        this.imageAssetRepository = imageAssetRepository;
    }

    @Test
    void findByIdAndUserId_returnsOnlyOwnerImageAsset() {
        User owner = persistUser("image-owner", Provider.GOOGLE);
        User otherUser = persistUser("image-other", Provider.KAKAO);
        ImageAsset imageAsset = persistUserUploadImageAsset(owner, "images/owner.png");

        entityManager.flush();
        entityManager.clear();

        Optional<ImageAsset> foundImageAsset = imageAssetRepository.findByIdAndUserId(imageAsset.getId(), owner.getId());
        Optional<ImageAsset> notFoundImageAsset = imageAssetRepository.findByIdAndUserId(imageAsset.getId(), otherUser.getId());

        assertEquals(imageAsset.getId(), foundImageAsset.orElseThrow().getId());
        assertTrue(notFoundImageAsset.isEmpty());
    }

    @Test
    void findAttachableByIdAndUserId_allowsOwnerUploadAndSystemDefaultOnly() {
        User owner = persistUser("attach-owner", Provider.GOOGLE);
        User otherUser = persistUser("attach-other", Provider.KAKAO);
        ImageAsset ownerImageAsset = persistUserUploadImageAsset(owner, "images/owner.png");
        ImageAsset otherImageAsset = persistUserUploadImageAsset(otherUser, "images/other.png");
        ImageAsset systemDefaultImageAsset = persistSystemDefaultImageAsset("images/default.png");

        entityManager.flush();
        entityManager.clear();

        Optional<ImageAsset> ownerAttachable = imageAssetRepository.findAttachableByIdAndUserId(
                ownerImageAsset.getId(),
                owner.getId()
        );
        Optional<ImageAsset> systemAttachable = imageAssetRepository.findAttachableByIdAndUserId(
                systemDefaultImageAsset.getId(),
                owner.getId()
        );
        Optional<ImageAsset> otherNotAttachable = imageAssetRepository.findAttachableByIdAndUserId(
                otherImageAsset.getId(),
                owner.getId()
        );

        assertEquals(ownerImageAsset.getId(), ownerAttachable.orElseThrow().getId());
        assertEquals(systemDefaultImageAsset.getId(), systemAttachable.orElseThrow().getId());
        assertTrue(otherNotAttachable.isEmpty());
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
    }

    private ImageAsset persistUserUploadImageAsset(User user, String objectKey) {
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

    private ImageAsset persistSystemDefaultImageAsset(String objectKey) {
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                null,
                AssetType.SYSTEM_DEFAULT,
                ImageKind.INGREDIENT,
                StorageProvider.LOCAL,
                objectKey,
                300,
                300
        ));
        entityManager.persist(imageAsset);
        return imageAsset;
    }
}
