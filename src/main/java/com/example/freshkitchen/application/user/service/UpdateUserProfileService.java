package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
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
        Long userId = requireUserId(command);
        User user = userRepository.findById(userId) // update는 profile 존재 여부만 먼저 확인하고, 필요한 연관만 지연 로딩
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND)); // 유저 없으면 exception 던지기

        UserProfile profile = user.getProfile();
        if (profile == null) {    // 유저 있는데 profile == null이면 새 UserProfile 붙이기
            String nickname = requireNicknameForProfileCreation(command.nickname());
            UserProfile.create(new UserProfile.CreateCommand(
                    user,
                    nickname,
                    command.profileImageUrlSet() ? command.profileImageUrl() : null, // "유지 vs null로 지우기" 구분하기 위한 플래그
                    command.bioSet() ? command.bio() : null,
                    emptyIfNull(command.preferredIngredients()),
                    emptyIfNull(command.foodStyles()),
                    emptyIfNull(command.allergies()),
                    emptyIfNull(command.cookingTools())
            ));
            return;
        }

        profile.apply(new UserProfile.UpdateCommand( // 이미 profile 있다면 apply로 수정하기
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

    private static String requireNicknameForProfileCreation(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessValidationException("nickname must not be blank");
        }
        return nickname;
    }

    private static Long requireUserId(Command command) {
        if (command == null || command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        return command.userId();
    }
}
