package com.example.freshkitchen.presentation.scan;

import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.application.scan.usecase.ScanIngredientImageUseCase;
import com.example.freshkitchen.application.scan.usecase.ScanReceiptImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageSource;
import com.example.freshkitchen.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/scan")
public class ScanController {

    private final ScanIngredientImageUseCase scanIngredientImageUseCase;
    private final ScanReceiptImageUseCase scanReceiptImageUseCase;

    @PostMapping(value = "/ingredient-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ScanDto.IngredientImageScanResponse>> scanIngredientImage(
            @AuthenticationPrincipal Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam ImageSource imageSource
    ) {
        return ApiResponse.success(scanIngredientImageUseCase.scan(
                new ScanIngredientImageUseCase.Command(userId, file, imageSource)
        ));
    }

    @PostMapping(value = "/receipt-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ScanDto.ReceiptImageScanResponse>> scanReceiptImage(
            @AuthenticationPrincipal Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(scanReceiptImageUseCase.scan(
                new ScanReceiptImageUseCase.Command(userId, file)
        ));
    }
}
