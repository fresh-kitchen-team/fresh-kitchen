package com.example.freshkitchen.application.scan.usecase;

import com.example.freshkitchen.application.scan.dto.ScanDto;
import org.springframework.web.multipart.MultipartFile;

public interface ScanFridgeImageUseCase {

    ScanDto.FridgeImageScanResponse scan(Command command);

    record Command(
            Long userId,
            MultipartFile file
    ) {
    }
}
