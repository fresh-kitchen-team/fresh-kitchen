package com.example.freshkitchen.domain.user.entity;

import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.user.enums.DeviceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_fcm_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_fcm_token_token_value",
                columnNames = "token_value"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFcmToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_value", nullable = false, length = 512)
    private String tokenValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    private UserFcmToken(User user, String tokenValue, DeviceType deviceType) {
        this.user = requireNonNull(user, "user");
        this.tokenValue = requireNonBlank(tokenValue, "tokenValue");
        this.deviceType = requireNonNull(deviceType, "deviceType");
    }

    public static UserFcmToken create(CreateCommand command) {
        requireNonNull(command, "command");
        return new UserFcmToken(command.user(), command.tokenValue(), command.deviceType());
    }

    public void reassign(User user, DeviceType deviceType) {
        this.user = requireNonNull(user, "user");
        this.deviceType = requireNonNull(deviceType, "deviceType");
    }

    public record CreateCommand(
            User user,
            String tokenValue,
            DeviceType deviceType
    ) {
    }
}