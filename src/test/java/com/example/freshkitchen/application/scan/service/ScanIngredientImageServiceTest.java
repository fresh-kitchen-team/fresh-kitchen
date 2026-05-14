package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanIngredientImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.ImageSource;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.FoodClassificationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanIngredientImageServiceTest {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase =
            mock(StoreMultipartImageAssetUseCase.class);
    private final AiServerClient aiServerClient = mock(AiServerClient.class);
    private final ScanIngredientImageService service =
            new ScanIngredientImageService(storeMultipartImageAssetUseCase, aiServerClient);
    private final OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-01T14:20:30+09:00");

    @Test
    void scan_storesIngredientImageAssetAndReturnsFoodClassificationCandidates() {
        MockMultipartFile file = new MockMultipartFile("file", "tomato.jpg", "image/jpeg", "image".getBytes());
        when(storeMultipartImageAssetUseCase.store(any(StoreMultipartImageAssetUseCase.Command.class)))
                .thenReturn(new StoreMultipartImageAssetUseCase.Result(
                        10L,
                        ImageKind.INGREDIENT,
                        StorageProvider.S3,
                        "https://cdn.example.com/images/1/ingredient/tomato.jpg",
                        createdAt
                ));
        when(aiServerClient.classifyFood(file))
                .thenReturn(new FoodClassificationResponse(
                        "Tomato",
                        0.93,
                        List.of(new FoodClassificationResponse.FoodCandidate("Tomato", 0.93)),
                        "model",
                        "red round vegetable",
                        false
                ));

        ScanDto.IngredientImageScanResponse response = service.scan(
                new ScanIngredientImageUseCase.Command(1L, file, ImageSource.GALLERY)
        );

        assertAll(
                () -> assertEquals(ScanDto.ScanType.INGREDIENT_IMAGE, response.scanType()),
                () -> assertEquals(10L, response.imageAsset().imageAssetId()),
                () -> assertEquals(ImageKind.INGREDIENT, response.imageAsset().kind()),
                () -> assertEquals(StorageProvider.S3, response.imageAsset().storageProvider()),
                () -> assertEquals("https://cdn.example.com/images/1/ingredient/tomato.jpg",
                        response.imageAsset().imageUrl()),
                () -> assertEquals("Tomato", response.recognizedItems().get(0).name()),
                () -> assertEquals(0.93, response.recognizedItems().get(0).confidence()),
                () -> assertEquals(createdAt, response.createdAt())
        );
        ArgumentCaptor<StoreMultipartImageAssetUseCase.Command> captor =
                ArgumentCaptor.forClass(StoreMultipartImageAssetUseCase.Command.class);
        verify(storeMultipartImageAssetUseCase).store(captor.capture());
        assertAll(
                () -> assertEquals(1L, captor.getValue().userId()),
                () -> assertEquals(ImageKind.INGREDIENT, captor.getValue().kind()),
                () -> assertEquals("tomato.jpg", captor.getValue().originalFilename()),
                () -> assertEquals("image/jpeg", captor.getValue().contentType()),
                () -> assertArrayEquals("image".getBytes(), captor.getValue().content())
        );
        verify(aiServerClient).classifyFood(file);
        InOrder inOrder = inOrder(aiServerClient, storeMultipartImageAssetUseCase);
        inOrder.verify(aiServerClient).classifyFood(file);
        inOrder.verify(storeMultipartImageAssetUseCase).store(any(StoreMultipartImageAssetUseCase.Command.class));
    }

    @Test
    void scan_doesNotStoreImageAssetWhenFoodClassificationFails() {
        MockMultipartFile file = new MockMultipartFile("file", "tomato.jpg", "image/jpeg", "image".getBytes());
        RuntimeException failure = new RuntimeException("ai server unavailable");
        when(aiServerClient.classifyFood(file)).thenThrow(failure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.scan(new ScanIngredientImageUseCase.Command(1L, file, ImageSource.GALLERY))
        );

        assertEquals(failure, thrown);
        verify(aiServerClient).classifyFood(file);
        verifyNoInteractions(storeMultipartImageAssetUseCase);
    }

    @Test
    void scan_doesNotCallAiServerWhenUserIdIsMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "tomato.jpg", "image/jpeg", "image".getBytes());

        BusinessValidationException thrown = assertThrows(
                BusinessValidationException.class,
                () -> service.scan(new ScanIngredientImageUseCase.Command(null, file, ImageSource.GALLERY))
        );

        assertEquals("userId must not be null", thrown.getMessage());
        verifyNoInteractions(aiServerClient, storeMultipartImageAssetUseCase);
    }

    @Test
    void scan_isNotTransactionalSoAiCallRunsOutsideStorageTransaction() throws NoSuchMethodException {
        Method scan = ScanIngredientImageService.class.getMethod("scan", ScanIngredientImageUseCase.Command.class);

        assertFalse(scan.isAnnotationPresent(Transactional.class));
    }
}
