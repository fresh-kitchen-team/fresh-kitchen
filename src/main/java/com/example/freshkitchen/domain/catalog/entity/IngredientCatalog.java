package com.example.freshkitchen.domain.catalog.entity;

import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
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
@Table(name = "ingredient_catalog")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientCatalog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_image_asset_id")
    private ImageAsset defaultImageAsset;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private CatalogCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_storage_type", nullable = false, length = 20)
    private StorageType defaultStorageType;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String iconUrl;

    @OneToMany(mappedBy = "catalog", fetch = FetchType.LAZY)
    private Set<Ingredient> ingredients = new LinkedHashSet<>();

    @OneToMany(mappedBy = "catalog", fetch = FetchType.LAZY)
    private Set<CatalogExpiryRule> catalogExpiryRules = new LinkedHashSet<>();

    private IngredientCatalog(
            ImageAsset defaultImageAsset,
            String name,
            CatalogCategory category,
            StorageType defaultStorageType,
            String iconUrl
    ) {
        this.defaultImageAsset = defaultImageAsset;
        this.name = requireNonBlank(name, "name");
        this.category = requireNonNull(category, "category");
        this.defaultStorageType = requireNonNull(defaultStorageType, "defaultStorageType");
        this.iconUrl = iconUrl;
    }

    public static IngredientCatalog create(CreateCommand command) {
        requireNonNull(command, "command");
        return new IngredientCatalog(
                command.defaultImageAsset(),
                command.name(),
                command.category(),
                command.defaultStorageType(),
                command.iconUrl()
        );
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.defaultImageAssetSet()) {
            this.defaultImageAsset = command.defaultImageAsset();
        }
        if (command.name() != null) {
            this.name = requireNonBlank(command.name(), "name");
        }
        if (command.category() != null) {
            this.category = requireNonNull(command.category(), "category");
        }
        if (command.defaultStorageType() != null) {
            this.defaultStorageType = requireNonNull(command.defaultStorageType(), "defaultStorageType");
        }
        if (command.iconUrlSet()) {
            this.iconUrl = command.iconUrl();
        }
    }

    public record CreateCommand(
            ImageAsset defaultImageAsset,
            String name,
            CatalogCategory category,
            StorageType defaultStorageType,
            String iconUrl
    ) {
    }

    public record UpdateCommand(
            ImageAsset defaultImageAsset,
            boolean defaultImageAssetSet,
            String name,
            CatalogCategory category,
            StorageType defaultStorageType,
            String iconUrl,
            boolean iconUrlSet
    ) {
    }
}
