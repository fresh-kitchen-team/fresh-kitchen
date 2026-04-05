package com.example.freshkitchen.domain.image.entity;

import com.example.freshkitchen.domain.common.entity.CreatedAtEntity;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
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
@Table(name = "ingredient_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientImage extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_asset_id", nullable = false)
    private ImageAsset imageAsset;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private IngredientImageSourceType sourceType;

    private IngredientImage(
            Ingredient ingredient,
            ImageAsset imageAsset,
            boolean primary,
            IngredientImageSourceType sourceType
    ) {
        this.ingredient = requireNonNull(ingredient, "ingredient");
        this.imageAsset = requireNonNull(imageAsset, "imageAsset");
        this.primary = primary;
        this.sourceType = requireNonNull(sourceType, "sourceType");
    }

    public static IngredientImage create(CreateCommand command) {
        requireNonNull(command, "command");
        IngredientImage ingredientImage = new IngredientImage(
                command.ingredient(),
                command.imageAsset(),
                command.primary(),
                command.sourceType()
        );
        command.ingredient().addImage(ingredientImage);
        return ingredientImage;
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.imageAsset() != null) {
            this.imageAsset = requireNonNull(command.imageAsset(), "imageAsset");
        }
        if (command.primary() != null) {
            if (this.ingredient == null) {
                this.primary = command.primary();
            } else {
                this.ingredient.changeImagePrimary(this, command.primary());
            }
        }
        if (command.sourceType() != null) {
            this.sourceType = requireNonNull(command.sourceType(), "sourceType");
        }
    }

    public void attachIngredient(Ingredient ingredient) {
        Ingredient nextIngredient = requireNonNull(ingredient, "ingredient");
        if (this.ingredient == null) {
            this.ingredient = nextIngredient;
            return;
        }
        if (!sameEntity(this.ingredient, nextIngredient, Ingredient::getId)) {
            throw new IllegalArgumentException("ingredient image is already attached to another ingredient");
        }
    }

    public void forcePrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isPrimary() {
        return primary;
    }

    public record CreateCommand(
            Ingredient ingredient,
            ImageAsset imageAsset,
            boolean primary,
            IngredientImageSourceType sourceType
    ) {
    }

    public record UpdateCommand(
            ImageAsset imageAsset,
            Boolean primary,
            IngredientImageSourceType sourceType
    ) {
    }
}
