package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.dto.UserProfileResult;
import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessException;
import com.example.freshkitchen.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserProfileService implements GetUserProfileUseCase {

    private final UserRepository userRepository;

    @Override
    public UserProfileResult get(Query query) {
        Long userId = requireUserId(query);
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResult.from(user);
    }

    private static Long requireUserId(Query query) {
        if (query == null || query.userId() == null) {
            throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
        }
        return query.userId();
    }
}