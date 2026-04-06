package com.example.freshkitchen.domain.ingredient.entity;

import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "ingredient")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "storage_id", nullable = false)
    private Storage storage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id")
    private IngredientCatalog catalog;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "registered_at")
    private LocalDate registeredAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_source_type", nullable = false, length = 20)
    private ExpirySourceType expirySourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IngredientStatus status;

    @Column(name = "consumed_at")
    private LocalDate consumedAt;

    @Column(name = "discarded_at")
    private LocalDate discardedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private IngredientSourceType sourceType;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "ingredient", fetch = FetchType.LAZY)
    private Set<IngredientImage> ingredientImages = new LinkedHashSet<>();

    private Ingredient(
            User user,
            Storage storage,
            IngredientCatalog catalog,
            String name,
            LocalDate registeredAt,
            LocalDate expiresAt,
            ExpirySourceType expirySourceType,
            String note,
            IngredientSourceType sourceType
    ) {
        this.user = requireNonNull(user, "user");
        this.storage = requireOwnedStorage(user, storage);
        this.catalog = catalog;
        this.name = requireNonBlank(name, "name");
        this.registeredAt = registeredAt;
        this.expiresAt = expiresAt;
        this.expirySourceType = requireNonNull(expirySourceType, "expirySourceType");
        this.note = note;
        this.sourceType = requireNonNull(sourceType, "sourceType");
        this.status = IngredientStatus.ACTIVE;
    }

    public static Ingredient create(CreateCommand command) {
        requireNonNull(command, "command");
        return new Ingredient(
                command.user(),
                command.storage(),
                command.catalog(),
                command.name(),
                command.registeredAt(),
                command.expiresAt(),
                command.expirySourceType(),
                command.note(),
                command.sourceType()
        );
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.storage() != null) {
            this.storage = requireOwnedStorage(this.user, command.storage());
        }
        if (command.catalogSet()) {
            this.catalog = command.catalog();
        }
        if (command.name() != null) {
            this.name = requireNonBlank(command.name(), "name");
        }
        if (command.registeredAtSet()) {
            this.registeredAt = command.registeredAt();
        }
        if (command.expiresAtSet()) {
            this.expiresAt = command.expiresAt();
        }
        if (command.expirySourceType() != null) {
            this.expirySourceType = requireNonNull(command.expirySourceType(), "expirySourceType");
        }
        if (command.noteSet()) {
            this.note = command.note();
        }
        if (command.sourceType() != null) {
            this.sourceType = requireNonNull(command.sourceType(), "sourceType");
        }
    }

    public void markConsumed(LocalDate consumedAt) {
        this.status = IngredientStatus.CONSUMED;
        this.consumedAt = consumedAt != null ? consumedAt : LocalDate.now();
        this.discardedAt = null;
    }

    public void markDiscarded(LocalDate discardedAt) {
        this.status = IngredientStatus.DISCARDED;
        this.discardedAt = discardedAt != null ? discardedAt : LocalDate.now();
        this.consumedAt = null;
    }

    public void addImage(IngredientImage ingredientImage) {
        requireNonNull(ingredientImage, "ingredientImage");
        if (ingredientImages.isEmpty() && !ingredientImage.isPrimary()) {
            throw new IngredientException(IngredientErrorCode.FIRST_IMAGE_MUST_BE_PRIMARY);
        }
        ingredientImage.attachIngredient(this);
        this.ingredientImages.add(ingredientImage);
        if (ingredientImage.isPrimary()) {
            enforcePrimaryImage(ingredientImage);
        }
        ensurePrimaryImageInvariant();
    }

    public void enforcePrimaryImage(IngredientImage primaryImage) {
        requireNonNull(primaryImage, "primaryImage");
        if (!containsImage(primaryImage)) {
            throw new IngredientException(IngredientErrorCode.PRIMARY_IMAGE_MUST_BELONG_TO_INGREDIENT);
        }
        for (IngredientImage ingredientImage : ingredientImages) {
            ingredientImage.forcePrimary(sameEntity(ingredientImage, primaryImage, IngredientImage::getId));
        }
    }

    public void changeImagePrimary(IngredientImage ingredientImage, boolean primary) {
        requireNonNull(ingredientImage, "ingredientImage");
        if (!containsImage(ingredientImage)) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT);
        }

        if (primary) {
            enforcePrimaryImage(ingredientImage);
            return;
        }

        if (ingredientImage.isPrimary()) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_PRIMARY_IMAGE_REQUIRED);
        }

        ingredientImage.forcePrimary(false);
        ensurePrimaryImageInvariant();
    }

    private void ensurePrimaryImageInvariant() {
        if (ingredientImages.isEmpty()) {
            return;
        }

        long primaryCount = ingredientImages.stream()
                .filter(IngredientImage::isPrimary)
                .count();
        if (primaryCount != 1) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_PRIMARY_IMAGE_INVARIANT_BROKEN);
        }
    }

    private static Storage requireOwnedStorage(User user, Storage storage) {
        requireNonNull(user, "user");
        Storage validatedStorage = requireNonNull(storage, "storage");
        if (!sameEntity(validatedStorage.getUser(), user, User::getId)) {
            throw new IngredientException(IngredientErrorCode.STORAGE_NOT_OWNED_BY_USER);
        }
        return validatedStorage;
    }

    private boolean containsImage(IngredientImage candidate) {
        return ingredientImages.stream()
                .anyMatch(ingredientImage -> sameEntity(ingredientImage, candidate, IngredientImage::getId));
    }

    public record CreateCommand(
            User user,
            Storage storage,
            IngredientCatalog catalog,
            String name,
            LocalDate registeredAt,
            LocalDate expiresAt,
            ExpirySourceType expirySourceType,
            String note,
            IngredientSourceType sourceType
    ) {
    }

    public record UpdateCommand(
            Storage storage,
            IngredientCatalog catalog,
            boolean catalogSet,
            String name,
            LocalDate registeredAt,
            boolean registeredAtSet,
            LocalDate expiresAt,
            boolean expiresAtSet,
            ExpirySourceType expirySourceType,
            String note,
            boolean noteSet,
            IngredientSourceType sourceType
    ) {
    }
}
