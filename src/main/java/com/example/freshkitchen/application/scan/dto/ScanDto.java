package com.example.freshkitchen.application.scan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ScanDto {

    private ScanDto() {
    }

    public record RecognizedItem(
            String name,
            String category,
            Double confidence
    ) {
    }

    public enum ScanType {
        INGREDIENT_IMAGE,
        RECEIPT_IMAGE,
        FRIDGE_IMAGE
    }

    public enum ReceiptPurchaseDateSourceType {
        OCR,
        DEFAULT_TODAY
    }

    public record ReceiptRecognizedItem(
            String name,
            String category,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            LocalDate registeredAt
    ) {
    }

    public record DetectedItem(
            String name,
            String category
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
            ImageAssetSummary imageAsset,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            LocalDate purchasedAt,
            ReceiptPurchaseDateSourceType purchasedAtSourceType,
            List<ReceiptRecognizedItem> recognizedItems,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            OffsetDateTime createdAt
    ) {
    }

    public record FridgeImageScanResponse(
            ScanType scanType,
            ImageAssetSummary imageAsset,
            List<DetectedItem> detectedItems,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            OffsetDateTime createdAt
    ) {
    }
}
