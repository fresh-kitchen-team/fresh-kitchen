package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanIngredientImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.ImageSource;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.FoodClassificationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanIngredientImageServiceTest {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase =
            mock(StoreMultipartImageAssetUseCase.class);
    private final AiServerClient aiServerClient = mock(AiServerClient.class);
    private final ScanIngredientImageService service =
            new ScanIngredientImageService(storeMultipartImageAssetUseCase, aiServerClient);

    @Test
    void scan_storesIngredientImageAssetAndReturnsFoodClassificationCandidates() {
        MockMultipartFile file = new MockMultipartFile("file", "tomato.jpg", "image/jpeg", "image".getBytes());
        when(storeMultipartImageAssetUseCase.store(any(StoreMultipartImageAssetUseCase.Command.class)))
                .thenReturn(new StoreMultipartImageAssetUseCase.Result(10L, "/uploads/images/1/ingredient/tomato.jpg"));
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
                () -> assertEquals(10L, response.imageAssetId()),
                () -> assertEquals("/uploads/images/1/ingredient/tomato.jpg", response.imageUrl()),
                () -> assertEquals(ImageSource.GALLERY, response.imageSource()),
                () -> assertEquals("Tomato", response.recognizedItems().get(0).name()),
                () -> assertEquals(0.93, response.recognizedItems().get(0).confidence())
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
    }
}
