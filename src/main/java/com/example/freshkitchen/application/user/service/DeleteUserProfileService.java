package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessException;
import com.example.freshkitchen.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteUserProfileService implements DeleteUserProfileUseCase {

    private final UserRepository userRepository;

    @Override
    public void delete(Command command) {
        Long userId = requireUserId(command);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.removeProfile();
    }

    private static Long requireUserId(Command command) {
        if (command == null || command.userId() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return command.userId();
    }
}