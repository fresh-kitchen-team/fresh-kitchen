package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.usecase.DeleteStoredImageAssetUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.image.repository.IngredientImageRepository;
import com.example.freshkitchen.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteStoredImageAssetServiceTest {

    private final ImageAssetRepository imageAssetRepository = mock(ImageAssetRepository.class);
    private final IngredientImageRepository ingredientImageRepository = mock(IngredientImageRepository.class);
    private final MultipartImageStoragePort multipartImageStoragePort = mock(MultipartImageStoragePort.class);
    private final TransactionOperations transactionOperations = mock(TransactionOperations.class);
    private final DeleteStoredImageAssetService service = new DeleteStoredImageAssetService(
            imageAssetRepository,
            ingredientImageRepository,
            multipartImageStoragePort,
            transactionOperations
    );

    @Test
    void deleteIfUnattached_deletesImageAssetAndStoredObject() {
        runInTransaction();
        ImageAsset imageAsset = imageAsset();
        when(imageAssetRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(imageAsset));
        when(ingredientImageRepository.existsByImageAssetId(imageAsset.getId())).thenReturn(false);

        service.deleteIfUnattached(new DeleteStoredImageAssetUseCase.Command(1L, 20L));

        verify(imageAssetRepository).delete(imageAsset);
        verify(multipartImageStoragePort).delete(new MultipartImageStoragePort.DeleteCommand(
                "images/1/ingredient/tomato.jpg"
        ));
    }

    @Test
    void deleteIfUnattached_doesNotDeleteAttachedImageAsset() {
        runInTransaction();
        ImageAsset imageAsset = imageAsset();
        when(imageAssetRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(imageAsset));
        when(ingredientImageRepository.existsByImageAssetId(imageAsset.getId())).thenReturn(true);

        service.deleteIfUnattached(new DeleteStoredImageAssetUseCase.Command(1L, 20L));

        verify(imageAssetRepository, never()).delete(any());
        verify(multipartImageStoragePort, never()).delete(any());
    }

    private void runInTransaction() {
        when(transactionOperations.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    private static ImageAsset imageAsset() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        ImageAsset imageAsset = ImageAsset.create(new ImageAsset.CreateCommand(
                user,
                AssetType.USER_UPLOAD,
                ImageKind.INGREDIENT,
                StorageProvider.S3,
                "images/1/ingredient/tomato.jpg",
                null,
                null
        ));
        ReflectionTestUtils.setField(imageAsset, "id", 20L);
        return imageAsset;
    }
}
