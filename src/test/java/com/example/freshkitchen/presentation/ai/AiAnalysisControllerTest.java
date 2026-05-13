package com.example.freshkitchen.presentation.ai;

import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.global.exception.handler.GlobalExceptionHandler;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.FoodClassificationResponse;
import com.example.freshkitchen.infrastructure.ai.dto.FridgeDetectionResponse;
import com.example.freshkitchen.infrastructure.ai.dto.ReceiptOcrResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiAnalysisControllerTest {

    private final AiServerClient aiServerClient = mock(AiServerClient.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AiAnalysisController(aiServerClient))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void classifyFood_returnsClassification() throws Exception {
        given(aiServerClient.classifyFood(any()))
                .willReturn(new FoodClassificationResponse(
                        "Kimchi stew",
                        0.91,
                        List.of(new FoodClassificationResponse.FoodCandidate("Kimchi stew", 0.91)),
                        "model",
                        "red soup with kimchi",
                        false
                ));

        mockMvc.perform(multipart("/api/v1/ai/food-classification").file(imageFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.bestMatch").value("Kimchi stew"))
                .andExpect(jsonPath("$.data.confidence").value(0.91))
                .andExpect(jsonPath("$.data.top3[0].name").value("Kimchi stew"))
                .andExpect(jsonPath("$.data.source").value("model"))
                .andExpect(jsonPath("$.data.geminiReason").value("red soup with kimchi"))
                .andExpect(jsonPath("$.data.autoSaved").value(false));

        then(aiServerClient).should().classifyFood(any());
    }

    @Test
    void extractReceiptIngredients_returnsIngredients() throws Exception {
        given(aiServerClient.extractReceiptIngredients(any()))
                .willReturn(new ReceiptOcrResponse(
                        "Emart",
                        LocalDate.of(2026, 5, 1),
                        List.of(new ReceiptOcrResponse.RecognizedItem(
                                "Egg",
                                LocalDate.of(2026, 5, 16),
                                ExpirySourceType.POLICY,
                                0.87
                        )),
                        "Emart\nEgg"
                ));

        mockMvc.perform(multipart("/api/v1/ai/receipt-ocr").file(imageFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeName").value("Emart"))
                .andExpect(jsonPath("$.data.purchasedAt").value("2026-05-01"))
                .andExpect(jsonPath("$.data.recognizedItems[0].name").value("Egg"))
                .andExpect(jsonPath("$.data.recognizedItems[0].estimatedExpiresAt").value("2026-05-16"))
                .andExpect(jsonPath("$.data.recognizedItems[0].expirySourceType").value("POLICY"))
                .andExpect(jsonPath("$.data.recognizedItems[0].confidence").value(0.87))
                .andExpect(jsonPath("$.data.ocrText").value("Emart\nEgg"));

        then(aiServerClient).should().extractReceiptIngredients(any());
    }

    @Test
    void detectFridgeObjects_returnsObjects() throws Exception {
        given(aiServerClient.detectFridgeObjects(any()))
                .willReturn(new FridgeDetectionResponse(List.of(
                        new FridgeDetectionResponse.DetectedObject(
                                "Apple",
                                0.82,
                                new FridgeDetectionResponse.Box(1.0, 2.0, 30.0, 40.0)
                        )
                )));

        mockMvc.perform(multipart("/api/v1/ai/fridge-detection").file(imageFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objects[0].name").value("Apple"))
                .andExpect(jsonPath("$.data.objects[0].confidence").value(0.82))
                .andExpect(jsonPath("$.data.objects[0].box.x1").value(1.0))
                .andExpect(jsonPath("$.data.objects[0].box.y2").value(40.0));

        then(aiServerClient).should().detectFridgeObjects(any());
    }

    @Test
    void classifyFood_returnsBadRequestWhenFileIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/v1/ai/food-classification"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/ai/food-classification"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(aiServerClient);
    }

    @Test
    void classifyFood_returnsBadRequestWhenFileIsEmpty() throws Exception {
        mockMvc.perform(multipart("/api/v1/ai/food-classification").file(emptyImageFile()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/ai/food-classification"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(aiServerClient);
    }

    @Test
    void extractReceiptIngredients_returnsBadRequestWhenFileIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/v1/ai/receipt-ocr"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/ai/receipt-ocr"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(aiServerClient);
    }

    @Test
    void extractReceiptIngredients_returnsBadRequestWhenFileIsEmpty() throws Exception {
        mockMvc.perform(multipart("/api/v1/ai/receipt-ocr").file(emptyImageFile()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/ai/receipt-ocr"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(aiServerClient);
    }

    @Test
    void detectFridgeObjects_returnsBadRequestWhenFileIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/v1/ai/fridge-detection"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/ai/fridge-detection"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(aiServerClient);
    }

    @Test
    void detectFridgeObjects_returnsBadRequestWhenFileIsEmpty() throws Exception {
        mockMvc.perform(multipart("/api/v1/ai/fridge-detection").file(emptyImageFile()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/ai/fridge-detection"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(aiServerClient);
    }

    private static MockMultipartFile imageFile() {
        return new MockMultipartFile("file", "food.jpg", "image/jpeg", "image".getBytes());
    }

    private static MockMultipartFile emptyImageFile() {
        return new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
    }
}
