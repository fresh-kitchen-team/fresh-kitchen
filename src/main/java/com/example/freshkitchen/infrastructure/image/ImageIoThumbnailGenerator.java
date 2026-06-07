package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.ThumbnailImageGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Optional;

/**
 * JDK 내장 ImageIO 기반 썸네일 생성기. 외부 라이브러리 의존성 없이 동작한다.
 * 디코딩 실패(비이미지) 등 예외 상황에서는 {@link Optional#empty()}를 반환하여 best-effort로 처리되도록 한다.
 * 디코딩 전 헤더에서 해상도를 검사해 과도하게 큰 이미지(decompression bomb)는 건너뛴다.
 */
@Slf4j
@Component
public class ImageIoThumbnailGenerator implements ThumbnailImageGenerator {

    private static final String THUMBNAIL_FORMAT = "jpg";
    private static final String THUMBNAIL_CONTENT_TYPE = "image/jpeg";
    /** 전체 디코딩 전 허용하는 원본 최대 픽셀 수 (약 50MP). 초과 시 OOM 방지를 위해 건너뛴다. */
    private static final long DEFAULT_MAX_SOURCE_PIXELS = 50_000_000L;

    private final long maxSourcePixels;

    public ImageIoThumbnailGenerator() {
        this(DEFAULT_MAX_SOURCE_PIXELS);
    }

    ImageIoThumbnailGenerator(long maxSourcePixels) {
        this.maxSourcePixels = maxSourcePixels;
    }

    @Override
    public Optional<Thumbnail> generate(byte[] source, int maxDimension) {
        if (source == null || source.length == 0 || maxDimension <= 0) {
            return Optional.empty();
        }
        try (ImageInputStream imageInputStream =
                     ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            if (imageInputStream == null) {
                return Optional.empty();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                int originalWidth = reader.getWidth(0);
                int originalHeight = reader.getHeight(0);
                if (originalWidth <= 0 || originalHeight <= 0) {
                    return Optional.empty();
                }
                if ((long) originalWidth * originalHeight > maxSourcePixels) {
                    log.warn("[Thumbnail] source too large, skipping. {}x{} exceeds {} pixels",
                            originalWidth, originalHeight, maxSourcePixels);
                    return Optional.empty();
                }
                return Optional.of(render(reader.read(0), originalWidth, originalHeight, maxDimension));
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            log.warn("[Thumbnail] generation failed, skipping. reason={}", e.getMessage());
            return Optional.empty();
        }
    }

    private Thumbnail render(BufferedImage original, int originalWidth, int originalHeight, int maxDimension)
            throws java.io.IOException {
        double scale = Math.min(1.0, (double) maxDimension / Math.max(originalWidth, originalHeight));
        int targetWidth = Math.max(1, (int) Math.round(originalWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(originalHeight * scale));

        BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            // 투명 PNG 등 알파 채널을 JPEG로 변환할 때 흰 배경으로 채운다.
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(thumbnail, THUMBNAIL_FORMAT, out)) {
            throw new java.io.IOException("no writer for thumbnail format: " + THUMBNAIL_FORMAT);
        }
        return new Thumbnail(out.toByteArray(), THUMBNAIL_CONTENT_TYPE, targetWidth, targetHeight);
    }
}
