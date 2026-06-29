package com.example.freshkitchen.domain.tips.entity;

import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
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
@Table(name = "storage_tip")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private CatalogCategory category;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "emoji", length = 10)
    private String emoji;

    @Column(name = "tip", nullable = false, columnDefinition = "TEXT")
    private String tip;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private StorageType storageType;
}
