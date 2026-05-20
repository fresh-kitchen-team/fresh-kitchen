package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanReceiptImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.ReceiptOcrResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        MockMultipartFile file = new MockMultipartFile("file", " receipt.jpg ", "image/jpeg", "image".getBytes());
        when(storeMultipartImageAssetUseCase.store(any(StoreMultipartImageAssetUseCase.Command.class)))
                .thenReturn(new StoreMultipartImageAssetUseCase.Result(
                        11L,
                        ImageKind.RECEIPT,
                        StorageProvider.S3,
                        "https://cdn.example.com/images/1/receipt/receipt.jpg",
                        createdAt
                ));
        when(aiServerClient.extractReceiptIngredients(eq("receipt.jpg"), any(byte[].class)))
                .thenReturn(new ReceiptOcrResponse(
                        LocalDate.of(2026, 5, 1),
                        List.of(
                                new ReceiptOcrResponse.IngredientItem("Egg", "ETC"),
                                new ReceiptOcrResponse.IngredientItem("Milk", "DAIRY")
                        )
                ));

        ScanDto.ReceiptImageScanResponse response = service.scan(
                new ScanReceiptImageUseCase.Command(1L, file)
        );

        assertAll(
                () -> assertEquals(ScanDto.ScanType.RECEIPT_IMAGE, response.scanType()),
                () -> assertEquals(11L, response.imageAsset().imageAssetId()),
                () -> assertEquals(ImageKind.RECEIPT, response.imageAsset().kind()),
                () -> assertEquals(LocalDate.of(2026, 5, 1), response.purchasedAt()),
                () -> assertEquals(ScanDto.ReceiptPurchaseDateSourceType.OCR, response.purchasedAtSourceType()),
                () -> assertEquals("Egg", response.recognizedItems().get(0).name()),
                () -> assertEquals("ETC", response.recognizedItems().get(0).category()),
                () -> assertEquals(LocalDate.of(2026, 5, 1), response.recognizedItems().get(0).registeredAt()),
                () -> assertEquals("Milk", response.recognizedItems().get(1).name()),
                () -> assertEquals("DAIRY", response.recognizedItems().get(1).category()),
                () -> assertEquals(2, response.recognizedItems().size()),
                () -> assertEquals(createdAt, response.createdAt())
        );
        ArgumentCaptor<StoreMultipartImageAssetUseCase.Command> captor =
                ArgumentCaptor.forClass(StoreMultipartImageAssetUseCase.Command.class);
        verify(storeMultipartImageAssetUseCase).store(captor.capture());
        assertEquals(ImageKind.RECEIPT, captor.getValue().kind());
        assertEquals("receipt.jpg", captor.getValue().originalFilename());
        assertEquals("image/jpeg", captor.getValue().contentType());
        assertArrayEquals("image".getBytes(), captor.getValue().content());
        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(aiServerClient).extractReceiptIngredients(eq("receipt.jpg"), contentCaptor.capture());
        assertArrayEquals("image".getBytes(), contentCaptor.getValue());
        InOrder inOrder = inOrder(aiServerClient, storeMultipartImageAssetUseCase);
        inOrder.verify(aiServerClient).extractReceiptIngredients(eq("receipt.jpg"), any(byte[].class));
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
        when(aiServerClient.extractReceiptIngredients(eq("receipt.jpg"), any(byte[].class)))
                .thenReturn(new ReceiptOcrResponse(
                        null,
                        List.of(new ReceiptOcrResponse.IngredientItem("Egg", "ETC"))
                ));

        ScanDto.ReceiptImageScanResponse response = service.scan(
                new ScanReceiptImageUseCase.Command(1L, file)
        );

        assertAll(
                () -> assertEquals(LocalDate.of(2026, 5, 13), response.purchasedAt()),
                () -> assertEquals(
                        ScanDto.ReceiptPurchaseDateSourceType.DEFAULT_TODAY,
                        response.purchasedAtSourceType()
                ),
                () -> assertEquals(LocalDate.of(2026, 5, 13), response.recognizedItems().get(0).registeredAt())
        );
    }

    @Test
    void scan_doesNotStoreImageAssetWhenReceiptOcrFails() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image".getBytes());
        RuntimeException failure = new RuntimeException("ocr server unavailable");
        when(aiServerClient.extractReceiptIngredients(eq("receipt.jpg"), any(byte[].class))).thenThrow(failure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.scan(new ScanReceiptImageUseCase.Command(1L, file))
        );

        assertEquals(failure, thrown);
        verify(aiServerClient).extractReceiptIngredients(eq("receipt.jpg"), any(byte[].class));
        verifyNoInteractions(storeMultipartImageAssetUseCase);
    }

    @Test
    void scan_doesNotCallAiServerWhenUserIdIsMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image".getBytes());

        BusinessValidationException thrown = assertThrows(
                BusinessValidationException.class,
                () -> service.scan(new ScanReceiptImageUseCase.Command(null, file))
        );

        assertEquals("userId must not be null", thrown.getMessage());
        verifyNoInteractions(aiServerClient, storeMultipartImageAssetUseCase);
    }

    @Test
    void scan_doesNotReadFileWhenOriginalFilenameIsBlank() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("   ");

        BusinessValidationException thrown = assertThrows(
                BusinessValidationException.class,
                () -> service.scan(new ScanReceiptImageUseCase.Command(1L, file))
        );

        assertEquals("originalFilename must not be blank", thrown.getMessage());
        verify(file, never()).getBytes();
        verifyNoInteractions(aiServerClient, storeMultipartImageAssetUseCase);
    }

    @Test
    void scan_isNotTransactionalSoAiCallRunsOutsideStorageTransaction() throws NoSuchMethodException {
        Method scan = ScanReceiptImageService.class.getMethod("scan", ScanReceiptImageUseCase.Command.class);

        assertFalse(scan.isAnnotationPresent(Transactional.class));
    }
}
