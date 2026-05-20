package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanIngredientImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.FoodClassificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanIngredientImageService implements ScanIngredientImageUseCase {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase;
    private final AiServerClient aiServerClient;

    @Override
    public ScanDto.IngredientImageScanResponse scan(Command command) {
        validate(command);
        byte[] content = bytes(command.file());
        String originalFilename = originalFilename(command.file());
        FoodClassificationResponse classification = aiServerClient.classifyFood(
                originalFilename,
                content
        );
        StoreMultipartImageAssetUseCase.Result imageAsset = storeMultipartImageAssetUseCase.store(
                new StoreMultipartImageAssetUseCase.Command(
                        command.userId(),
                        ImageKind.INGREDIENT,
                        originalFilename,
                        command.file().getContentType(),
                        content
                )
        );

        return new ScanDto.IngredientImageScanResponse(
                ScanDto.ScanType.INGREDIENT_IMAGE,
                new ScanDto.ImageAssetSummary(
                        imageAsset.imageAssetId(),
                        imageAsset.kind(),
                        imageAsset.storageProvider(),
                        imageAsset.imageUrl()
                ),
                recognizedItems(classification),
                imageAsset.createdAt()
        );
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        if (command.file() == null || command.file().isEmpty()) {
            throw new BusinessValidationException("file must not be empty");
        }
    }

    private static String originalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessValidationException("originalFilename must not be blank");
        }
        return originalFilename.trim();
    }

    private static List<ScanDto.RecognizedItem> recognizedItems(FoodClassificationResponse classification) {
        if (classification.top3().isEmpty()) {
            return List.of(new ScanDto.RecognizedItem(
                    classification.bestMatch(),
                    classification.category(),
                    classification.confidence()
            ));
        }
        return classification.top3().stream()
                .map(item -> new ScanDto.RecognizedItem(item.name(), classification.category(), item.confidence()))
                .toList();
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessValidationException("failed to read image file", e);
        }
    }
}
