package com.example.freshkitchen.presentation.ai;

import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.FoodClassificationResponse;
import com.example.freshkitchen.infrastructure.ai.dto.FridgeDetectionResponse;
import com.example.freshkitchen.infrastructure.ai.dto.ReceiptOcrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final AiServerClient aiServerClient;

    @PostMapping(value = "/food-classification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FoodClassificationResponse>> classifyFood(@RequestPart MultipartFile file) {
        return ApiResponse.success(aiServerClient.classifyFood(file));
    }

    @PostMapping(value = "/receipt-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReceiptOcrResponse>> extractReceiptIngredients(@RequestPart MultipartFile file) {
        return ApiResponse.success(aiServerClient.extractReceiptIngredients(file));
    }

    @PostMapping(value = "/fridge-detection", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FridgeDetectionResponse>> detectFridgeObjects(@RequestPart MultipartFile file) {
        return ApiResponse.success(aiServerClient.detectFridgeObjects(file));
    }
}
