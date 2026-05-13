package com.example.freshkitchen.domain.chat.repository;

import com.example.freshkitchen.domain.chat.entity.AiSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiSettingRepository extends JpaRepository<AiSetting, Long> {
    Optional<AiSetting> findByUserId(Long userId);
}