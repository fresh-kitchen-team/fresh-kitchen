package com.example.freshkitchen.application.alarm.service;

import com.example.freshkitchen.application.alarm.config.ExpiringIngredientNotificationProperties;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.UserFcmToken;
import com.example.freshkitchen.domain.user.repository.UserFcmTokenRepository;
import com.example.freshkitchen.infrastructure.fcm.FcmMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiringIngredientNotificationService {

    private static final String NOTIFICATION_TITLE = "유통기한 임박 알림";
    private static final String DATA_KEY_TYPE = "type";
    private static final String DATA_TYPE_VALUE = "INGREDIENT_EXPIRING";
    private static final String DATA_KEY_INGREDIENT_IDS = "ingredientIds";
    private static final String DATA_KEY_AS_OF = "asOf";

    private final IngredientRepository ingredientRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final FcmMessageSender fcmMessageSender;
    private final ExpiringIngredientNotificationProperties properties;

    @Transactional
    public SendSummary notifyExpiring() {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(properties.getDaysAhead());

        List<Ingredient> ingredients = ingredientRepository.findExpiringWithUser(
                IngredientStatus.ACTIVE, today, until
        );
        if (ingredients.isEmpty()) {
            log.info("[ExpiringNotification] no expiring ingredients between {} ~ {}", today, until);
            return SendSummary.empty(today, until);
        }

        Map<Long, List<Ingredient>> ingredientsByUser = ingredients.stream()
                .collect(Collectors.groupingBy(
                        ingredient -> ingredient.getUser().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<Long, List<String>> tokensByUser = userFcmTokenRepository
                .findByUser_IdIn(ingredientsByUser.keySet())
                .stream()
                .collect(Collectors.groupingBy(
                        token -> token.getUser().getId(),
                        Collectors.mapping(UserFcmToken::getTokenValue, Collectors.toList())
                ));

        Set<String> invalidTokens = new HashSet<>();
        int totalSuccess = 0;
        int totalFailure = 0;
        int usersWithoutTokens = 0;
        List<UserMessage> perUserMessages = new ArrayList<>();

        for (Map.Entry<Long, List<Ingredient>> entry : ingredientsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<Ingredient> userIngredients = entry.getValue().stream()
                    .sorted(Comparator.comparing(Ingredient::getExpiresAt))
                    .toList();
            String body = buildBody(userIngredients, today);
            List<IngredientPreview> previews = userIngredients.stream()
                    .map(ingredient -> new IngredientPreview(
                            ingredient.getId(),
                            ingredient.getName(),
                            ingredient.getExpiresAt(),
                            ChronoUnit.DAYS.between(today, ingredient.getExpiresAt())
                    ))
                    .toList();

            List<String> userTokens = tokensByUser.get(userId);
            if (userTokens == null || userTokens.isEmpty()) {
                usersWithoutTokens++;
                perUserMessages.add(new UserMessage(userId, NOTIFICATION_TITLE, body, 0, previews, "SKIPPED_NO_TOKEN"));
                continue;
            }

            Map<String, String> data = buildData(userIngredients, today);
            FcmMessageSender.SendResult result = fcmMessageSender.sendToTokens(
                    userTokens, NOTIFICATION_TITLE, body, data
            );
            totalSuccess += result.successCount();
            totalFailure += result.failureCount();
            invalidTokens.addAll(result.invalidTokens());

            String status = (result.successCount() > 0) ? "SENT" : "FAILED";
            perUserMessages.add(new UserMessage(userId, NOTIFICATION_TITLE, body, userTokens.size(), previews, status));
        }

        log.info("[ExpiringNotification] users={}, usersWithoutTokens={}, success={}, failure={}, invalid={}",
                ingredientsByUser.size(), usersWithoutTokens, totalSuccess, totalFailure, invalidTokens.size());

        if (!invalidTokens.isEmpty()) {
            userFcmTokenRepository.deleteByTokenValueIn(invalidTokens);
        }

        return new SendSummary(
                today, until,
                ingredientsByUser.size(),
                usersWithoutTokens,
                totalSuccess,
                totalFailure,
                invalidTokens.size(),
                perUserMessages
        );
    }

    private static String buildBody(List<Ingredient> sortedIngredients, LocalDate today) {
        Ingredient nearest = sortedIngredients.get(0);
        long daysLeft = ChronoUnit.DAYS.between(today, nearest.getExpiresAt());
        String daysPhrase = formatDaysLeft(daysLeft);
        int count = sortedIngredients.size();

        if (count == 1) {
            return String.format("'%s'의 유통기한이 %s.", nearest.getName(), daysPhrase);
        }
        return String.format("'%s'의 유통기한이 %s. (외 %d개 식재료)",
                nearest.getName(), daysPhrase, count - 1);
    }

    private static String formatDaysLeft(long daysLeft) {
        if (daysLeft <= 0) {
            return "오늘이에요";
        }
        if (daysLeft == 1) {
            return "내일이에요";
        }
        return daysLeft + "일 남았어요";
    }

    private static Map<String, String> buildData(List<Ingredient> sortedIngredients, LocalDate today) {
        String ingredientIds = sortedIngredients.stream()
                .map(ingredient -> String.valueOf(ingredient.getId()))
                .collect(Collectors.joining(","));
        return Map.of(
                DATA_KEY_TYPE, DATA_TYPE_VALUE,
                DATA_KEY_INGREDIENT_IDS, ingredientIds,
                DATA_KEY_AS_OF, today.toString()
        );
    }

    public record SendSummary(
            LocalDate from,
            LocalDate until,
            int users,
            int usersWithoutTokens,
            int success,
            int failure,
            int invalid,
            List<UserMessage> messages
    ) {
        public static SendSummary empty(LocalDate from, LocalDate until) {
            return new SendSummary(from, until, 0, 0, 0, 0, 0, List.of());
        }
    }

    public record UserMessage(
            Long userId,
            String title,
            String body,
            int tokenCount,
            List<IngredientPreview> ingredients,
            String status
    ) {
    }

    public record IngredientPreview(
            Long ingredientId,
            String name,
            LocalDate expiresAt,
            long daysLeft
    ) {
    }
}
