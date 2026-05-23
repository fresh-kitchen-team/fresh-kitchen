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
            - RECIPE: 요리법/레시피/조리법/식단 구성/식재료 활용(무엇을 만들까)/영양 정보 관련 질문,
              그리고 "이 식재료를 어떻게 보관하지?" 같이 식재료의 보관 방법을 묻는 질문 —
              보관 장소 3종(냉장(FRIDGE)/냉동(FREEZER)/실온·팬트리(PANTRY)) 중 어디에 어떻게 두는지 묻는 모든 경우 포함.
              예: "감자 냉장 보관해도 돼?", "고기 냉동실에 얼마나 둘 수 있어?", "양파는 실온이 나아 냉장이 나아?",
                  "팬트리에 둬도 되는 채소 알려줘", "바나나는 냉장 vs 실온 어디가 좋아?", "냉동 보관법 알려줘"
            - GENERAL: 그 외 모든 일상 대화, 실시간 정보(날씨/뉴스/환율 등) 질문,
              단순히 식재료 자체에 대해 묻는 질문(예: "토마토가 뭐야?", "양파는 어디서 나?", "사과 제철이 언제야?", "바나나 칼로리 알려줘"),
              그리고 **사용자 본인의 재고/보유 식재료를 조회하는 질문** —
              "내가 가진 식재료가 뭐가 있어?", "가지고 있는 식재료가 뭐가 있어?",
              "내가 지금 뭐 가지고 있어?", "오늘 식재료 뭐 있어?", "냉장고에 뭐 있어?", "냉동실에 뭐 들어있지?",
              "팬트리에 뭐 있어?", "남은 재료 보여줘", "내 재료 목록", "지금 있는 거 알려줘" 같이
              저장 장소 안에 들어있는 항목을 나열·확인하려는 질문은 모두 GENERAL.
              이러한 재고 조회 질문은 시스템이 사용자의 IngredientRepository에서 실제 보유 목록을 조회해
              "보유 재료:" 형태로 시스템 프롬프트에 주입한다. 답변 시에는 반드시 그 주입된 목록에만
              근거해 사실 그대로 나열·요약하며, 임의로 재료를 만들어내거나 추측하지 마라.
            판단 기준:
              1) 메시지에 "요리해줘", "만들어줘", "레시피", "어떻게 해먹어", "활용법" 같이 조리/활용 의도가 드러나면 RECIPE.
              2) "○○ 어떻게 보관해?", "○○ 냉장/냉동/실온 어디?", "보관법", "얼려도 돼?", "두면 며칠?" 처럼
                 **특정 식재료의 보관 방법**을 묻는 질문은 RECIPE.
              3) 그러나 "내가 가진/가지고 있는 식재료 뭐 있어?", "냉장고/냉동실/팬트리에 뭐 있어?",
                 "내 재료/내 식재료/남은 재료/오늘 뭐 있어/지금 있는 거"처럼
                 **사용자의 보유 재고 목록을 조회**하려는 질문은 RECIPE가 아니라 GENERAL이며,
                 답변은 주입된 "보유 재료:" 목록을 그대로 활용한다.
              4) 위 어디에도 해당하지 않고 식재료 이름만 언급되거나 단순 정보(뜻/원산지/제철/가격/영양 성분 등)만 물으면 GENERAL.
            핵심 구분: "보관 방법을 묻는다" = RECIPE / "보관된 내용물(재고)을 묻는다" = GENERAL (보유 재료 주입 기반 응답).
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
