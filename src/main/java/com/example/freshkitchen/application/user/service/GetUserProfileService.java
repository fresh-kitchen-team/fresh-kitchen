package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.dto.UserProfileResult;
import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
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
        User user = userRepository.findByIdWithProfile(query.userId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return UserProfileResult.from(user);
    }
}
