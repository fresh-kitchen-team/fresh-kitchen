package com.example.freshkitchen.application.scan.dto;

import com.example.freshkitchen.domain.image.enums.ImageSource;

import java.util.List;

public final class ScanDto {

    private ScanDto() {
    }

    public record RecognizedItem(
            String name,
            Double confidence
    ) {
    }

    public record IngredientImageScanResponse(
            Long imageAssetId,
            String imageUrl,
            ImageSource imageSource,
            List<RecognizedItem> recognizedItems
    ) {
    }

    public record ReceiptImageScanResponse(
            Long imageAssetId,
            String imageUrl,
            List<RecognizedItem> recognizedItems
    ) {
    }
}
