package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.port.ScanAiAnalysisPort;
import com.example.freshkitchen.application.scan.usecase.ScanFridgeImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanFridgeImageService implements ScanFridgeImageUseCase {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase;
    private final ScanAiAnalysisPort scanAiAnalysisPort;

    @Override
    public ScanDto.FridgeImageScanResponse scan(Command command) {
        MultipartFile file = ScanFileProcessor.validateCommand(
                command,
                ScanFridgeImageUseCase.Command::userId,
                ScanFridgeImageUseCase.Command::file
        );
        String originalFilename = ScanFileProcessor.originalFilename(file);
        byte[] content = ScanFileProcessor.bytes(file);
        ScanAiAnalysisPort.FridgeDetection detection = scanAiAnalysisPort.detectFridgeObjects(
                originalFilename,
                content
        );
        StoreMultipartImageAssetUseCase.Result imageAsset = storeMultipartImageAssetUseCase.store(
                new StoreMultipartImageAssetUseCase.Command(
                        command.userId(),
                        ImageKind.FRIDGE,
                        originalFilename,
                        file.getContentType(),
                        content
                )
        );

        return new ScanDto.FridgeImageScanResponse(
                ScanDto.ScanType.FRIDGE_IMAGE,
                new ScanDto.ImageAssetSummary(
                        imageAsset.imageAssetId(),
                        imageAsset.kind(),
                        imageAsset.storageProvider(),
                        imageAsset.imageUrl()
                ),
                detectedItems(detection),
                imageAsset.createdAt()
        );
    }

    private static List<ScanDto.DetectedItem> detectedItems(ScanAiAnalysisPort.FridgeDetection detection) {
        return detection.items().stream()
                .map(item -> new ScanDto.DetectedItem(item.name(), item.category()))
                .toList();
    }
}
