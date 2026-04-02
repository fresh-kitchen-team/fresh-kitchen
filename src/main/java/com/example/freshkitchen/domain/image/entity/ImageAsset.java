package com.example.freshkitchen.domain.image.entity;

import com.example.freshkitchen.domain.common.entity.CreatedAtEntity;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.user.entity.User;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "IMAGE_ASSET")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageAsset extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private ImageKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false)
    private StorageProvider storageProvider;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @OneToMany(mappedBy = "imageAsset", fetch = FetchType.LAZY)
    private Set<ImageVariant> variants = new LinkedHashSet<>();

    @OneToMany(mappedBy = "imageAsset", fetch = FetchType.LAZY)
    private Set<IngredientImage> ingredientImages = new LinkedHashSet<>();

    private ImageAsset(
            User user,
            AssetType assetType,
            ImageKind kind,
            StorageProvider storageProvider,
            String imageUrl,
            Integer width,
            Integer height
    ) {
        this.user = user;
        this.assetType = requireNonNull(assetType, "assetType");
        this.kind = requireNonNull(kind, "kind");
        this.storageProvider = requireNonNull(storageProvider, "storageProvider");
        this.imageUrl = requireNonBlank(imageUrl, "imageUrl");
        this.width = width;
        this.height = height;
    }

    public static ImageAsset create(CreateCommand command) {
        requireNonNull(command, "command");
        return new ImageAsset(
                command.user(),
                command.assetType(),
                command.kind(),
                command.storageProvider(),
                command.imageUrl(),
                command.width(),
                command.height()
        );
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.userSet()) {
            this.user = command.user();
        }
        if (command.assetType() != null) {
            this.assetType = requireNonNull(command.assetType(), "assetType");
        }
        if (command.kind() != null) {
            this.kind = requireNonNull(command.kind(), "kind");
        }
        if (command.storageProvider() != null) {
            this.storageProvider = requireNonNull(command.storageProvider(), "storageProvider");
        }
        if (command.imageUrl() != null) {
            this.imageUrl = requireNonBlank(command.imageUrl(), "imageUrl");
        }
        if (command.widthSet()) {
            this.width = command.width();
        }
        if (command.heightSet()) {
            this.height = command.height();
        }
    }

    public record CreateCommand(
            User user,
            AssetType assetType,
            ImageKind kind,
            StorageProvider storageProvider,
            String imageUrl,
            Integer width,
            Integer height
    ) {
    }

    public record UpdateCommand(
            User user,
            boolean userSet,
            AssetType assetType,
            ImageKind kind,
            StorageProvider storageProvider,
            String imageUrl,
            Integer width,
            boolean widthSet,
            Integer height,
            boolean heightSet
    ) {
    }
}
