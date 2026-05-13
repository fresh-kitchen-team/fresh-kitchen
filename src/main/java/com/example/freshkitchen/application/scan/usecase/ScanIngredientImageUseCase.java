package com.example.freshkitchen.application.scan.usecase;

import com.example.freshkitchen.application.scan.dto.ScanDto;
import com.example.freshkitchen.domain.image.enums.ImageSource;
import org.springframework.web.multipart.MultipartFile;

public interface ScanIngredientImageUseCase {

    ScanDto.IngredientImageScanResponse scan(Command command);

    record Command(
            Long userId,
            MultipartFile file,
            ImageSource imageSource
    ) {
    }
}
