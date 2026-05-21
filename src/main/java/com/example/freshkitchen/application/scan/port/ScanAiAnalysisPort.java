package com.example.freshkitchen.application.scan.port;

import java.time.LocalDate;
import java.util.List;

public interface ScanAiAnalysisPort {

    /**
     * @param originalFilename non-blank original filename
     * @param content non-empty file content
     */
    FoodClassification classifyFood(String originalFilename, byte[] content);

    /**
     * @param originalFilename non-blank original filename
     * @param content non-empty file content
     */
    ReceiptOcr extractReceiptIngredients(String originalFilename, byte[] content);

    /**
     * @param originalFilename non-blank original filename
     * @param content non-empty file content
     */
    FridgeDetection detectFridgeObjects(String originalFilename, byte[] content);

    record FoodClassification(
            String bestMatch,
            String category,
            Double confidence,
            List<FoodCandidate> top3
    ) {
    }

    record FoodCandidate(
            String name,
            Double confidence
    ) {
    }

    record ReceiptOcr(
            LocalDate purchasedAt,
            List<ReceiptIngredient> ingredients
    ) {
    }

    record ReceiptIngredient(
            String name,
            String category
    ) {
    }

    record FridgeDetection(
            List<DetectedItem> items
    ) {
    }

    record DetectedItem(
            String name,
            String category
    ) {
    }
}
