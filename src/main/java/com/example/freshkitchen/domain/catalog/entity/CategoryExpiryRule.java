package com.example.freshkitchen.domain.catalog.entity;

import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "category_expiry_rule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryExpiryRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private CatalogCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private StorageType storageType;

    @Column(name = "shelf_life_days", nullable = false)
    private int shelfLifeDays;

    @Column(name = "reference_note", columnDefinition = "TEXT")
    private String referenceNote;

    private CategoryExpiryRule(
            CatalogCategory category,
            StorageType storageType,
            int shelfLifeDays,
            String referenceNote
    ) {
        this.category = requireNonNull(category, "category");
        this.storageType = requireNonNull(storageType, "storageType");
        this.shelfLifeDays = requireNonNegative(shelfLifeDays, "shelfLifeDays");
        this.referenceNote = referenceNote;
    }

    public static CategoryExpiryRule create(CreateCommand command) {
        requireNonNull(command, "command");
        return new CategoryExpiryRule(
                command.category(),
                command.storageType(),
                command.shelfLifeDays(),
                command.referenceNote()
        );
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.category() != null) {
            this.category = requireNonNull(command.category(), "category");
        }
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
            CatalogCategory category,
            StorageType storageType,
            int shelfLifeDays,
            String referenceNote
    ) {
    }

    public record UpdateCommand(
            CatalogCategory category,
            StorageType storageType,
            Integer shelfLifeDays,
            String referenceNote,
            boolean referenceNoteSet
    ) {
    }
}
