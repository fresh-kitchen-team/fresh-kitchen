package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanReceiptImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.ReceiptOcrResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanReceiptImageServiceTest {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase =
            mock(StoreMultipartImageAssetUseCase.class);
    private final AiServerClient aiServerClient = mock(AiServerClient.class);
    private final ScanReceiptImageService service =
            new ScanReceiptImageService(storeMultipartImageAssetUseCase, aiServerClient);

    @Test
    void scan_storesReceiptImageAndReturnsOcrIngredients() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image".getBytes());
        when(storeMultipartImageAssetUseCase.store(any(StoreMultipartImageAssetUseCase.Command.class)))
                .thenReturn(new StoreMultipartImageAssetUseCase.Result(11L, "/uploads/images/1/receipt/receipt.jpg"));
        when(aiServerClient.extractReceiptIngredients(file))
                .thenReturn(new ReceiptOcrResponse(List.of("Egg", "Milk")));

        ScanDto.ReceiptImageScanResponse response = service.scan(
                new ScanReceiptImageUseCase.Command(1L, file)
        );

        assertAll(
                () -> assertEquals(11L, response.imageAssetId()),
                () -> assertEquals("/uploads/images/1/receipt/receipt.jpg", response.imageUrl()),
                () -> assertEquals("Egg", response.recognizedItems().get(0).name()),
                () -> assertNull(response.recognizedItems().get(0).confidence())
        );
        ArgumentCaptor<StoreMultipartImageAssetUseCase.Command> captor =
                ArgumentCaptor.forClass(StoreMultipartImageAssetUseCase.Command.class);
        verify(storeMultipartImageAssetUseCase).store(captor.capture());
        assertEquals(ImageKind.RECEIPT, captor.getValue().kind());
        verify(aiServerClient).extractReceiptIngredients(file);
    }
}
