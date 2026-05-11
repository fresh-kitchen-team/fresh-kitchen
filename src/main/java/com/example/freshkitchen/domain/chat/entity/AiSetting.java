package com.example.freshkitchen.domain.chat.entity;

import com.example.freshkitchen.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class AiSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // AI 기능: 추가 정보 제공 여부
    private boolean provideExtraInfo;

    // 추천 기준 개별 처리
    private boolean priorityExpiration;      // 유통기한 우선 추천

    private boolean priorityNutrition;       // 영양 균형 기반 추천

    private boolean priorityFrequent;        // 자주 사용하는 재료 우선
    // 알림 설정
    private boolean notifyRecipeComplete;

    private boolean notifyAiRecommend;

    // 응답 스타일 (친절, 간단)
    private String responseStyle;

    // 이미지 포함 여부
    private boolean includeImage;
}