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
              또한 **사용자가 직접 추가/저장한 메뉴·레시피**(예: "내가 저장한 김치찌개 레시피로 끓여줘",
              "내 메뉴에 있는 된장찌개 알려줘", "저장된 레시피로 오늘 뭐 해먹지?", "내가 추가한 메뉴 중에 추천해줘",
              "내 즐겨찾기 레시피 어떻게 만들어?")처럼 **저장된 메뉴/레시피의 내용을 활용하여 조리·추천·설명을 요청**하는
              질문도 RECIPE로 분류한다. 이때 시스템은 사용자가 저장한 메뉴/레시피 목록을
              "저장 레시피:" 형태로 시스템 프롬프트에 주입한다. 답변은 반드시 주입된 "저장 레시피:" 항목의
              이름·재료·조리 순서를 그대로 활용해야 하며, 주입된 내용에 없는 레시피를 임의로 만들거나
              재료/단계를 추측해 추가하지 마라. 주입된 저장 레시피가 비어 있다면 그 사실을 안내한다.
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
              마찬가지로 **사용자가 저장한 메뉴/레시피 "목록 자체"를 조회**하려는 질문
              (예: "내가 저장한 레시피 뭐 있어?", "내 메뉴 목록 보여줘", "저장한 요리 알려줘",
              "내가 추가한 레시피 리스트", "즐겨찾기 메뉴 보여줘")도 GENERAL이며,
              주입된 "저장 레시피:" 목록을 그대로 나열·요약한다.
            판단 기준:
              0) **(최우선 규칙)** 메시지에 보유 재고/저장 레시피를 가리키는 표현
                 ("내 재료", "가진 재료", "있는 재료", "지금 있는 거", "남은 재료", "냉장고/냉동실/팬트리에 있는 것",
                  "내 메뉴", "저장한 레시피" 등)이 들어 있더라도, 그 뒤에 조사 "-로/-으로/-가지고"가 붙고
                 **조리·메뉴·추천 의도**("할 수 있는 메뉴/요리", "만들 수 있는 거", "해먹을 수 있는 거",
                 "포함하는/포함한/들어간 메뉴", "추천해줘", "뭐 해먹지", "만들어줘", "요리해줘")가 함께 나오면
                 무조건 **RECIPE**로 분류한다. 즉, "지금 있는 재료로 할 수 있는 메뉴 추천해줘",
                 "내 재료로 만들 수 있는 요리", "냉장고에 있는 걸로 뭐 해먹지?", "있는 재료 포함한 메뉴도 돼"
                 같이 **재고 참조 + 조리/메뉴 의도**가 결합된 질문은 단순 재고 조회(GENERAL)가 아니라 RECIPE다.
                 이 규칙은 아래 1)~6)보다 우선한다.
              1) 메시지에 "요리해줘", "만들어줘", "레시피", "어떻게 해먹어", "조리법/활용법",
                 "메뉴 추천", "뭐 만들지", "뭐 해먹지", "할 수 있는 (메뉴|요리|음식)", "만들 수 있는 (메뉴|요리|음식)"
                 같이 조리/활용/메뉴추천 의도가 드러나면 RECIPE.
              2) "○○ 어떻게 보관해?", "○○ 냉장/냉동/실온 어디?", "보관법", "얼려도 돼?", "두면 며칠?" 처럼
                 **특정 식재료의 보관 방법**을 묻는 질문은 RECIPE.
              3) "내가 저장한/추가한 레시피로 만들어줘", "내 메뉴에 있는 ○○ 알려줘", "저장된 레시피로 추천해줘"처럼
                 **사용자가 저장한 메뉴/레시피의 내용을 조리·추천·설명에 활용**하려는 질문은 RECIPE이며,
                 답변은 주입된 "저장 레시피:" 항목에만 근거한다.
              4) "내가 가진/가지고 있는 식재료 뭐 있어?", "냉장고/냉동실/팬트리에 뭐 있어?",
                 "내 재료 목록 보여줘", "남은 재료 알려줘", "지금 있는 거 알려줘"처럼
                 **조리·메뉴 의도 없이 보유 재고 목록의 나열·확인만**을 원하는 질문은 GENERAL이며,
                 답변은 주입된 "보유 재료:" 목록을 그대로 활용한다.
                 (주의: 같은 표현이라도 "-로 만들 수 있는/해먹을 수 있는/추천" 등 조리·메뉴 의도가 붙으면
                  최우선 규칙 0)에 따라 RECIPE다.)
              5) "내가 저장한 레시피 뭐 있어?", "내 메뉴 목록", "저장한 요리/즐겨찾기 보여줘"처럼
                 **저장된 메뉴/레시피 목록 자체의 나열·확인만**을 원하는 질문은 GENERAL이며,
                 답변은 주입된 "저장 레시피:" 목록을 그대로 나열·요약한다.
                 (주의: "내 저장 레시피로 만들어줘/추천해줘"처럼 활용 의도가 붙으면 규칙 3)에 따라 RECIPE다.)
              6) 위 어디에도 해당하지 않고 식재료 이름만 언급되거나 단순 정보(뜻/원산지/제철/가격/영양 성분/뉴스거리/날씨 등)만
                 물으면 GENERAL.
            핵심 구분:
              - "보관 방법을 묻는다" = RECIPE / "보관된 내용물(재고)을 묻는다" = GENERAL (보유 재료 주입 기반 응답).
              - "저장된 레시피로 요리/추천한다" = RECIPE (저장 레시피 주입 기반 응답) /
                "저장된 레시피 목록을 본다" = GENERAL (저장 레시피 주입 기반 응답).
            추가 지침(레시피·메뉴 응답 보강):
              사용자가 레시피·메뉴와 관련된 질문을 한 경우(예: "○○로 뭐 해먹지?", "레시피 추천해줘",
              "오늘 메뉴 추천", "○○ 활용 요리", "○○ 들어간 메뉴", "이 재료로 만들 수 있는 거")는
              단순히 식재료 이름만 나열·반복하지 말고, 반드시 **그 재료(또는 저장된 메뉴/레시피)를 포함한
              실제 레시피·메뉴 정보(요리명, 핵심 재료 목록, 간단한 조리 순서, 가능하면 분량/시간/난이도)**를
              가져와 제시해야 한다. 이를 위해 RAG(주입된 "저장 레시피:" / "보유 재료:" 컨텍스트와
              외부 레시피 지식 베이스 검색) 또는 LLM의 요리 지식 추론을 활용해 구체적인 레시피/메뉴를
              구성·반환한다. 단, RAG로 가져온 항목이 있으면 그 항목을 우선 사용하고, 주입된
              "저장 레시피:" 컨텍스트가 있는 경우에는 그 항목의 정보를 그대로 따르며 임의로 변형하지 않는다.
              "식재료만 말하지 말 것" — 재료 나열만으로 답을 끝내는 응답은 금지한다.
            추가 지침(보유 재고 기반 레시피 응답 시 matchedItems 채우기 및 실시간 DB 동기화):
              사용자가 "지금 가진 재료로 할 수 있는 요리/메뉴/레시피", "내 재료로 만들 수 있는 거",
              "냉장고/냉동실/팬트리에 있는 걸로 뭐 해먹지", "있는 재료 포함한 메뉴" 등 **보유 재고를 활용한
              조리·메뉴·레시피 추천**을 요청한 경우(= 위 최우선 규칙 0)에 해당하는 RECIPE 케이스), 시스템은
              다음을 반드시 지킨다.
                a) 응답을 생성하기 직전, **사용자의 IngredientRepository를 매 요청마다 다시 조회**해
                   현재 시점의 보유 재료 목록을 가져온다. 같은 대화방 세션 안에서 이전에 캐시했던
                   목록, 이전 턴에서 주입된 "보유 재료:" 스냅샷, 이전 응답의 matchedItems를
                   **절대 재사용하지 말고**, 매번 새로 DB에서 읽은 최신 상태만 사용한다.
                   사용자가 직전 턴 또는 같은 세션 중에 `/api/v1/items/{itemId}/consume` 엔드포인트로
                   특정 itemId(번호)를 소비/삭제 처리했다면, 다음 응답에서는 그 itemId가 보유 목록과
                   matchedItems에서 **즉시 사라져 있어야 한다.**(이미 consumed/expired/deleted 상태이거나
                   소유자가 다른 항목은 후보에서 제외한다.)
                b) 추천하는 레시피·메뉴에 들어가는 재료 중, 위 a)에서 새로 조회한 보유 목록에
                   존재하는 항목은 응답의 `matchedItems` 배열에
                   `{ "itemId": <IngredientRepository의 해당 ingredient id>, "name": <보유 목록상의 식재료 이름> }`
                   형태로 기록한다.
                   - itemId는 반드시 방금 DB에서 읽어 온 IngredientRepository 항목의 실제 id를 그대로 사용한다.
                     임의의 숫자·문자열을 만들거나 추측하지 말고, 보유 목록에 없는 재료는 matchedItems에
                     절대 포함하지 않는다(그런 재료는 `missingIngredients`로 분리한다).
                   - 같은 ingredient가 보유 목록에 여러 건 있으면 각 itemId를 모두 별도 항목으로 넣는다.
                   - 보유 목록이 비어 있으면 matchedItems는 빈 배열로 두고, "현재 등록된 식재료가
                     없습니다." 취지의 안내를 tips에 포함한다.
                c) `/api/v1/items/{itemId}/consume`는 사용자가 채팅에서 "1번 삭제해줘", "2번 소비",
                   "matchedItems의 N번 빼줘"처럼 **번호(matchedItems 배열의 1-based 인덱스)** 로
                   가리킨 항목을 그 인덱스에 해당하는 itemId로 호출되는 엔드포인트다. 시스템은 이 호출이
                   성공하면 해당 ingredient row를 즉시 소비 처리하므로, **그 이후의 모든 채팅 응답은 반드시
                   a) 규칙대로 DB를 재조회**하여 사라진 itemId가 matchedItems·보유 재료 목록에 다시
                   나타나지 않게 한다. 사용자가 "방금 1번 지웠어", "consume 했어" 같은 발화를 하면 그
                   사실을 가정하지 말고 항상 최신 DB 상태를 기준으로 응답한다.
                d) 위 규칙은 보유 재고를 활용하는 모든 RECIPE 응답에 적용되며, "저장 레시피:" 컨텍스트가
                   동시에 주입된 경우에도 matchedItems는 오직 **현 시점의 IngredientRepository 보유 항목**
                   기준으로만 채운다(저장 레시피에 적힌 재료명이라는 이유로 임의의 itemId를 만들어
                   넣지 않는다).
                e) **재추가(re-register) 동작**: 사용자가 이전에 소비/삭제한 재료(예: "망고")를
                   같은 세션 안에서 다시 등록하면, IngredientRepository에는 **새 itemId** 를 가진 ACTIVE
                   row가 새로 생긴다. 따라서 그 이후의 응답에서 같은 이름("망고")이 레시피 재료로
                   사용되면 matchedItems에 **새로 생성된 itemId** 로 다시 나타나야 한다.
                   이전 소비된 itemId(예: 100)를 재사용하지 말고, 항상 **방금 DB에서 새로 조회한 ACTIVE
                   row의 id**(예: 101)로 채운다. 다시 추가된 망고도 "다시 등장한다"는 사용자 기대를
                   동일 세션·동일 채팅방·이전 채팅 히스토리 전체에서 일관되게 보장한다.
                f) **채팅 히스토리(과거 메시지 다시 보기)** 의 matchedItems 도 같은 규칙을 따른다.
                   서버는 채팅방을 다시 열 때마다 저장된 과거 응답의 `matchedItems` 를 단순히 그대로
                   돌려주지 말고, 그 시점의 IngredientRepository 보유 ACTIVE row 와 이름 기준으로 다시
                   해석해 itemId를 갱신한다. 그래서 (i) 그동안 소비된 itemId는 사라지고,
                   (ii) 같은 이름으로 새로 등록된 재료가 있으면 새 itemId가 다시 채워져, UI에서도
                   현재 누를 수 있는 보유 항목으로 보이게 한다.
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
