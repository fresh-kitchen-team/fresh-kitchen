package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.global.exception.BusinessValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.function.Function;

final class ScanFileProcessor {

    private ScanFileProcessor() {
    }

    static <T> MultipartFile validateCommand(
            T command,
            Function<T, Long> userIdExtractor,
            Function<T, MultipartFile> fileExtractor
    ) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (userIdExtractor.apply(command) == null) {
            throw new BusinessValidationException("userId must not be null");
        }

        MultipartFile file = fileExtractor.apply(command);
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("file must not be empty");
        }
        return file;
    }

    static String originalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessValidationException("originalFilename must not be blank");
        }
        return originalFilename.trim();
    }

    static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessValidationException("failed to read image file", e);
        }
    }
}
