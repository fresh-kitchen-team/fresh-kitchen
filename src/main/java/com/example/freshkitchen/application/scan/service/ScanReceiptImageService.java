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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScanReceiptImageService implements ScanReceiptImageUseCase {

    private final StoreMultipartImageAssetUseCase storeMultipartImageAssetUseCase;
    private final AiServerClient aiServerClient;
    private final Clock clock;

    @Override
    public ScanDto.ReceiptImageScanResponse scan(Command command) {
        validate(command);
        byte[] content = bytes(command.file());
        ReceiptOcrResponse receiptOcr = aiServerClient.extractReceiptIngredients(
                command.file().getOriginalFilename(),
                content
        );
        LocalDate purchasedAt = purchasedAt(receiptOcr);
        StoreMultipartImageAssetUseCase.Result imageAsset = storeMultipartImageAssetUseCase.store(
                new StoreMultipartImageAssetUseCase.Command(
                        command.userId(),
                        ImageKind.RECEIPT,
                        command.file().getOriginalFilename(),
                        command.file().getContentType(),
                        content
                )
        );

        return new ScanDto.ReceiptImageScanResponse(
                ScanDto.ScanType.RECEIPT_IMAGE,
                purchasedAt,
                sourceType(receiptOcr),
                recognizedItems(receiptOcr, purchasedAt),
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

    private LocalDate purchasedAt(ReceiptOcrResponse receiptOcr) {
        if (receiptOcr.purchasedAt() != null) {
            return receiptOcr.purchasedAt();
        }
        return LocalDate.now(clock);
    }

    private static ScanDto.ReceiptPurchaseDateSourceType sourceType(ReceiptOcrResponse receiptOcr) {
        if (receiptOcr.purchasedAt() != null) {
            return ScanDto.ReceiptPurchaseDateSourceType.OCR;
        }
        return ScanDto.ReceiptPurchaseDateSourceType.DEFAULT_TODAY;
    }

    private static List<ScanDto.ReceiptRecognizedItem> recognizedItems(
            ReceiptOcrResponse receiptOcr,
            LocalDate purchasedAt
    ) {
        return receiptOcr.ingredients().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> new ScanDto.ReceiptRecognizedItem(name, purchasedAt))
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
