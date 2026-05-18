package com.example.freshkitchen.domain.chat.intent;

import com.example.freshkitchen.presentation.chat.dto.request.ChatType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class
IntentClassifier {

    private static final String SYSTEM_PROMPT = """
            너는 사용자의 채팅 메시지를 두 가지 카테고리 중 하나로 분류한다.
            - RECIPE: 요리/레시피/식재료 활용/보관법/조리법/식단/영양 관련 질문
            - GENERAL: 그 외 모든 일상 대화, 실시간 정보(날씨/뉴스/환율 등) 질문
            응답은 반드시 "RECIPE" 또는 "GENERAL" 한 단어로만 답하라. 다른 텍스트 금지.
            """;

    private static final Pattern INVENTORY_INTENT_PATTERN = Pattern.compile(
            "내\\s*(식)?재료|" +
                    "내가\\s*(가진|사놓은|사둔|있는|보유한|쟁여둔)|" +
                    "가진\\s*(식)?재료|" +
                    "가지고\\s*있는|" +
                    "보유(한|\\s*재료)?|" +
                    "냉장고|" +
                    "있는\\s*(걸|거|것)\\s*로|" +
                    "남은\\s*(식)?재료|" +
                    "지금\\s*(있는|가진)"
    );

    private static final Pattern WEB_SEARCH_INTENT_PATTERN = Pattern.compile(
            "날씨|기온|미세먼지|황사|예보|기상|폭염|한파|장마|" +
                    "뉴스|속보|이슈|" +
                    "환율|주가|주식|시세|코인|비트코인|이더리움|시총|시가|종가|" +
                    "경기\\s*(결과|일정|점수)|스코어|" +
                    "박스오피스|개봉|상영|" +
                    "실시간|최신|지금\\s*(몇|어디|어떻|어떤)"
    );

    private final ChatClient chatClient;
    private final IntentClassifierProperties properties;

    public IntentClassifier(ChatClient.Builder chatClientBuilder, IntentClassifierProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
    }

    public boolean wantsToUseInventory(String message) {
        return message != null && INVENTORY_INTENT_PATTERN.matcher(message).find();
    }

    public boolean needsWebSearch(String message) {
        return message != null && WEB_SEARCH_INTENT_PATTERN.matcher(message).find();
    }

    public ChatType classify(String message) {
        if (!properties.isEnabled() || message == null || message.isBlank()) {
            return properties.getFallbackType();
        }

        try {
            String raw = chatClient.prompt()
                    .options(GoogleGenAiChatOptions.builder()
                            .model(properties.getModel())
                            .temperature(0.0)
                            .build())
                    .system(SYSTEM_PROMPT)
                    .user(message)
                    .call()
                    .content();

            return parse(raw);
        } catch (Exception e) {
            log.warn("사용자 질문 의도 분석 : 실패 {}: {}",
                    properties.getFallbackType(), e.getMessage());
            return properties.getFallbackType();
        }
    }

    private ChatType parse(String raw) {
        if (raw == null) {
            return properties.getFallbackType();
        }
        String normalized = raw.trim().toUpperCase();
        if (normalized.contains("RECIPE")) {
            return ChatType.RECIPE;
        }
        if (normalized.contains("GENERAL")) {
            return ChatType.GENERAL;
        }
        log.warn("intent classifier returned unrecognized value: '{}'", raw);
        return properties.getFallbackType();
    }
}
