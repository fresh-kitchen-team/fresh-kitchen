package com.example.freshkitchen.domain.tips.repository;

import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.tips.entity.StorageTip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageTipRepository extends JpaRepository<StorageTip, Long> {

    List<StorageTip> findByCategory(CatalogCategory category);
}
