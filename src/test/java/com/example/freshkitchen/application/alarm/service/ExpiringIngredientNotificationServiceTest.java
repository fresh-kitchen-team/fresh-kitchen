package com.example.freshkitchen.application.alarm.service;

import com.example.freshkitchen.application.alarm.config.ExpiringIngredientNotificationProperties;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.repository.UserFcmTokenRepository;
import com.example.freshkitchen.infrastructure.fcm.FcmMessageSender;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpiringIngredientNotificationServiceTest {

    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final UserFcmTokenRepository userFcmTokenRepository = mock(UserFcmTokenRepository.class);
    private final FcmMessageSender fcmMessageSender = mock(FcmMessageSender.class);
    private final ExpiringIngredientNotificationProperties properties = new ExpiringIngredientNotificationProperties();
    private final ExpiringIngredientNotificationService service = new ExpiringIngredientNotificationService(
            ingredientRepository,
            userFcmTokenRepository,
            fcmMessageSender,
            properties
    );

    @Test
    void notifyExpiring_queriesManualAndPolicyExpirySources() {
        properties.setDaysAhead(3);
        when(ingredientRepository.findExpiringWithUser(
                eq(IngredientStatus.ACTIVE),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(List.of(ExpirySourceType.MANUAL, ExpirySourceType.POLICY))
        )).thenReturn(List.of());

        service.notifyExpiring();

        verify(ingredientRepository).findExpiringWithUser(
                eq(IngredientStatus.ACTIVE),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(List.of(ExpirySourceType.MANUAL, ExpirySourceType.POLICY))
        );
    }
}
