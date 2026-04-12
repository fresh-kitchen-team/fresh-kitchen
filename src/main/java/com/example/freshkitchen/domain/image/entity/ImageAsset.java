package com.example.freshkitchen.domain.image.entity;

import com.example.freshkitchen.domain.common.entity.CreatedAtEntity;
import com.example.freshkitchen.domain.image.exception.ImageErrorCode;
import com.example.freshkitchen.domain.image.exception.ImageException;
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
@Table(name = "image_asset")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageAsset extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private ImageKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20)
    private StorageProvider storageProvider;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
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
        this.assetType = requireNonNull(assetType, "assetType");
        this.user = validateOwnerConsistency(user, this.assetType);
        this.kind = requireNonNull(kind, "kind");
        this.storageProvider = requireNonNull(storageProvider, "storageProvider");
        this.imageUrl = requireNonBlank(imageUrl, "imageUrl");
        this.width = requirePositiveNullable(width, "width");
        this.height = requirePositiveNullable(height, "height");
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

        User nextUser = this.user;
        AssetType nextAssetType = this.assetType;

        if (command.userSet()) {
            nextUser = command.user();
        }
        if (command.assetType() != null) {
            nextAssetType = requireNonNull(command.assetType(), "assetType");
        }
        this.assetType = nextAssetType;
        this.user = validateOwnerConsistency(nextUser, nextAssetType);
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
            this.width = requirePositiveNullable(command.width(), "width");
        }
        if (command.heightSet()) {
            this.height = requirePositiveNullable(command.height(), "height");
        }
    }

    private static User validateOwnerConsistency(User user, AssetType assetType) {
        if (assetType == AssetType.SYSTEM_DEFAULT && user != null) {
            throw new ImageException(ImageErrorCode.SYSTEM_DEFAULT_OWNER_MUST_BE_NULL);
        }
        if (assetType == AssetType.USER_UPLOAD && user == null) {
            throw new ImageException(ImageErrorCode.USER_UPLOAD_OWNER_REQUIRED);
        }
        return user;
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
