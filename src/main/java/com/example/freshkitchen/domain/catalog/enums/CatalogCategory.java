package com.example.freshkitchen.domain.catalog.enums;

public enum CatalogCategory {
    VEGETABLE("🥬"),
    FRUIT("🍎"),
    MEAT("🥩"),
    SEAFOOD("🐟"),
    DAIRY("🥛"),
    SAUCE("🧂"),
    DRINK("🥤"),
    GRAIN("🌾"),
    ETC("🍽️");

    private final String emoji;

    CatalogCategory(String emoji) {
        this.emoji = emoji;
    }

    public String emoji() {
        return emoji;
    }
}
