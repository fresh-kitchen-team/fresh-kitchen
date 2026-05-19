package com.example.freshkitchen.domain.tips.repository;

import com.example.freshkitchen.domain.tips.entity.StorageTip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageTipRepository extends JpaRepository<StorageTip, Long> {
}
