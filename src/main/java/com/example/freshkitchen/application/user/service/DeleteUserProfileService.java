package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteUserProfileService implements DeleteUserProfileUseCase {
    // 여기서는 유저 계정 삭제가 아니라 UserProfile만 삭제합니다

    private final UserRepository userRepository;

    @Override
    public void delete(Command command) {
        User user = userRepository.findByIdWithProfile(command.userId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND)); // 유저 없으면

        if (user.getProfile() == null) {
            return; // 프로필이 없으면 아무것도 안함 -> 프로필이 있으면 다음으로 remove
        }

        user.removeProfile();
    }
}
