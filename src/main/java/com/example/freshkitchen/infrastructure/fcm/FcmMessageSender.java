package com.example.freshkitchen.infrastructure.fcm;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class FcmMessageSender {

    private static final int BATCH_LIMIT = 500;

    public SendResult sendToTokens(
            Collection<String> tokens,
            String title,
            String body,
            Map<String, String> data
    ) {
        if (tokens == null || tokens.isEmpty()) {
            return SendResult.empty();
        }

        List<String> tokenList = new ArrayList<>(new HashSet<>(tokens));
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Set<String> invalidTokens = new HashSet<>();
        int successCount = 0;
        int failureCount = 0;

        for (int start = 0; start < tokenList.size(); start += BATCH_LIMIT) {
            List<String> chunk = tokenList.subList(start, Math.min(start + BATCH_LIMIT, tokenList.size()));
            List<Message> messages = chunk.stream()
                    .map(token -> {
                        Message.Builder builder = Message.builder()
                                .setToken(token)
                                .setNotification(notification);
                        if (data != null && !data.isEmpty()) {
                            builder.putAllData(data);
                        }
                        return builder.build();
                    })
                    .toList();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);
                successCount += response.getSuccessCount();
                failureCount += response.getFailureCount();
                collectInvalidTokens(response.getResponses(), chunk, invalidTokens);
            } catch (FirebaseMessagingException e) {
                failureCount += chunk.size();
                log.error("FCM batch send failed: code={}, message={}", e.getErrorCode(), e.getMessage(), e);
            }
        }

        return new SendResult(successCount, failureCount, invalidTokens);
    }

    private static void collectInvalidTokens(
            List<SendResponse> responses,
            List<String> tokensInOrder,
            Set<String> invalidTokens
    ) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }
            FirebaseMessagingException exception = response.getException();
            if (isInvalidTokenError(exception)) {
                invalidTokens.add(tokensInOrder.get(i));
            } else {
                log.warn("FCM send failed for token: code={}, message={}",
                        exception != null ? exception.getErrorCode() : null,
                        exception != null ? exception.getMessage() : null);
            }
        }
    }

    private static boolean isInvalidTokenError(FirebaseMessagingException exception) {
        if (exception == null) {
            return false;
        }
        MessagingErrorCode messagingErrorCode = exception.getMessagingErrorCode();
        if (messagingErrorCode == MessagingErrorCode.UNREGISTERED
                || messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT
                || messagingErrorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
            return true;
        }
        return exception.getErrorCode() == ErrorCode.INVALID_ARGUMENT;
    }

    public record SendResult(int successCount, int failureCount, Set<String> invalidTokens) {

        public static SendResult empty() {
            return new SendResult(0, 0, Collections.emptySet());
        }
    }
}