package com.example.freshkitchen.domain.user.entity;

import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "user_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "user_profile_preferred_ingredient", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "ingredient_name", nullable = false, length = 100)
    private Set<String> preferredIngredients = new LinkedHashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_profile_food_style", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "food_style", nullable = false, length = 50)
    private Set<FoodStyle> foodStyles = new LinkedHashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_profile_allergy", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "allergy_type", nullable = false, length = 50)
    private Set<AllergyType> allergies = new LinkedHashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_profile_cooking_tool", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "cooking_tool", nullable = false, length = 50)
    private Set<CookingTool> cookingTools = new LinkedHashSet<>();

    private UserProfile(
            String nickname,
            String profileImageUrl,
            String bio,
            Set<String> preferredIngredients,
            Set<FoodStyle> foodStyles,
            Set<AllergyType> allergies,
            Set<CookingTool> cookingTools
    ) {
        this.nickname = requireNonBlank(nickname, "nickname");
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        replacePreferredIngredients(preferredIngredients);
        replaceFoodStyles(foodStyles);
        replaceAllergies(allergies);
        replaceCookingTools(cookingTools);
    }

    public static UserProfile create(CreateCommand command) {
        requireNonNull(command, "command");

        UserProfile profile = new UserProfile(
                command.nickname(),
                command.profileImageUrl(),
                command.bio(),
                command.preferredIngredients(),
                command.foodStyles(),
                command.allergies(),
                command.cookingTools()
        );

        User user = requireNonNull(command.user(), "user");
        user.assignProfile(profile); // profile 생성은 user aggregate에 연결되면서 완료
        return profile;
    }

    public void apply(UpdateCommand command) {
        requireNonNull(command, "command");

        if (command.nickname() != null) {
            this.nickname = requireNonBlank(command.nickname(), "nickname");
        }
        if (command.profileImageUrlSet()) {
            this.profileImageUrl = command.profileImageUrl();
        }
        if (command.bioSet()) {
            this.bio = command.bio();
        }
        if (command.preferredIngredients() != null) {
            replacePreferredIngredients(command.preferredIngredients());
        }
        if (command.foodStyles() != null) {
            replaceFoodStyles(command.foodStyles());
        }
        if (command.allergies() != null) {
            replaceAllergies(command.allergies());
        }
        if (command.cookingTools() != null) {
            replaceCookingTools(command.cookingTools());
        }
    }

    void attachUser(User user) {
        this.user = requireNonNull(user, "user");
    }

    private void replacePreferredIngredients(Set<String> preferredIngredients) {
        this.preferredIngredients.clear();
        toLinkedHashSet(preferredIngredients)
                .stream()
                .map(value -> requireNonBlank(value, "preferredIngredient"))
                .forEach(this.preferredIngredients::add);
    }

    private void replaceFoodStyles(Set<FoodStyle> foodStyles) {
        this.foodStyles.clear();
        toLinkedHashSet(foodStyles)
                .stream()
                .map(value -> requireNonNull(value, "foodStyle"))
                .forEach(this.foodStyles::add);
    }

    private void replaceAllergies(Set<AllergyType> allergies) {
        this.allergies.clear();
        toLinkedHashSet(allergies)
                .stream()
                .map(value -> requireNonNull(value, "allergy"))
                .forEach(this.allergies::add);
    }

    private void replaceCookingTools(Set<CookingTool> cookingTools) {
        this.cookingTools.clear();
        toLinkedHashSet(cookingTools)
                .stream()
                .map(value -> requireNonNull(value, "cookingTool"))
                .forEach(this.cookingTools::add);
    }

    public record CreateCommand(
            User user,
            String nickname,
            String profileImageUrl,
            String bio,
            Set<String> preferredIngredients,
            Set<FoodStyle> foodStyles,
            Set<AllergyType> allergies,
            Set<CookingTool> cookingTools
    ) {
    }

    public record UpdateCommand(
            String nickname,
            String profileImageUrl,
            boolean profileImageUrlSet,
            String bio,
            boolean bioSet,
            Set<String> preferredIngredients,
            Set<FoodStyle> foodStyles,
            Set<AllergyType> allergies,
            Set<CookingTool> cookingTools
    ) {
    }
}
