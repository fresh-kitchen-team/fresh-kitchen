package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.port.ScanAiAnalysisPort;
import com.example.freshkitchen.application.scan.usecase.ScanReceiptImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScanReceiptImageService implements ScanReceiptImageUseCase {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase;
    private final ScanAiAnalysisPort scanAiAnalysisPort;
    private final Clock clock;

    @Override
    public ScanDto.ReceiptImageScanResponse scan(Command command) {
        MultipartFile file = ScanFileProcessor.validateCommand(
                command,
                ScanReceiptImageUseCase.Command::userId,
                ScanReceiptImageUseCase.Command::file
        );
        String originalFilename = ScanFileProcessor.originalFilename(file);
        byte[] content = ScanFileProcessor.bytes(file);
        ScanAiAnalysisPort.ReceiptOcr receiptOcr = scanAiAnalysisPort.extractReceiptIngredients(
                originalFilename,
                content
        );
        LocalDate purchasedAt = purchasedAt(receiptOcr);
        StoreMultipartImageAssetUseCase.Result imageAsset = storeMultipartImageAssetUseCase.store(
                new StoreMultipartImageAssetUseCase.Command(
                        command.userId(),
                        ImageKind.RECEIPT,
                        originalFilename,
                        file.getContentType(),
                        content
                )
        );

        return new ScanDto.ReceiptImageScanResponse(
                ScanDto.ScanType.RECEIPT_IMAGE,
                new ScanDto.ImageAssetSummary(
                        imageAsset.imageAssetId(),
                        imageAsset.kind(),
                        imageAsset.storageProvider(),
                        imageAsset.imageUrl()
                ),
                purchasedAt,
                sourceType(receiptOcr),
                recognizedItems(receiptOcr, purchasedAt),
                imageAsset.createdAt()
        );
    }

    private LocalDate purchasedAt(ScanAiAnalysisPort.ReceiptOcr receiptOcr) {
        if (receiptOcr.purchasedAt() != null) {
            return receiptOcr.purchasedAt();
        }
        return LocalDate.now(clock);
    }

    private static ScanDto.ReceiptPurchaseDateSourceType sourceType(ScanAiAnalysisPort.ReceiptOcr receiptOcr) {
        if (receiptOcr.purchasedAt() != null) {
            return ScanDto.ReceiptPurchaseDateSourceType.OCR;
        }
        return ScanDto.ReceiptPurchaseDateSourceType.DEFAULT_TODAY;
    }

    private static List<ScanDto.ReceiptRecognizedItem> recognizedItems(
            ScanAiAnalysisPort.ReceiptOcr receiptOcr,
            LocalDate purchasedAt
    ) {
        return receiptOcr.ingredients().stream()
                .filter(Objects::nonNull)
                .map(item -> new ScanDto.ReceiptRecognizedItem(
                        item.name().trim(),
                        item.category(),
                        purchasedAt
                ))
                .toList();
    }
}
