package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanFridgeImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.FridgeDetectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanFridgeImageService implements ScanFridgeImageUseCase {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase;
    private final AiServerClient aiServerClient;

    @Override
    public ScanDto.FridgeImageScanResponse scan(Command command) {
        validate(command);
        byte[] content = bytes(command.file());
        FridgeDetectionResponse detection = aiServerClient.detectFridgeObjects(
                command.file().getOriginalFilename(),
                content
        );
        StoreMultipartImageAssetUseCase.Result imageAsset = storeMultipartImageAssetUseCase.store(
                new StoreMultipartImageAssetUseCase.Command(
                        command.userId(),
                        ImageKind.FRIDGE,
                        command.file().getOriginalFilename(),
                        command.file().getContentType(),
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

    private static List<ScanDto.DetectedItem> detectedItems(FridgeDetectionResponse detection) {
        return detection.items().stream()
                .map(item -> new ScanDto.DetectedItem(item.name(), item.category()))
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
