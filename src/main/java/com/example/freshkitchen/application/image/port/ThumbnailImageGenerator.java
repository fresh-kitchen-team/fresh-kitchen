package com.example.freshkitchen.application.image.port;

import java.util.Optional;

/**
 * 원본 이미지 바이트로부터 축소된 썸네일을 생성한다.
 * 디코딩 불가(비이미지) 등 생성 실패 시 {@link Optional#empty()}를 반환하여 호출 측이 best-effort로 처리하도록 한다.
 */
public interface ThumbnailImageGenerator {

    /**
     * @param source       원본 이미지 바이트
     * @param maxDimension 긴 변 기준 최대 픽셀 (원본보다 크면 확대하지 않음)
     */
    Optional<Thumbnail> generate(byte[] source, int maxDimension);

    record Thumbnail(byte[] content, String contentType, int width, int height) {
    }
}
