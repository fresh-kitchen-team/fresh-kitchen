package com.example.freshkitchen.infrastructure.fcm;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

        // data-only 메시지로 전송한다. notification 필드를 포함하면 앱이 백그라운드/종료 상태일 때
        // 시스템이 알림을 자동 표시하고 onMessageReceived가 호출되지 않아, 클라이언트의 타입별 on/off
        // 설정이 무시된다. title/body도 data에 담아 클라이언트가 직접 알림을 빌드하도록 한다.
        Map<String, String> payload = new HashMap<>();
        if (data != null) {
            payload.putAll(data);
        }
        if (title != null) {
            payload.put("title", title);
        }
        if (body != null) {
            payload.put("body", body);
        }

        // data-only 메시지는 기본 우선순위가 낮아 도즈/백그라운드에서 지연될 수 있으므로 HIGH로 설정한다.
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();

        Set<String> invalidTokens = new HashSet<>();
        int successCount = 0;
        int failureCount = 0;

        for (int start = 0; start < tokenList.size(); start += BATCH_LIMIT) {
            List<String> chunk = tokenList.subList(start, Math.min(start + BATCH_LIMIT, tokenList.size()));
            List<Message> messages = chunk.stream()
                    .map(token -> Message.builder()
                            .setToken(token)
                            .putAllData(payload)
                            .setAndroidConfig(androidConfig)
                            .build())
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