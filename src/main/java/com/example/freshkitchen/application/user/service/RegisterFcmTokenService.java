package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.RegisterFcmTokenUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserFcmToken;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserFcmTokenRepository;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterFcmTokenService implements RegisterFcmTokenUseCase {

    private final UserRepository userRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;

    @Override
    public void register(Command command) {
        Long userId = requireUserId(command);
        String tokenValue = requireTokenValue(command);
        requireDeviceType(command);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        userFcmTokenRepository.findByTokenValue(tokenValue)
                .ifPresentOrElse( // 같은 토큰이 다른 사용자에 묶여 있을 수 있으므로 user/deviceType 재할당
                        existing -> existing.reassign(user, command.deviceType()),
                        () -> userFcmTokenRepository.save(UserFcmToken.create(
                                new UserFcmToken.CreateCommand(user, tokenValue, command.deviceType())
                        ))
                );
    }

    private static Long requireUserId(Command command) {
        if (command == null || command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        return command.userId();
    }

    private static String requireTokenValue(Command command) {
        if (command.tokenValue() == null || command.tokenValue().isBlank()) {
            throw new BusinessValidationException("tokenValue must not be blank");
        }
        return command.tokenValue();
    }

    private static void requireDeviceType(Command command) {
        if (command.deviceType() == null) {
            throw new BusinessValidationException("deviceType must not be null");
        }
    }
}
