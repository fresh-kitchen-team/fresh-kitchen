package com.example.freshkitchen.domain.chat.entity;

import com.example.freshkitchen.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "provide_extra_info", nullable = false)
    private boolean provideExtraInfo;

    @Column(name = "priority_expiration", nullable = false)
    private boolean priorityExpiration;

    @Column(name = "priority_nutrition", nullable = false)
    private boolean priorityNutrition;

    @Column(name = "priority_frequent", nullable = false)
    private boolean priorityFrequent;

    @Column(name = "notify_recipe_complete", nullable = false)
    private boolean notifyRecipeComplete;

    @Column(name = "notify_ai_recommend", nullable = false)
    private boolean notifyAiRecommend;

    @Column(name = "response_style")
    private String responseStyle;

    @Column(name = "include_image", nullable = false)
    private boolean includeImage;

    private AiSetting(User user) {
        this.user = user;
    }

    public static AiSetting createDefault(User user) {
        return new AiSetting(user);
    }
}
