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

    // 추가 영양 정보
    @Column(name = "provide_extra_info", nullable = false)
    private boolean provideExtraInfo;

    // 유통기한 우선 추천
    @Column(name = "priority_expiration", nullable = false)
    private boolean priorityExpiration;

    //영양 균형 추천
    @Column(name = "priority_nutrition", nullable = false)
    private boolean priorityNutrition;

    //자주 사용하는 재료 우선
    @Column(name = "priority_frequent", nullable = false)
    private boolean priorityFrequent;




    @Column(name = "response_style")
    private String responseStyle;



    private AiSetting(User user) {
        this.user = user;
    }

    public static AiSetting createDefault(User user) {
        return new AiSetting(user);
    }

    public void update(boolean provideExtraInfo,
                       boolean priorityExpiration,
                       boolean priorityNutrition,
                       boolean priorityFrequent,
                       String responseStyle) {
        this.provideExtraInfo = provideExtraInfo;
        this.priorityExpiration = priorityExpiration;
        this.priorityNutrition = priorityNutrition;
        this.priorityFrequent = priorityFrequent;
        this.responseStyle = responseStyle;
    }
}
