package com.example.freshkitchen.application.user.usecase;

import com.example.freshkitchen.domain.user.enums.DeviceType;

public interface RegisterFcmTokenUseCase {

    void register(Command command);

    record Command(
            Long userId,
            String tokenValue,
            DeviceType deviceType
    ) {
    }
}