package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Override
    public void update(Command command) {
        User user = userRepository.findByIdWithProfile(command.userId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (user.getProfile() == null) {
            UserProfile profile = UserProfile.create(new UserProfile.CreateCommand(
                    user,
                    command.nickname(),
                    command.profileImageUrlSet() ? command.profileImageUrl() : null,
                    command.bioSet() ? command.bio() : null,
                    emptyIfNull(command.preferredIngredients()),
                    emptyIfNull(command.foodStyles()),
                    emptyIfNull(command.allergies()),
                    emptyIfNull(command.cookingTools())
            ));
            entityManager.persist(profile);
            return;
        }

        user.getProfile().apply(new UserProfile.UpdateCommand(
                command.nickname(),
                command.profileImageUrl(),
                command.profileImageUrlSet(),
                command.bio(),
                command.bioSet(),
                command.preferredIngredients(),
                command.foodStyles(),
                command.allergies(),
                command.cookingTools()
        ));
    }

    private static <T> Set<T> emptyIfNull(Set<T> values) {
        return values != null ? values : Set.of();
    }
}
