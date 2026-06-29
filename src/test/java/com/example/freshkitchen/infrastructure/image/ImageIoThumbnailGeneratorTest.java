package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.ThumbnailImageGenerator;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageIoThumbnailGeneratorTest {

    private final ImageIoThumbnailGenerator generator = new ImageIoThumbnailGenerator();

    @Test
    void generate_downscalesLongEdgeToMaxDimension() throws IOException {
        byte[] source = pngImage(800, 400);

        Optional<ThumbnailImageGenerator.Thumbnail> result = generator.generate(source, 320);

        assertTrue(result.isPresent());
        ThumbnailImageGenerator.Thumbnail thumbnail = result.get();
        assertAll(
                () -> assertEquals(320, thumbnail.width()),
                () -> assertEquals(160, thumbnail.height()),
                () -> assertEquals("image/jpeg", thumbnail.contentType())
        );
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(thumbnail.content()));
        assertNotNull(decoded);
        assertEquals(320, decoded.getWidth());
        assertEquals(160, decoded.getHeight());
    }

    @Test
    void generate_doesNotUpscaleSmallImage() throws IOException {
        byte[] source = pngImage(100, 80);

        Optional<ThumbnailImageGenerator.Thumbnail> result = generator.generate(source, 320);

        assertTrue(result.isPresent());
        assertAll(
                () -> assertEquals(100, result.get().width()),
                () -> assertEquals(80, result.get().height())
        );
    }

    @Test
    void generate_returnsEmptyForNonImageBytes() {
        Optional<ThumbnailImageGenerator.Thumbnail> result = generator.generate("not an image".getBytes(), 320);

        assertTrue(result.isEmpty());
    }

    @Test
    void generate_returnsEmptyWhenSourceExceedsMaxPixels() throws IOException {
        // 100x100 = 10,000 픽셀, 한도를 9,999로 낮춰 디코딩 전 헤더 검사로 건너뛰는지 검증
        ImageIoThumbnailGenerator guarded = new ImageIoThumbnailGenerator(9_999L);
        byte[] source = pngImage(100, 100);

        Optional<ThumbnailImageGenerator.Thumbnail> result = guarded.generate(source, 320);

        assertTrue(result.isEmpty());
    }

    @Test
    void generate_returnsEmptyForNullOrEmptySource() {
        assertAll(
                () -> assertTrue(generator.generate(null, 320).isEmpty()),
                () -> assertTrue(generator.generate(new byte[0], 320).isEmpty())
        );
    }

    private static byte[] pngImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
