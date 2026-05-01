package com.example.freshkitchen.domain.image.entity;

import com.example.freshkitchen.domain.common.entity.CreatedAtEntity;
import com.example.freshkitchen.domain.image.enums.ImageVariantType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "image_variant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageVariant extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_asset_id", nullable = false)
    private ImageAsset imageAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", nullable = false, length = 20)
    private ImageVariantType variantType;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    private ImageVariant(
            ImageAsset imageAsset,
            ImageVariantType variantType,
            String imageUrl,
            int width,
            int height
    ) {
        this.imageAsset = requireNonNull(imageAsset, "imageAsset");
        this.variantType = requireNonNull(variantType, "variantType");
        this.imageUrl = requireNonBlank(imageUrl, "imageUrl");
        this.width = requirePositive(width, "width");
        this.height = requirePositive(height, "height");
    }

    public static ImageVariant create(CreateCommand command) {
        requireNonNull(command, "command");
        return new ImageVariant(
                command.imageAsset(),
                command.variantType(),
                command.imageUrl(),
                command.width(),
                command.height()
        );
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.imageAsset() != null) {
            this.imageAsset = requireNonNull(command.imageAsset(), "imageAsset");
        }
        if (command.variantType() != null) {
            this.variantType = requireNonNull(command.variantType(), "variantType");
        }
        if (command.imageUrl() != null) {
            this.imageUrl = requireNonBlank(command.imageUrl(), "imageUrl");
        }
        if (command.width() != null) {
            this.width = requirePositive(command.width(), "width");
        }
        if (command.height() != null) {
            this.height = requirePositive(command.height(), "height");
        }
    }

    public record CreateCommand(
            ImageAsset imageAsset,
            ImageVariantType variantType,
            String imageUrl,
            int width,
            int height
    ) {
    }

    public record UpdateCommand(
            ImageAsset imageAsset,
            ImageVariantType variantType,
            String imageUrl,
            Integer width,
            Integer height
    ) {
    }
}
