package com.example.freshkitchen.infrastructure.ai;

import com.example.freshkitchen.infrastructure.ai.dto.FoodClassificationResponse;
import com.example.freshkitchen.infrastructure.ai.dto.FridgeDetectionResponse;
import com.example.freshkitchen.infrastructure.ai.dto.ReceiptOcrResponse;
import com.example.freshkitchen.infrastructure.ai.exception.AiServerErrorCode;
import com.example.freshkitchen.infrastructure.ai.exception.AiServerException;

final class AiResponseProcessor {

    private AiResponseProcessor() {
    }

    static FoodClassificationResponse process(FoodClassificationResponse response) {
        validateFoodClassification(response);
        return new FoodClassificationResponse(
                response.bestMatch().trim(),
                response.category().trim(),
                response.confidence(),
                response.top3().stream()
                        .map(item -> new FoodClassificationResponse.FoodCandidate(
                                item.name().trim(),
                                item.confidence()
                        ))
                        .toList(),
                response.source(),
                response.geminiReason(),
                response.autoSaved()
        );
    }

    static ReceiptOcrResponse process(ReceiptOcrResponse response) {
        validateReceiptOcr(response);
        return new ReceiptOcrResponse(
                response.purchasedAt(),
                response.ingredients().stream()
                        .map(item -> new ReceiptOcrResponse.IngredientItem(
                                item.name().trim(),
                                item.category().trim()
                        ))
                        .toList()
        );
    }

    static FridgeDetectionResponse process(FridgeDetectionResponse response) {
        validateFridgeDetection(response);
        return new FridgeDetectionResponse(
                response.items().stream()
                        .map(item -> new FridgeDetectionResponse.DetectedItem(
                                item.name().trim(),
                                item.category().trim()
                        ))
                        .toList()
        );
    }

    private static void validateFoodClassification(FoodClassificationResponse response) {
        if (response == null
                || response.bestMatch() == null
                || response.bestMatch().isBlank()
                || response.category() == null
                || response.category().isBlank()
                || response.confidence() == null
                || response.top3() == null
                || response.source() == null
                || response.source().isBlank()
                || response.top3().stream().anyMatch(AiResponseProcessor::isInvalidFoodCandidate)) {
            throw new AiServerException(AiServerErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private static boolean isInvalidFoodCandidate(FoodClassificationResponse.FoodCandidate item) {
        return item == null
                || item.name() == null
                || item.name().isBlank()
                || item.confidence() == null;
    }

    private static void validateReceiptOcr(ReceiptOcrResponse response) {
        if (response == null
                || response.ingredients() == null
                || response.ingredients().stream().anyMatch(AiResponseProcessor::isInvalidIngredientItem)) {
            throw new AiServerException(AiServerErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private static boolean isInvalidIngredientItem(ReceiptOcrResponse.IngredientItem item) {
        return item == null
                || item.name() == null
                || item.name().isBlank()
                || item.category() == null
                || item.category().isBlank();
    }

    private static void validateFridgeDetection(FridgeDetectionResponse response) {
        if (response == null
                || response.items() == null
                || response.items().stream().anyMatch(AiResponseProcessor::isInvalidDetectedItem)) {
            throw new AiServerException(AiServerErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private static boolean isInvalidDetectedItem(FridgeDetectionResponse.DetectedItem item) {
        return item == null
                || item.name() == null
                || item.name().isBlank()
                || item.category() == null
                || item.category().isBlank();
    }
}
