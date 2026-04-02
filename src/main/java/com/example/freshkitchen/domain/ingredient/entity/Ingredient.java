package com.example.freshkitchen.domain.ingredient.entity;

import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.image.entity.IngredientImage;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "INGREDIENT")
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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "registrated_at")
    private LocalDate registeredAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_source_type", nullable = false)
    private ExpirySourceType expirySourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IngredientStatus status;

    @Column(name = "consumed_at")
    private LocalDate consumedAt;

    @Column(name = "discarded_at")
    private LocalDate discardedAt;

    @Column(name = "note")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private IngredientSourceType sourceType;

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
        this.storage = requireNonNull(storage, "storage");
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
            this.storage = requireNonNull(command.storage(), "storage");
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
        if (command.status() != null) {
            changeStatus(command.status(), command.consumedAt(), command.discardedAt());
        } else {
            if (command.consumedAtSet()) {
                this.consumedAt = command.consumedAt();
            }
            if (command.discardedAtSet()) {
                this.discardedAt = command.discardedAt();
            }
        }
    }

    public void addImage(IngredientImage ingredientImage) {
        requireNonNull(ingredientImage, "ingredientImage");
        ingredientImage.attachIngredient(this);
        this.ingredientImages.add(ingredientImage);
        if (ingredientImage.isPrimary()) {
            enforcePrimaryImage(ingredientImage);
        }
    }

    public void enforcePrimaryImage(IngredientImage primaryImage) {
        for (IngredientImage ingredientImage : ingredientImages) {
            ingredientImage.forcePrimary(ingredientImage == primaryImage);
        }
    }

    private void changeStatus(IngredientStatus nextStatus, LocalDate consumedAt, LocalDate discardedAt) {
        this.status = requireNonNull(nextStatus, "status");
        switch (nextStatus) {
            case ACTIVE -> {
                this.consumedAt = null;
                this.discardedAt = null;
            }
            case CONSUMED -> {
                this.consumedAt = consumedAt != null ? consumedAt : LocalDate.now();
                this.discardedAt = null;
            }
            case DISCARDED -> {
                this.discardedAt = discardedAt != null ? discardedAt : LocalDate.now();
                this.consumedAt = null;
            }
        }
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
            IngredientStatus status,
            LocalDate consumedAt,
            boolean consumedAtSet,
            LocalDate discardedAt,
            boolean discardedAtSet,
            String note,
            boolean noteSet,
            IngredientSourceType sourceType
    ) {
    }
}
