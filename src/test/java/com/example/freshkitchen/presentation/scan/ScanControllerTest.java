package com.example.freshkitchen.presentation.scan;

import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanIngredientImageUseCase;
import com.example.freshkitchen.application.scan.usecase.ScanReceiptImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.ImageSource;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.global.exception.handler.GlobalExceptionHandler;
import com.example.freshkitchen.global.security.JwtAuthentication;
import com.example.freshkitchen.global.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScanControllerTest {

    private ScanIngredientImageUseCase scanIngredientImageUseCase;
    private ScanReceiptImageUseCase scanReceiptImageUseCase;
    private MockMvc mockMvc;
    private final OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-01T14:20:30+09:00");

    @BeforeEach
    void setUp() {
        scanIngredientImageUseCase = mock(ScanIngredientImageUseCase.class);
        scanReceiptImageUseCase = mock(ScanReceiptImageUseCase.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ScanController(scanIngredientImageUseCase, scanReceiptImageUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void scanIngredientImage_returnsRecognizedItemsWithImageAsset() throws Exception {
        when(scanIngredientImageUseCase.scan(any(ScanIngredientImageUseCase.Command.class)))
                .thenReturn(new ScanDto.IngredientImageScanResponse(
                        ScanDto.ScanType.INGREDIENT_IMAGE,
                        new ScanDto.ImageAssetSummary(
                                10L,
                                ImageKind.INGREDIENT,
                                StorageProvider.S3,
                                "https://cdn.example.com/images/1/ingredient/tomato.jpg"
                        ),
                        List.of(new ScanDto.RecognizedItem("Tomato", 0.93)),
                        createdAt
                ));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(1L, Role.USER));

        mockMvc.perform(multipart("/api/v1/scan/ingredient-image")
                        .file(imageFile())
                        .param("imageSource", "CAMERAX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.scanType").value("INGREDIENT_IMAGE"))
                .andExpect(jsonPath("$.data.imageAsset.imageAssetId").value(10))
                .andExpect(jsonPath("$.data.imageAsset.kind").value("INGREDIENT"))
                .andExpect(jsonPath("$.data.imageAsset.storageProvider").value("S3"))
                .andExpect(jsonPath("$.data.imageAsset.imageUrl")
                        .value("https://cdn.example.com/images/1/ingredient/tomato.jpg"))
                .andExpect(jsonPath("$.data.recognizedItems[0].name").value("Tomato"))
                .andExpect(jsonPath("$.data.recognizedItems[0].confidence").value(0.93))
                .andExpect(jsonPath("$.data.createdAt").value("2026-05-01T14:20:30+09:00"));

        ArgumentCaptor<ScanIngredientImageUseCase.Command> captor =
                ArgumentCaptor.forClass(ScanIngredientImageUseCase.Command.class);
        verify(scanIngredientImageUseCase).scan(captor.capture());
        assertAll(
                () -> assertEquals(1L, captor.getValue().userId()),
                () -> assertEquals("tomato.jpg", captor.getValue().file().getOriginalFilename()),
                () -> assertEquals(ImageSource.CAMERAX, captor.getValue().imageSource())
        );
    }

    @Test
    void scanReceiptImage_returnsRecognizedItemsWithImageAsset() throws Exception {
        when(scanReceiptImageUseCase.scan(any(ScanReceiptImageUseCase.Command.class)))
                .thenReturn(new ScanDto.ReceiptImageScanResponse(
                        ScanDto.ScanType.RECEIPT_IMAGE,
                        "Emart",
                        LocalDate.of(2026, 5, 1),
                        ScanDto.ReceiptPurchaseDateSourceType.OCR,
                        new ScanDto.ImageAssetSummary(
                                11L,
                                ImageKind.RECEIPT,
                                StorageProvider.S3,
                                "https://cdn.example.com/images/1/receipt/receipt.jpg"
                        ),
                        List.of(new ScanDto.ReceiptRecognizedItem(
                                "Egg",
                                LocalDate.of(2026, 5, 1),
                                LocalDate.of(2026, 5, 16),
                                ExpirySourceType.POLICY,
                                0.87
                        )),
                        "Emart\nEgg 1 pack",
                        createdAt
                ));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(1L, Role.USER));

        mockMvc.perform(multipart("/api/v1/scan/receipt-image").file(imageFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scanType").value("RECEIPT_IMAGE"))
                .andExpect(jsonPath("$.data.storeName").value("Emart"))
                .andExpect(jsonPath("$.data.purchasedAt").value("2026-05-01"))
                .andExpect(jsonPath("$.data.sourceType").value("OCR"))
                .andExpect(jsonPath("$.data.imageAsset.imageAssetId").value(11))
                .andExpect(jsonPath("$.data.imageAsset.kind").value("RECEIPT"))
                .andExpect(jsonPath("$.data.imageAsset.storageProvider").value("S3"))
                .andExpect(jsonPath("$.data.imageAsset.imageUrl")
                        .value("https://cdn.example.com/images/1/receipt/receipt.jpg"))
                .andExpect(jsonPath("$.data.recognizedItems[0].name").value("Egg"))
                .andExpect(jsonPath("$.data.recognizedItems[0].registeredAt").value("2026-05-01"))
                .andExpect(jsonPath("$.data.recognizedItems[0].estimatedExpiresAt").value("2026-05-16"))
                .andExpect(jsonPath("$.data.recognizedItems[0].expirySourceType").value("POLICY"))
                .andExpect(jsonPath("$.data.recognizedItems[0].confidence").value(0.87))
                .andExpect(jsonPath("$.data.ocrText").value("Emart\nEgg 1 pack"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-05-01T14:20:30+09:00"));

        ArgumentCaptor<ScanReceiptImageUseCase.Command> captor =
                ArgumentCaptor.forClass(ScanReceiptImageUseCase.Command.class);
        verify(scanReceiptImageUseCase).scan(captor.capture());
        assertEquals(1L, captor.getValue().userId());
    }

    @Test
    void scanIngredientImage_returnsBadRequestWhenImageSourceIsInvalid() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(1L, Role.USER));

        mockMvc.perform(multipart("/api/v1/scan/ingredient-image")
                        .file(imageFile())
                        .param("imageSource", "CAMERA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"));

        verifyNoInteractions(scanIngredientImageUseCase);
    }

    @Test
    void scanReceiptImage_returnsBadRequestWhenFileIsMissing() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(1L, Role.USER));

        mockMvc.perform(multipart("/api/v1/scan/receipt-image"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"));

        verifyNoInteractions(scanReceiptImageUseCase);
    }

    private static MockMultipartFile imageFile() {
        return new MockMultipartFile("file", "tomato.jpg", "image/jpeg", "image".getBytes());
    }
}
