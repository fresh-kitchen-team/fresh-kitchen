package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanReceiptImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.ai.dto.ReceiptOcrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanReceiptImageService implements ScanReceiptImageUseCase {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase;
    private final AiServerClient aiServerClient;

    @Override
    @Transactional
    public ScanDto.ReceiptImageScanResponse scan(Command command) {
        validate(command);
        StoreMultipartImageAssetUseCase.Result imageAsset = storeMultipartImageAssetUseCase.store(
                new StoreMultipartImageAssetUseCase.Command(
                        command.userId(),
                        ImageKind.RECEIPT,
                        command.file().getOriginalFilename(),
                        command.file().getContentType(),
                        bytes(command.file())
                )
        );
        ReceiptOcrResponse receiptOcr = aiServerClient.extractReceiptIngredients(command.file());

        return new ScanDto.ReceiptImageScanResponse(
                ScanDto.ScanType.RECEIPT_IMAGE,
                new ScanDto.ImageAssetSummary(
                        imageAsset.imageAssetId(),
                        imageAsset.kind(),
                        imageAsset.storageProvider(),
                        imageAsset.imageUrl()
                ),
                recognizedItems(receiptOcr),
                imageAsset.createdAt()
        );
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.file() == null || command.file().isEmpty()) {
            throw new BusinessValidationException("file must not be empty");
        }
    }

    private static List<ScanDto.RecognizedItem> recognizedItems(ReceiptOcrResponse receiptOcr) {
        return receiptOcr.ingredients().stream()
                .map(name -> new ScanDto.RecognizedItem(name, null))
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
