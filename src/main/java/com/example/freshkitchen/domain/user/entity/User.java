package com.example.freshkitchen.domain.user.entity;

import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.enums.UserStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_provider_provider_user_id",
                columnNames = {"provider", "provider_user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "inactive_at")
    private OffsetDateTime inactiveAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserProfile profile;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Storage> storages = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Ingredient> ingredients = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<ImageAsset> imageAssets = new LinkedHashSet<>();

    private User(String providerUserId, Provider provider) {
        this.providerUserId = requireNonBlank(providerUserId, "providerUserId");
        this.provider = requireNonNull(provider, "provider");
        this.status = UserStatus.ACTIVE;
    }

    public static User create(CreateCommand command) {
        requireNonNull(command, "command");
        return new User(command.providerUserId(), command.provider());
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.providerUserId() != null) {
            this.providerUserId = requireNonBlank(command.providerUserId(), "providerUserId");
        }
        if (command.status() != null) {
            changeStatus(command.status(), command.inactiveAt());
        }
    }

    public void assignProfile(UserProfile profile) {
        requireNonNull(profile, "profile");
        this.profile = profile;
        profile.attachUser(this);
    }

    public void removeProfile() { // orphanRemoval(부모와 연관관계가 끊긴 자식 엔티티를 JPA가 자동 삭제)이 동작하도록 profile 연결만 제거
        if (this.profile == null) {
            return;
        }

        this.profile = null;
    }

    private void changeStatus(UserStatus nextStatus, OffsetDateTime inactiveAt) {
        this.status = requireNonNull(nextStatus, "status");
        if (nextStatus == UserStatus.INACTIVE) {
            this.inactiveAt = inactiveAt != null ? inactiveAt : OffsetDateTime.now();
            return;
        }
        this.inactiveAt = null;
    }

    public record CreateCommand(
            String providerUserId,
            Provider provider
    ) {
    }

    public record UpdateCommand(
            String providerUserId,
            UserStatus status,
            OffsetDateTime inactiveAt
    ) {
    }
}
