package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.dto.UserProfileResult;
import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
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
@Transactional(readOnly = true)
public class GetUserProfileService implements GetUserProfileUseCase {

    private final UserRepository userRepository;

    @Override
    public UserProfileResult get(Query query) { // 유저는 반드시 찾아야 하며, 없으면 Exception
        Long userId = requireUserId(query);
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return UserProfileResult.from(user); // '유저는 있는데 프로필이 아직 없는 경우'는 서비스에서 직접 예외로 처리 안하며, dto단에서 결과 객체로 변환하도록(비어있는 결과로)
    }

    private static Long requireUserId(Query query) {
        if (query == null || query.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        return query.userId();
    }
}
