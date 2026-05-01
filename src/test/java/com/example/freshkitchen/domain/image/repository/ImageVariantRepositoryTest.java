package com.example.freshkitchen.domain.image.repository;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.ImageVariant;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.ImageVariantType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ImageVariantRepositoryTest extends PostgreSqlTestContainerSupport {

    private final ImageVariantRepository imageVariantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    ImageVariantRepositoryTest(ImageVariantRepository imageVariantRepository) {
        this.imageVariantRepository = imageVariantRepository;
    }

    @Test
    void findByImageAssetIdAndVariantType_returnsMatchingVariant() {
        User user = persistUser("variant-user", Provider.GOOGLE);
        ImageAsset imageAsset = persistImageAsset(user, "images/asset.png");
        ImageVariant thumbnail = persistImageVariant(
                imageAsset,
                ImageVariantType.THUMBNAIL,
                "images/thumb.png",
                120,
                90
        );
        persistImageVariant(
                imageAsset,
                ImageVariantType.DETAIL,
                "images/detail.png",
                640,
                480
        );

        entityManager.flush();
        entityManager.clear();

        ImageVariant foundVariant = imageVariantRepository.findByImageAssetIdAndVariantType(
                imageAsset.getId(),
                ImageVariantType.THUMBNAIL
        ).orElseThrow();

        assertEquals(thumbnail.getId(), foundVariant.getId());
        assertEquals(ImageVariantType.THUMBNAIL, foundVariant.getVariantType());
    }

    @Test
    void findAllByImageAssetIdOrderByIdAsc_returnsOnlyAssetVariantsInCreationOrder() {
        User user = persistUser("variant-list-user", Provider.KAKAO);
        ImageAsset targetAsset = persistImageAsset(user, "images/target.png");
        ImageAsset otherAsset = persistImageAsset(user, "images/other.png");
        ImageVariant thumbnail = persistImageVariant(
                targetAsset,
                ImageVariantType.THUMBNAIL,
                "images/target-thumb.png",
                120,
                90
        );
        ImageVariant detail = persistImageVariant(
                targetAsset,
                ImageVariantType.DETAIL,
                "images/target-detail.png",
                640,
                480
        );
        persistImageVariant(
                otherAsset,
                ImageVariantType.THUMBNAIL,
                "images/other-thumb.png",
                120,
                90
        );

        entityManager.flush();
        entityManager.clear();

        List<Long> variantIds = imageVariantRepository.findAllByImageAssetIdOrderByIdAsc(targetAsset.getId())
                .stream()
                .map(ImageVariant::getId)
                .toList();

        assertEquals(List.of(thumbnail.getId(), detail.getId()), variantIds);
    }

    @Test
    void existsByImageAssetIdAndVariantType_returnsTrueOnlyForExistingVariant() {
        User user = persistUser("variant-exists-user", Provider.GOOGLE);
        ImageAsset imageAsset = persistImageAsset(user, "images/asset.png");
        persistImageVariant(
                imageAsset,
                ImageVariantType.THUMBNAIL,
                "images/thumb.png",
                120,
                90
        );

        entityManager.flush();
        entityManager.clear();

        assertTrue(imageVariantRepository.existsByImageAssetIdAndVariantType(
                imageAsset.getId(),
                ImageVariantType.THUMBNAIL
        ));
        assertFalse(imageVariantRepository.existsByImageAssetIdAndVariantType(
                imageAsset.getId(),
                ImageVariantType.DETAIL
        ));
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
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

    private ImageVariant persistImageVariant(
            ImageAsset imageAsset,
            ImageVariantType variantType,
            String objectKey,
            int width,
            int height
    ) {
        ImageVariant imageVariant = ImageVariant.create(new ImageVariant.CreateCommand(
                imageAsset,
                variantType,
                objectKey,
                width,
                height
        ));
        entityManager.persist(imageVariant);
        return imageVariant;
    }
}
