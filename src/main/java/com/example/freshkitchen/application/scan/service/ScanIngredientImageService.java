package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.port.ScanAiAnalysisPort;
import com.example.freshkitchen.application.scan.usecase.ScanIngredientImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanIngredientImageService implements ScanIngredientImageUseCase {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase;
    private final ScanAiAnalysisPort scanAiAnalysisPort;

    @Override
    public ScanDto.IngredientImageScanResponse scan(Command command) {
        MultipartFile file = ScanFileProcessor.validateCommand(
                command,
                ScanIngredientImageUseCase.Command::userId,
                ScanIngredientImageUseCase.Command::file
        );
        String originalFilename = ScanFileProcessor.originalFilename(file);
        byte[] content = ScanFileProcessor.bytes(file);
        ScanAiAnalysisPort.FoodClassification classification = scanAiAnalysisPort.classifyFood(
                originalFilename,
                content
        );
        StoreMultipartImageAssetUseCase.Result imageAsset = storeMultipartImageAssetUseCase.store(
                new StoreMultipartImageAssetUseCase.Command(
                        command.userId(),
                        ImageKind.INGREDIENT,
                        originalFilename,
                        file.getContentType(),
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

    private static List<ScanDto.RecognizedItem> recognizedItems(ScanAiAnalysisPort.FoodClassification classification) {
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
}
