package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.usecase.AttachIngredientImageUseCase;
import com.example.freshkitchen.application.image.usecase.DeleteStoredImageAssetUseCase;
import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.image.usecase.UploadIngredientImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadIngredientImageServiceTest {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase =
            mock(StoreMultipartImageAssetUseCase.class);
    private final AttachIngredientImageUseCase attachIngredientImageUseCase =
            mock(AttachIngredientImageUseCase.class);
    private final DeleteStoredImageAssetUseCase deleteStoredImageAssetUseCase =
            mock(DeleteStoredImageAssetUseCase.class);
    private final UploadIngredientImageService service = new UploadIngredientImageService(
            storeMultipartImageAssetUseCase,
            attachIngredientImageUseCase,
            deleteStoredImageAssetUseCase
    );

    @Test
    void upload_attachesStoredImageAndDoesNotCompensateWhenAttachSucceeds() {
        when(storeMultipartImageAssetUseCase.store(any(StoreMultipartImageAssetUseCase.Command.class)))
                .thenReturn(storedImage());
        when(attachIngredientImageUseCase.attach(any(AttachIngredientImageUseCase.Command.class)))
                .thenReturn(30L);

        UploadIngredientImageUseCase.Result result = service.upload(command());

        assertEquals(30L, result.ingredientImageId());
        verify(deleteStoredImageAssetUseCase, never()).deleteIfUnattached(any());
    }

    @Test
    void upload_deletesStoredImageAssetWhenAttachFails() {
        RuntimeException failure = new RuntimeException("attach failed");
        when(storeMultipartImageAssetUseCase.store(any(StoreMultipartImageAssetUseCase.Command.class)))
                .thenReturn(storedImage());
        when(attachIngredientImageUseCase.attach(any(AttachIngredientImageUseCase.Command.class)))
                .thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.upload(command()));

        assertEquals(failure, thrown);
        verify(deleteStoredImageAssetUseCase).deleteIfUnattached(new DeleteStoredImageAssetUseCase.Command(1L, 20L));
    }

    @Test
    void upload_isNotTransactionalSoStorageRunsOutsideAttachTransaction() throws NoSuchMethodException {
        Method upload = UploadIngredientImageService.class.getMethod("upload", UploadIngredientImageUseCase.Command.class);

        assertFalse(UploadIngredientImageService.class.isAnnotationPresent(Transactional.class));
        assertFalse(upload.isAnnotationPresent(Transactional.class));
    }

    private static StoreMultipartImageAssetUseCase.Result storedImage() {
        return new StoreMultipartImageAssetUseCase.Result(
                20L,
                ImageKind.INGREDIENT,
                StorageProvider.S3,
                "https://cdn.example.com/images/1/ingredient/tomato.jpg",
                OffsetDateTime.parse("2026-05-01T14:20:30+09:00")
        );
    }

    private static UploadIngredientImageUseCase.Command command() {
        return new UploadIngredientImageUseCase.Command(
                1L,
                10L,
                "tomato.jpg",
                "image/jpeg",
                "image".getBytes(),
                true,
                IngredientImageSourceType.PHOTO
        );
    }
}
