package com.example.freshkitchen.domain.catalog.entity;

import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "catalog_expiry_rule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatalogExpiryRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_id", nullable = false)
    private IngredientCatalog catalog;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private StorageType storageType;

    @Column(name = "shelf_life_days", nullable = false)
    private int shelfLifeDays;

    @Column(name = "reference_note", columnDefinition = "TEXT")
    private String referenceNote;

    private CatalogExpiryRule(
            IngredientCatalog catalog,
            StorageType storageType,
            int shelfLifeDays,
            String referenceNote
    ) {
        this.catalog = requireNonNull(catalog, "catalog");
        this.storageType = requireNonNull(storageType, "storageType");
        this.shelfLifeDays = requireNonNegative(shelfLifeDays, "shelfLifeDays");
        this.referenceNote = referenceNote;
    }

    public static CatalogExpiryRule create(CreateCommand command) {
        requireNonNull(command, "command");
        return new CatalogExpiryRule(
                command.catalog(),
                command.storageType(),
                command.shelfLifeDays(),
                command.referenceNote()
        );
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.storageType() != null) {
            this.storageType = requireNonNull(command.storageType(), "storageType");
        }
        if (command.shelfLifeDays() != null) {
            this.shelfLifeDays = requireNonNegative(command.shelfLifeDays(), "shelfLifeDays");
        }
        if (command.referenceNoteSet()) {
            this.referenceNote = command.referenceNote();
        }
    }

    public record CreateCommand(
            IngredientCatalog catalog,
            StorageType storageType,
            int shelfLifeDays,
            String referenceNote
    ) {
    }

    public record UpdateCommand(
            StorageType storageType,
            Integer shelfLifeDays,
            String referenceNote,
            boolean referenceNoteSet
    ) {
    }
}
