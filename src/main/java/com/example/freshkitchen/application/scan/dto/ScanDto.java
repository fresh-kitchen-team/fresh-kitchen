package com.example.freshkitchen.application.scan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ScanDto {

    private ScanDto() {
    }

    public record RecognizedItem(
            String name,
            Double confidence
    ) {
    }

    public enum ScanType {
        INGREDIENT_IMAGE,
        RECEIPT_IMAGE
    }

    public enum ReceiptPurchaseDateSourceType {
        OCR,
        DEFAULT_TODAY
    }

    public record ReceiptRecognizedItem(
            String name,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            LocalDate registeredAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            LocalDate estimatedExpiresAt,
            ExpirySourceType expirySourceType,
            Double confidence
    ) {
    }

    public record ImageAssetSummary(
            Long imageAssetId,
            ImageKind kind,
            StorageProvider storageProvider,
            String imageUrl
    ) {
    }

    public record IngredientImageScanResponse(
            ScanType scanType,
            ImageAssetSummary imageAsset,
            List<RecognizedItem> recognizedItems,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            OffsetDateTime createdAt
    ) {
    }

    public record ReceiptImageScanResponse(
            ScanType scanType,
            String storeName,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            LocalDate purchasedAt,
            ReceiptPurchaseDateSourceType sourceType,
            ImageAssetSummary imageAsset,
            List<ReceiptRecognizedItem> recognizedItems,
            String ocrText,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            OffsetDateTime createdAt
    ) {
    }
}
