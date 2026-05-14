package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanReceiptImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.ReceiptOcrResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
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
class ScanReceiptImageServiceTest {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase =
            mock(StoreMultipartImageAssetUseCase.class);
    private final AiServerClient aiServerClient = mock(AiServerClient.class);
    private final Clock clock = Clock.fixed(
            LocalDate.of(2026, 5, 13).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
    );
    private final ScanReceiptImageService service =
            new ScanReceiptImageService(storeMultipartImageAssetUseCase, aiServerClient, clock);
    private final OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-01T14:20:30+09:00");

    @Test
    void scan_storesReceiptImageAndReturnsOcrMetadata() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image".getBytes());
        when(storeMultipartImageAssetUseCase.store(any(StoreMultipartImageAssetUseCase.Command.class)))
                .thenReturn(new StoreMultipartImageAssetUseCase.Result(
                        11L,
                        ImageKind.RECEIPT,
                        StorageProvider.S3,
                        "https://cdn.example.com/images/1/receipt/receipt.jpg",
                        createdAt
                ));
        when(aiServerClient.extractReceiptIngredients(file))
                .thenReturn(new ReceiptOcrResponse(
                        "Emart",
                        LocalDate.of(2026, 5, 1),
                        List.of(
                                new ReceiptOcrResponse.RecognizedItem(
                                        "Egg",
                                        LocalDate.of(2026, 5, 16),
                                        ExpirySourceType.POLICY,
                                        0.87
                                ),
                                new ReceiptOcrResponse.RecognizedItem(
                                        "Milk",
                                        LocalDate.of(2026, 5, 8),
                                        ExpirySourceType.POLICY,
                                        0.84
                                )
                        ),
                        "Emart\nEgg\nMilk"
                ));

        ScanDto.ReceiptImageScanResponse response = service.scan(
                new ScanReceiptImageUseCase.Command(1L, file)
        );

        assertAll(
                () -> assertEquals(ScanDto.ScanType.RECEIPT_IMAGE, response.scanType()),
                () -> assertEquals("Emart", response.storeName()),
                () -> assertEquals(LocalDate.of(2026, 5, 1), response.purchasedAt()),
                () -> assertEquals(ScanDto.ReceiptPurchaseDateSourceType.OCR, response.sourceType()),
                () -> assertEquals(11L, response.imageAsset().imageAssetId()),
                () -> assertEquals(ImageKind.RECEIPT, response.imageAsset().kind()),
                () -> assertEquals(StorageProvider.S3, response.imageAsset().storageProvider()),
                () -> assertEquals("https://cdn.example.com/images/1/receipt/receipt.jpg", response.imageAsset().imageUrl()),
                () -> assertEquals("Egg", response.recognizedItems().get(0).name()),
                () -> assertEquals(LocalDate.of(2026, 5, 1), response.recognizedItems().get(0).registeredAt()),
                () -> assertEquals(LocalDate.of(2026, 5, 16), response.recognizedItems().get(0).estimatedExpiresAt()),
                () -> assertEquals(ExpirySourceType.POLICY, response.recognizedItems().get(0).expirySourceType()),
                () -> assertEquals(0.87, response.recognizedItems().get(0).confidence()),
                () -> assertEquals("Emart\nEgg\nMilk", response.ocrText()),
                () -> assertEquals(createdAt, response.createdAt())
        );
        ArgumentCaptor<StoreMultipartImageAssetUseCase.Command> captor =
                ArgumentCaptor.forClass(StoreMultipartImageAssetUseCase.Command.class);
        verify(storeMultipartImageAssetUseCase).store(captor.capture());
        assertEquals(ImageKind.RECEIPT, captor.getValue().kind());
        verify(aiServerClient).extractReceiptIngredients(file);
        InOrder inOrder = inOrder(aiServerClient, storeMultipartImageAssetUseCase);
        inOrder.verify(aiServerClient).extractReceiptIngredients(file);
        inOrder.verify(storeMultipartImageAssetUseCase).store(any(StoreMultipartImageAssetUseCase.Command.class));
    }

    @Test
    void scan_usesTodayWhenOcrPurchasedAtIsMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image".getBytes());
        when(storeMultipartImageAssetUseCase.store(any(StoreMultipartImageAssetUseCase.Command.class)))
                .thenReturn(new StoreMultipartImageAssetUseCase.Result(
                        11L,
                        ImageKind.RECEIPT,
                        StorageProvider.S3,
                        "https://cdn.example.com/images/1/receipt/receipt.jpg",
                        createdAt
                ));
        when(aiServerClient.extractReceiptIngredients(file))
                .thenReturn(new ReceiptOcrResponse(
                        "Emart",
                        null,
                        List.of(new ReceiptOcrResponse.RecognizedItem(
                                "Egg",
                                LocalDate.of(2026, 5, 16),
                                ExpirySourceType.POLICY,
                                0.87
                        )),
                        "Emart\nEgg"
                ));

        ScanDto.ReceiptImageScanResponse response = service.scan(
                new ScanReceiptImageUseCase.Command(1L, file)
        );

        assertAll(
                () -> assertEquals(LocalDate.of(2026, 5, 13), response.purchasedAt()),
                () -> assertEquals(ScanDto.ReceiptPurchaseDateSourceType.DEFAULT_TODAY, response.sourceType()),
                () -> assertEquals(LocalDate.of(2026, 5, 13), response.recognizedItems().get(0).registeredAt())
        );
    }

    @Test
    void scan_doesNotStoreImageAssetWhenReceiptOcrFails() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image".getBytes());
        RuntimeException failure = new RuntimeException("ocr server unavailable");
        when(aiServerClient.extractReceiptIngredients(file)).thenThrow(failure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.scan(new ScanReceiptImageUseCase.Command(1L, file))
        );

        assertEquals(failure, thrown);
        verify(aiServerClient).extractReceiptIngredients(file);
        verifyNoInteractions(storeMultipartImageAssetUseCase);
    }

    @Test
    void scan_isNotTransactionalSoAiCallRunsOutsideStorageTransaction() throws NoSuchMethodException {
        Method scan = ScanReceiptImageService.class.getMethod("scan", ScanReceiptImageUseCase.Command.class);

        assertFalse(scan.isAnnotationPresent(Transactional.class));
    }
}
