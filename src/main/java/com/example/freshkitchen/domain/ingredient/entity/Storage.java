package com.example.freshkitchen.domain.ingredient.entity;

import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "STORAGE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Storage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false)
    private StorageType storageType;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "storage", fetch = FetchType.LAZY)
    private Set<Ingredient> ingredients = new LinkedHashSet<>();

    private Storage(User user, StorageType storageType, String name) {
        this.user = requireNonNull(user, "user");
        this.storageType = requireNonNull(storageType, "storageType");
        this.name = requireNonBlank(name, "name");
    }

    public static Storage create(CreateCommand command) {
        requireNonNull(command, "command");
        return new Storage(command.user(), command.storageType(), command.name());
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.storageType() != null) {
            this.storageType = requireNonNull(command.storageType(), "storageType");
        }
        if (command.name() != null) {
            this.name = requireNonBlank(command.name(), "name");
        }
    }

    public record CreateCommand(
            User user,
            StorageType storageType,
            String name
    ) {
    }

    public record UpdateCommand(
            StorageType storageType,
            String name
    ) {
    }
}
