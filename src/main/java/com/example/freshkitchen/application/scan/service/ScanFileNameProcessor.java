package com.example.freshkitchen.application.scan.service;

import com.example.freshkitchen.global.exception.BusinessValidationException;

final class ScanFileNameProcessor {

    private ScanFileNameProcessor() {
    }

    static String process(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessValidationException("originalFilename must not be blank");
        }
        return originalFilename.trim();
    }
}
