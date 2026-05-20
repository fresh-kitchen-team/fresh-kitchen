package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanFridgeImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.FridgeDetectionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
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
class ScanFridgeImageServiceTest {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase =
            mock(StoreMultipartImageAssetUseCase.class);
    private final AiServerClient aiServerClient = mock(AiServerClient.class);
    private final ScanFridgeImageService service =
            new ScanFridgeImageService(storeMultipartImageAssetUseCase, aiServerClient);
    private final OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-01T14:20:30+09:00");

    @Test
    void scan_storesFridgeImageAndReturnsDetectedItems() {
        MockMultipartFile file = new MockMultipartFile("file", " fridge.jpg ", "image/jpeg", "image".getBytes());
        when(storeMultipartImageAssetUseCase.store(any(StoreMultipartImageAssetUseCase.Command.class)))
                .thenReturn(new StoreMultipartImageAssetUseCase.Result(
                        12L,
                        ImageKind.FRIDGE,
                        StorageProvider.S3,
                        "https://cdn.example.com/images/1/fridge/fridge.jpg",
                        createdAt
                ));
        when(aiServerClient.detectFridgeObjects(eq("fridge.jpg"), any(byte[].class)))
                .thenReturn(new FridgeDetectionResponse(List.of(
                        new FridgeDetectionResponse.DetectedItem("계란", "ETC"),
                        new FridgeDetectionResponse.DetectedItem("우유", "DAIRY")
                )));

        ScanDto.FridgeImageScanResponse response = service.scan(new ScanFridgeImageUseCase.Command(1L, file));

        assertAll(
                () -> assertEquals(ScanDto.ScanType.FRIDGE_IMAGE, response.scanType()),
                () -> assertEquals(12L, response.imageAsset().imageAssetId()),
                () -> assertEquals(ImageKind.FRIDGE, response.imageAsset().kind()),
                () -> assertEquals("계란", response.detectedItems().get(0).name()),
                () -> assertEquals("ETC", response.detectedItems().get(0).category()),
                () -> assertEquals("우유", response.detectedItems().get(1).name()),
                () -> assertEquals("DAIRY", response.detectedItems().get(1).category()),
                () -> assertEquals(createdAt, response.createdAt())
        );
        ArgumentCaptor<StoreMultipartImageAssetUseCase.Command> captor =
                ArgumentCaptor.forClass(StoreMultipartImageAssetUseCase.Command.class);
        verify(storeMultipartImageAssetUseCase).store(captor.capture());
        assertAll(
                () -> assertEquals(1L, captor.getValue().userId()),
                () -> assertEquals(ImageKind.FRIDGE, captor.getValue().kind()),
                () -> assertEquals("fridge.jpg", captor.getValue().originalFilename()),
                () -> assertEquals("image/jpeg", captor.getValue().contentType()),
                () -> assertArrayEquals("image".getBytes(), captor.getValue().content())
        );
        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(aiServerClient).detectFridgeObjects(eq("fridge.jpg"), contentCaptor.capture());
        assertArrayEquals("image".getBytes(), contentCaptor.getValue());
        InOrder inOrder = inOrder(aiServerClient, storeMultipartImageAssetUseCase);
        inOrder.verify(aiServerClient).detectFridgeObjects(eq("fridge.jpg"), any(byte[].class));
        inOrder.verify(storeMultipartImageAssetUseCase).store(any(StoreMultipartImageAssetUseCase.Command.class));
    }

    @Test
    void scan_doesNotStoreImageAssetWhenFridgeDetectionFails() {
        MockMultipartFile file = new MockMultipartFile("file", "fridge.jpg", "image/jpeg", "image".getBytes());
        RuntimeException failure = new RuntimeException("ai server unavailable");
        when(aiServerClient.detectFridgeObjects(eq("fridge.jpg"), any(byte[].class))).thenThrow(failure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.scan(new ScanFridgeImageUseCase.Command(1L, file))
        );

        assertEquals(failure, thrown);
        verify(aiServerClient).detectFridgeObjects(eq("fridge.jpg"), any(byte[].class));
        verifyNoInteractions(storeMultipartImageAssetUseCase);
    }

    @Test
    void scan_doesNotCallAiServerWhenUserIdIsMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "fridge.jpg", "image/jpeg", "image".getBytes());

        BusinessValidationException thrown = assertThrows(
                BusinessValidationException.class,
                () -> service.scan(new ScanFridgeImageUseCase.Command(null, file))
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
                () -> service.scan(new ScanFridgeImageUseCase.Command(1L, file))
        );

        assertEquals("originalFilename must not be blank", thrown.getMessage());
        verify(file, never()).getBytes();
        verifyNoInteractions(aiServerClient, storeMultipartImageAssetUseCase);
    }

    @Test
    void scan_isNotTransactionalSoAiCallRunsOutsideStorageTransaction() throws NoSuchMethodException {
        Method scan = ScanFridgeImageService.class.getMethod("scan", ScanFridgeImageUseCase.Command.class);

        assertFalse(scan.isAnnotationPresent(Transactional.class));
    }
}
