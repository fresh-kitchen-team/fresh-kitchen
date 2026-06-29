package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.usecase.PurgeOrphanImageAssetsUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.ImageVariant;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.ImageVariantType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.image.repository.ImageVariantRepository;
import com.example.freshkitchen.domain.catalog.repository.IngredientCatalogRepository;
import com.example.freshkitchen.domain.image.repository.IngredientImageRepository;
import com.example.freshkitchen.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurgeOrphanImageAssetsServiceTest {

    private final ImageAssetRepository imageAssetRepository = mock(ImageAssetRepository.class);
    private final ImageVariantRepository imageVariantRepository = mock(ImageVariantRepository.class);
    private final IngredientImageRepository ingredientImageRepository = mock(IngredientImageRepository.class);
    private final IngredientCatalogRepository ingredientCatalogRepository = mock(IngredientCatalogRepository.class);
    private final MultipartImageStoragePort multipartImageStoragePort = mock(MultipartImageStoragePort.class);
    private final TransactionOperations transactionOperations = mock(TransactionOperations.class);
    private final PurgeOrphanImageAssetsService service = new PurgeOrphanImageAssetsService(
            imageAssetRepository,
            imageVariantRepository,
            ingredientImageRepository,
            ingredientCatalogRepository,
            multipartImageStoragePort,
            transactionOperations
    );

    private static final OffsetDateTime CUTOFF = OffsetDateTime.parse("2026-06-01T00:00:00Z");

    @Test
    void purge_deletesOrphanAssetWithVariantsAndStorageObjects() {
        runInTransaction();
        ImageAsset orphan = imageAsset(20L, "images/1/ingredient/tomato.jpg");
        ImageVariant thumbnail = variant(orphan, "images/1/ingredient/tomato_thumb.jpg");
        when(imageAssetRepository.findOrphans(eq(AssetType.USER_UPLOAD), eq(CUTOFF), any(Pageable.class)))
                .thenReturn(List.of(orphan));
        when(imageAssetRepository.findById(20L)).thenReturn(Optional.of(orphan));
        when(ingredientImageRepository.existsByImageAssetId(20L)).thenReturn(false);
        when(imageVariantRepository.findAllByImageAssetIdOrderByIdAsc(20L)).thenReturn(List.of(thumbnail));

        PurgeOrphanImageAssetsUseCase.Result result =
                service.purge(new PurgeOrphanImageAssetsUseCase.Command(CUTOFF, 200));

        assertAll(
                () -> assertEquals(1, result.deletedAssets()),
                () -> assertEquals(1, result.deletedVariants()),
                () -> assertEquals(2, result.deletedStorageObjects())
        );
        verify(imageVariantRepository).deleteByImageAssetId(20L);
        verify(imageAssetRepository).delete(orphan);
        verify(multipartImageStoragePort).delete(new MultipartImageStoragePort.DeleteCommand(
                "images/1/ingredient/tomato.jpg"));
        verify(multipartImageStoragePort).delete(new MultipartImageStoragePort.DeleteCommand(
                "images/1/ingredient/tomato_thumb.jpg"));
    }

    @Test
    void purge_skipsAssetThatGotReattachedBeforeDeletion() {
        runInTransaction();
        ImageAsset orphan = imageAsset(20L, "images/1/ingredient/tomato.jpg");
        when(imageAssetRepository.findOrphans(eq(AssetType.USER_UPLOAD), eq(CUTOFF), any(Pageable.class)))
                .thenReturn(List.of(orphan));
        when(imageAssetRepository.findById(20L)).thenReturn(Optional.of(orphan));
        // 조회 시점엔 고아였지만 삭제 직전 다시 참조가 생긴 경우
        when(ingredientImageRepository.existsByImageAssetId(20L)).thenReturn(true);

        PurgeOrphanImageAssetsUseCase.Result result =
                service.purge(new PurgeOrphanImageAssetsUseCase.Command(CUTOFF, 200));

        assertEquals(0, result.deletedAssets());
        verify(imageAssetRepository, never()).delete(any());
        verify(imageVariantRepository, never()).deleteByImageAssetId(any());
        verify(multipartImageStoragePort, never()).delete(any());
    }

    @Test
    void purge_skipsAssetReattachedAsCatalogDefaultBeforeDeletion() {
        runInTransaction();
        ImageAsset orphan = imageAsset(20L, "images/1/ingredient/tomato.jpg");
        when(imageAssetRepository.findOrphans(eq(AssetType.USER_UPLOAD), eq(CUTOFF), any(Pageable.class)))
                .thenReturn(List.of(orphan));
        when(imageAssetRepository.findById(20L)).thenReturn(Optional.of(orphan));
        when(ingredientImageRepository.existsByImageAssetId(20L)).thenReturn(false);
        // 조회 시점엔 고아였지만 삭제 직전 카탈로그 기본 이미지로 다시 참조된 경우
        when(ingredientCatalogRepository.existsByDefaultImageAssetId(20L)).thenReturn(true);

        PurgeOrphanImageAssetsUseCase.Result result =
                service.purge(new PurgeOrphanImageAssetsUseCase.Command(CUTOFF, 200));

        assertEquals(0, result.deletedAssets());
        verify(imageAssetRepository, never()).delete(any());
        verify(imageVariantRepository, never()).deleteByImageAssetId(any());
        verify(multipartImageStoragePort, never()).delete(any());
    }

    @Test
    void purge_returnsZero_whenNoOrphans() {
        when(imageAssetRepository.findOrphans(eq(AssetType.USER_UPLOAD), eq(CUTOFF), any(Pageable.class)))
                .thenReturn(List.of());

        PurgeOrphanImageAssetsUseCase.Result result =
                service.purge(new PurgeOrphanImageAssetsUseCase.Command(CUTOFF, 200));

        assertEquals(0, result.deletedAssets());
        verify(multipartImageStoragePort, never()).delete(any());
    }

    private void runInTransaction() {
        when(transactionOperations.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    private static ImageAsset imageAsset(Long id, String objectKey) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.S3,
                objectKey,
                null,
                null
        ));
        ReflectionTestUtils.setField(imageAsset, "id", id);
        return imageAsset;
    }

    private static ImageVariant variant(ImageAsset imageAsset, String objectKey) {
        return ImageVariant.create(new ImageVariant.CreateCommand(
                imageAsset,
                ImageVariantType.THUMBNAIL,
                objectKey,
                100,
                100
        ));
    }
}
