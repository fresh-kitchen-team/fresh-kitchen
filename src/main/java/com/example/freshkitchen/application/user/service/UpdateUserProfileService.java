package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserProfileService implements UpdateUserProfileUseCase { // update가 create-or-update 방식으로 동작

    private final UserRepository userRepository;

    @Override
    public void update(Command command) {
        User user = userRepository.findByIdWithProfile(command.userId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND)); // 유저 없으면 exception 던지기

        if (user.getProfile() == null) { // 유저 있는데 profile == null이면 새 UserProfile 붙이기
            UserProfile.create(new UserProfile.CreateCommand(
                    user,
                    command.nickname(),
                    command.profileImageUrlSet() ? command.profileImageUrl() : null, // "유지 vs null로 지우기" 구분하기 위한 플래그
                    command.bioSet() ? command.bio() : null,
                    emptyIfNull(command.preferredIngredients()),
                    emptyIfNull(command.foodStyles()),
                    emptyIfNull(command.allergies()),
                    emptyIfNull(command.cookingTools())
            ));
            return;
        }

        user.getProfile().apply(new UserProfile.UpdateCommand( // 이미 profile 있다면 apply로 수정하기
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
