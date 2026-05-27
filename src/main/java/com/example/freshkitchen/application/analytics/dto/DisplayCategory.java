package com.example.freshkitchen.application.analytics.dto;

import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;

import java.util.List;
import java.util.Map;

/**
 * CatalogCategory(8종)를 프론트 표시용(4종)으로 그룹핑한다.
 */
public enum DisplayCategory {

    VEGETABLE_FRUIT("채소/과일", List.of(CatalogCategory.VEGETABLE, CatalogCategory.FRUIT)),
    DAIRY_DRINK("유제품", List.of(CatalogCategory.DAIRY, CatalogCategory.DRINK)),
    MEAT_SEAFOOD("육류/수산물", List.of(CatalogCategory.MEAT, CatalogCategory.SEAFOOD)),
    ETC("기타", List.of(CatalogCategory.SAUCE, CatalogCategory.ETC));

    private final String displayName;
    private final List<CatalogCategory> catalogCategories;

    private static final Map<CatalogCategory, DisplayCategory> LOOKUP;

    static {
        var builder = new java.util.HashMap<CatalogCategory, DisplayCategory>();
        for (DisplayCategory dc : values()) {
            for (CatalogCategory cc : dc.catalogCategories) {
                builder.put(cc, dc);
            }
        }
        LOOKUP = Map.copyOf(builder);
    }

    DisplayCategory(String displayName, List<CatalogCategory> catalogCategories) {
        this.displayName = displayName;
        this.catalogCategories = catalogCategories;
    }

    public String displayName() {
        return displayName;
    }

    public List<CatalogCategory> catalogCategories() {
        return catalogCategories;
    }

    public static DisplayCategory from(CatalogCategory catalogCategory) {
        if (catalogCategory == null) {
            return ETC;
        }
        return LOOKUP.getOrDefault(catalogCategory, ETC);
    }
}
