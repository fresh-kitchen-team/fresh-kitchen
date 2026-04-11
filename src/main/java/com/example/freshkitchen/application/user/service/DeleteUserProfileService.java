package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
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
        Long userId = requireUserId(command);
        User user = userRepository.findById(userId) // delete는 profile 존재 여부만 확인하면 되므로 기본 조회로 충분
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND)); // 유저 없으면

        user.removeProfile(); // profile이 없으면 no-op, 있으면 orphanRemoval로 삭제
    }

    private static Long requireUserId(Command command) {
        if (command == null || command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        return command.userId();
    }
}
