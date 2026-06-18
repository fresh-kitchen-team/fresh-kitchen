package com.example.freshkitchen.domain.catalog.entity;

import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "ingredient_catalog_alias")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientCatalogAlias extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_id", nullable = false)
    private IngredientCatalog catalog;

    @Column(name = "alias_name", nullable = false, length = 100)
    private String aliasName;

    @Column(name = "normalized_alias_name", nullable = false, length = 100)
    private String normalizedAliasName;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    private IngredientCatalogAlias(
            IngredientCatalog catalog,
            String aliasName,
            String normalizedAliasName,
            String language
    ) {
        this.catalog = requireNonNull(catalog, "catalog");
        this.aliasName = requireNonBlank(aliasName, "aliasName");
        this.normalizedAliasName = requireNonBlank(normalizedAliasName, "normalizedAliasName");
        this.language = requireNonBlank(language, "language");
    }

    public static IngredientCatalogAlias create(CreateCommand command) {
        requireNonNull(command, "command");
        return new IngredientCatalogAlias(
                command.catalog(),
                command.aliasName(),
                command.normalizedAliasName(),
                command.language()
        );
    }

    public record CreateCommand(
            IngredientCatalog catalog,
            String aliasName,
            String normalizedAliasName,
            String language
    ) {
    }
}
