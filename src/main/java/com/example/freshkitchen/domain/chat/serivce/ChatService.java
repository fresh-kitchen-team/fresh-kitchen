package com.example.freshkitchen.domain.chat.serivce;


import com.example.freshkitchen.domain.chat.entity.AiSetting;
import com.example.freshkitchen.domain.chat.entity.ChatMessage;
import com.example.freshkitchen.domain.chat.entity.ChatRoom;
import com.example.freshkitchen.domain.chat.entity.Sender;
import com.example.freshkitchen.domain.chat.exception.ChatErrorCode;
import com.example.freshkitchen.domain.chat.exception.ChatException;
import com.example.freshkitchen.domain.chat.repository.AiSettingRepository;
import com.example.freshkitchen.domain.chat.repository.ChatMessageRepository;
import com.example.freshkitchen.domain.chat.repository.ChatRoomRepository;
import com.example.freshkitchen.domain.chat.intent.IntentClassifier;
import com.example.freshkitchen.application.ingredient.usecase.ConsumeIngredientUseCase;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.presentation.chat.dto.request.AiSettingSaveRequest;
import com.example.freshkitchen.presentation.chat.dto.request.ChatMessageRequest;
import com.example.freshkitchen.presentation.chat.dto.request.ChatType;
import com.example.freshkitchen.presentation.chat.dto.request.UpdateRoomTitleRequest;
import com.example.freshkitchen.presentation.chat.dto.response.AiSettingResponse;
import com.example.freshkitchen.presentation.chat.dto.response.ChatHistoryResponse;
import com.example.freshkitchen.presentation.chat.dto.response.ChatMessageResponse;
import com.example.freshkitchen.presentation.chat.dto.response.ChatRoomListResponse;
import com.example.freshkitchen.presentation.chat.dto.response.ChatRoomResponse;
import com.example.freshkitchen.presentation.chat.dto.response.ConsumeItemsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ChatService {
    private static final int QUERY_REWRITE_MIN_LENGTH = 30;
    private static final int RAG_TOP_K = 1;
    private static final double RAG_SIMILARITY_THRESHOLD = 0.7;

    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisorNoRewrite;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository messageRepository;
    private final AiSettingRepository aiSettingRepository;
    private final IngredientRepository ingredientRepository;
    private final IntentClassifier intentClassifier;
    private final ConsumeIngredientUseCase consumeIngredientUseCase;
    private final ChatMemory chatMemory;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;

    public ChatService(ObjectMapper objectMapper, ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
                       UserRepository userRepository, ChatRoomRepository chatRoomRepository,
                       ChatMessageRepository messageRepository, AiSettingRepository aiSettingRepository,
                       IngredientRepository ingredientRepository,
                       IntentClassifier intentClassifier,
                       ConsumeIngredientUseCase consumeIngredientUseCase,
                       ChatMemory chatMemory) {
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
        this.aiSettingRepository = aiSettingRepository;
        this.ingredientRepository = ingredientRepository;
        this.intentClassifier = intentClassifier;
        this.consumeIngredientUseCase = consumeIngredientUseCase;
        this.chatMemory = chatMemory;
        this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "[SimpleLoggerAdvisor] Custom request: " + request.prompt().getUserMessage(),
                response -> "[SimpleLoggerAdvisor] Custom response: " + response.getResult(),
                Ordered.HIGHEST_PRECEDENCE);

        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .topK(RAG_TOP_K)
                .similarityThreshold(RAG_SIMILARITY_THRESHOLD)
                .vectorStore(vectorStore)
                .build();
        PromptTemplate ragPromptTemplate = PromptTemplate.builder()
                .template("""
                        아래 참고 자료가 주어졌으면 반드시 이 자료에 기반해 답하라.
                        자료에 없는 정보는 임의로 만들지 말고, 자료 범위 내에서 답하라.
                        외부 웹검색은 절대 사용하지 마라.
                        시스템 프롬프트의 JSON 포맷·규칙을 반드시 지키고, recipes를 빈 배열로 반환하지 마라.
                        
                        참고 자료:
                        {context}
                        
                        질문:
                        {query}
                        """)
                .build();
        PromptTemplate emptyContextPromptTemplate = PromptTemplate.builder()
                .template("""
                        참고 자료가 없다. 외부 웹검색은 절대 사용하지 말고,
                        너의 사전 지식으로 사용자의 질문에 직접 답하라.
                        시스템 프롬프트의 JSON 포맷·규칙을 반드시 지키고, recipes를 빈 배열로 반환하지 마라.
                        
                        질문:
                        {query}
                        """)
                .build();
        ContextualQueryAugmenter delegateAugmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(ragPromptTemplate)
                .allowEmptyContext(true)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
        QueryAugmenter queryAugmenter = (query, documents) -> {
            if (documents == null || documents.isEmpty()) {
                log.info("[RAG] EMPTY context -> LLM fallback (query='{}')", query.text());
            } else {
                log.info("[RAG] JOINED {} doc(s) -> RAG answer (query='{}', names={})",
                        documents.size(),
                        query.text(),
                        documents.stream().map(d -> String.valueOf(d.getMetadata().get("name"))).toList());
            }
            return delegateAugmenter.augment(query, documents);
        };

        retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(chatClientBuilder)
                        .build())
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();

        retrievalAugmentationAdvisorNoRewrite = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(customLogger)
                .build();
    }

    private RetrievalAugmentationAdvisor selectRagAdvisor(String message) {
        if (message != null && message.length() < QUERY_REWRITE_MIN_LENGTH) {
            return retrievalAugmentationAdvisorNoRewrite;
        }
        return retrievalAugmentationAdvisor;
    }

    private List<Document> searchSimilarDocuments(String query, String vectorStoreType) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(RAG_TOP_K)
                .similarityThreshold(RAG_SIMILARITY_THRESHOLD)
                .filterExpression("type == '%s'".formatted(vectorStoreType))
                .build();

        List<Document> matches = vectorStore.similaritySearch(request);
        if (matches == null || matches.isEmpty()) {
            log.info("vector similarity search type={}, query='{}': no match (topK={}, threshold={})",
                    vectorStoreType, query, RAG_TOP_K, RAG_SIMILARITY_THRESHOLD);
            return List.of();
        }
        for (Document doc : matches) {
            Object distanceObj = doc.getMetadata().get("distance");
            Double similarity = (distanceObj instanceof Number n) ? 1.0 - n.doubleValue() : null;
            log.info("vector similarity hit type={}, name={}, distance={}, similarity={}",
                    vectorStoreType,
                    doc.getMetadata().get("name"),
                    distanceObj,
                    similarity);
        }
        return matches;
    }

    @Transactional
    public AiSettingResponse saveAiSetting(Long userId, AiSettingSaveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.USER_NOT_FOUND));

        String responseStyle = request.responseStyle() ? "간단" : "자세";

        AiSetting setting = aiSettingRepository.findByUserId(userId)
                .orElseGet(() -> AiSetting.createDefault(user));

        setting.update(
                request.provideExtraInfo(),
                request.priorityExpiration(),
                request.priorityNutrition(),
                request.priorityFrequent(),
                responseStyle);

        AiSetting saved = aiSettingRepository.save(setting);
        return AiSettingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AiSettingResponse getAiSetting(Long userId) {
        AiSetting setting = aiSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.AI_SETTING_NOT_FOUND));
        return AiSettingResponse.from(setting);
    }

    @Transactional
    public ChatRoomResponse createChatRoom(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.create("AI 요리비서", user);
        ChatRoom saved = chatRoomRepository.save(chatRoom);


        return new ChatRoomResponse(saved.getId(), saved.getTitle(), saved.getCreatedAt());
    }

    public Flux<String> ragChat(String question, String type, String conversationId) {
        return this.chatClient.prompt()
                .system("친절하게 한국어로 답변해줘.")
                .user(question)
                .advisors(selectRagAdvisor(question))
                .stream()
                .content();
    }

    private static final String RECIPE_SYSTEM_PROMPT_PREFIX = """
            한국어 요리 비서. 아래 규칙·포맷을 지켜라.
            - 응답은 순수 JSON만. 마크다운/코드블록 금지.
            - ingredients는 재료 이름만 (수량/단위/괄호 설명 금지). 예: "고구마" O, "고구마 1개" X.
            - ingredients에는 레시피에 사용되는 모든 재료를 빠짐없이 나열하라.
              주재료뿐 아니라 양념(간장·설탕·소금·후추 등), 기름(식용유·참기름 등),
              향신료(다진마늘·생강 등), 부재료(양파·당근 등), 고명(통깨 등)도 반드시 포함한다.
              steps에 언급되는 모든 식재료는 ingredients에도 반드시 들어가야 한다.
            - missingIngredients: ingredients 중 "보유 재료"에 없는 항목을 모두 담아라.
              보유 재료 정보가 없거나, ingredients가 전부 보유 재료에 있으면 [].
            - 보유 재료가 있으면 그것을 최대한 활용하라. 일반적인 양념·기름 등 흔한 재료는
              보유 여부와 무관하게 ingredients에 포함하되, 보유 재료에 없으면 missingIngredients로 분리한다.
            - 반드시 "조리"가 필요한 요리만 추천하라. 즉 steps에는 굽기·볶기·찌기·끓이기·삶기·튀기기·
              조리기·부치기·데치기·전자레인지/오븐 가열 등 **열을 가하거나 조리 과정**이 최소 1단계 이상 포함되어야 한다.
              "그대로 먹는다", "씻어서 먹는다", "껍질만 까서 먹는다", "잘라서 먹는다" 같이
              **조리 없이 날것/생식 그대로 섭취하는 추천은 절대 하지 마라.** (샐러드도 드레싱 만들기·데치기 등 조리 단계가 있을 때만 허용)
            - 보유 재료만으로 제대로 된 요리가 어렵다면, 흔하게 구할 수 있는 재료(양파·마늘·달걀·우유·밀가루·간장·설탕 등)를
              **적극적으로 추가**해 한 끼로 먹을 수 있는 정식 메뉴를 구성하라. 추가한 재료는 missingIngredients에 담는다.
              사용자가 "다른 재료 추가해도 됨" 같은 명시를 하지 않아도 위 원칙을 기본으로 적용한다.
            - 사용자가 단순히 "내 재료로 메뉴 추천", "할 수 있는 음식" 정도로만 짧게 물어봐도
              위 규칙(조리 필수 + 보충 재료 허용)을 동일하게 적용해 답하라.
            포맷:
            {"recipes":[{"name":"","ingredients":[],"steps":[],"time":""}],"tips":[],"missingIngredients":[]}
            """;

    private String buildSystemPrompt(AiSettingSaveRequest setting, UserProfile profile,
                                     String userIngredients, boolean inventoryRequestedButEmpty) {
        StringBuilder sb = new StringBuilder(RECIPE_SYSTEM_PROMPT_PREFIX);

        if (userIngredients != null && !userIngredients.isBlank()) {
            sb.append("보유 재료: ").append(userIngredients).append('\n');
        } else if (inventoryRequestedButEmpty) {
            sb.append("사용자가 본인 재료로 만들 수 있는 메뉴를 물었지만 등록된 재료가 없다. ")
                    .append("이 사실을 한 줄 안내(tips에 포함)하고, 보편적으로 인기 있는 일반 레시피를 추천하라.\n");
        }

        if (profile != null) {
            if (!profile.getAllergies().isEmpty()) {
                sb.append("알러지(절대 제외): ")
                        .append(profile.getAllergies().stream().map(Enum::name).collect(Collectors.joining(",")))
                        .append('\n');
            }
            if (!profile.getFoodStyles().isEmpty()) {
                sb.append("선호 스타일: ")
                        .append(profile.getFoodStyles().stream().map(Enum::name).collect(Collectors.joining(",")))
                        .append('\n');
            }
            if (!profile.getCookingTools().isEmpty()) {
                sb.append("조리도구: ")
                        .append(profile.getCookingTools().stream().map(Enum::name).collect(Collectors.joining(",")))
                        .append('\n');
            }
            if (!profile.getPreferredIngredients().isEmpty()) {
                sb.append("선호 재료: ")
                        .append(String.join(",", profile.getPreferredIngredients()))
                        .append('\n');
            }
        }

        if (setting != null) {
            sb.append("## 사용자 응답 옵션(반드시 준수)\n");
            if (setting.responseStyle()) {
                sb.append("- 응답 스타일=간단: 각 레시피의 steps는 5단계 이내로 압축하고, 각 step은 짧은 명령형 한 문장으로 작성한다.\n");
            } else {
                sb.append("- 응답 스타일=자세: 각 레시피의 steps는 8단계 이상으로 세분화하고, 재료 손질·불 세기·시간·양념 비율을 구체적으로 명시한다.\n");
            }
            if (setting.provideExtraInfo()) {
                sb.append("- 추가 정보=ON: tips 배열에 영양 정보·보관 팁·조리 팁·대체 재료 제안 중에서 반드시 2~3개 항목을 채운다. 절대 빈 배열로 두지 말 것.\n");
            } else {
                sb.append("- 추가 정보=OFF: tips는 비어 있어도 무방.\n");
            }
            if (setting.priorityExpiration()) {
                sb.append("- 유통기한 우선=ON: 보유 재료 중 유통기한이 임박한 항목(7일 이내)을 가장 먼저 사용하는 레시피를 우선 추천한다.\n");
            }
            if (setting.priorityNutrition()) {
                sb.append("- 영양 균형=ON: 한 끼 안에 탄수화물·단백질·채소가 골고루 포함되도록 메뉴를 구성한다.\n");
            }
            if (setting.priorityFrequent()) {
                if (profile != null && !profile.getPreferredIngredients().isEmpty()) {
                    sb.append("- 자주 쓰는 재료 우선=ON: 위 \"선호 재료\" 목록을 적극 활용해 레시피를 구성한다.\n");
                } else {
                    sb.append("- 자주 쓰는 재료 우선=ON: 사용자의 선호 재료가 등록되어 있지 않다면, 보편적으로 자주 사용되는 기본 식재료를 우선 활용한다.\n");
                }
            }
        }

        return sb.toString();
    }

    private AiSettingSaveRequest loadSettingValues(Long userId) {
        return aiSettingRepository.findByUserId(userId)
                .map(setting -> new AiSettingSaveRequest(
                        "간단".equals(setting.getResponseStyle()),
                        setting.isPriorityExpiration(),
                        setting.isPriorityNutrition(),
                        setting.isPriorityFrequent(),
                        setting.isProvideExtraInfo()))
                .orElse(null);
    }

    private String responseStyleDirective(AiSettingSaveRequest setting) {
        if (setting == null) {
            return "";
        }
        return setting.responseStyle()
                ? " 답변은 핵심만 2~3문장 이내로 간결하게 작성하라."
                : " 답변은 상세하게, 배경 설명과 예시까지 포함해 친절하게 작성하라.";
    }

    // ✅ [추가] 첫 메시지 기반 채팅방 제목 자동 생성

    private String generateRoomTitle(String userMessage) {
        String titlePrompt = """
                다음 메시지를 보고 채팅방 제목을 10자 이내로 만들어줘.
                마크다운, 따옴표, 특수문자 없이 순수 텍스트로만 반환해.
                메시지: %s
                """.formatted(userMessage);

        String title = this.chatClient.prompt()
                .options(GoogleGenAiChatOptions.builder()
                        .model("gemini-2.5-flash-lite")
                        .temperature(0.0)
                        .build())
                .user(titlePrompt)
                .call()
                .content();

        return title != null ? title.trim() : "AI 요리비서";
    }



    private String cleanJsonResponse(String response) {
        return response
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("```", "")
                .trim();
    }

    private static final Pattern PARENTHESES = Pattern.compile("\\([^)]*\\)|\\[[^\\]]*\\]");
    private static final Pattern QUANTITY_SUFFIX = Pattern.compile(
            "\\s*[\\d./]+\\s*(kg|g|ml|l|cc|t|T|tbsp|tsp|개|컵|큰술|작은술|숟갈|숟가락|줌|마리|장|포기|모|대|쪽|알|봉|봉지|캔|팩|조각|꼬집)?\\s*$");

    private static String normalizeIngredientName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private static String stripQuantityAndParens(String name) {
        if (name == null) {
            return "";
        }
        String stripped = PARENTHESES.matcher(name).replaceAll(" ");
        stripped = QUANTITY_SUFFIX.matcher(stripped).replaceFirst("");
        return stripped.trim();
    }

    private static boolean isOwnedIngredient(String recipeIngredient, Set<String> ownedNormalizedNames) {
        if (recipeIngredient == null || recipeIngredient.isBlank() || ownedNormalizedNames.isEmpty()) {
            return false;
        }
        String normalized = normalizeIngredientName(recipeIngredient);
        if (normalized.isBlank()) {
            return false;
        }
        if (ownedNormalizedNames.contains(normalized)) {
            return true;
        }
        for (String owned : ownedNormalizedNames) {
            if (owned.length() < 2) {
                continue;
            }
            if (normalized.contains(owned) || owned.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static Ingredient findMatchedOwnedIngredient(String recipeIngredient, List<Ingredient> ownedIngredients) {
        if (recipeIngredient == null || recipeIngredient.isBlank() || ownedIngredients.isEmpty()) {
            return null;
        }
        String normalized = normalizeIngredientName(recipeIngredient);
        if (normalized.isBlank()) {
            return null;
        }
        for (Ingredient owned : ownedIngredients) {
            if (normalizeIngredientName(owned.getName()).equals(normalized)) {
                return owned;
            }
        }
        for (Ingredient owned : ownedIngredients) {
            String ownedNormalized = normalizeIngredientName(owned.getName());
            if (ownedNormalized.length() < 2) {
                continue;
            }
            if (normalized.contains(ownedNormalized) || ownedNormalized.contains(normalized)) {
                return owned;
            }
        }
        return null;
    }

    private ChatMessageResponse.AiPayloadResponse tryParsePayload(String json) {
        try {
            return objectMapper.readValue(json, ChatMessageResponse.AiPayloadResponse.class);
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", json);
            return null;
        }
    }

    @Transactional
    public ChatMessageResponse sendAiMessage(Long userId, Long roomId, ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        // 여기서 일차로 질문을 분류
        ChatType resolvedType = intentClassifier.classify(request.message());
        boolean isGeneralChat = resolvedType == ChatType.GENERAL;
        boolean inventoryIntent = intentClassifier.wantsToUseInventory(request.message());
        // 내가 가지고 있는 재고 사용 (RECIPE 분기)
        boolean useInventory = !isGeneralChat && inventoryIntent;
        // GENERAL 분기에서 '내가 가진 재료 뭐 있어?' 같은 재고 조회 질문
        boolean inventoryListQuery = isGeneralChat && inventoryIntent;
        List<Ingredient> userIngredients = (isGeneralChat && !inventoryListQuery)
                ? List.of()
                : ingredientRepository.findAllByUserIdAndStatus(userId, IngredientStatus.ACTIVE);

        log.info("chat intent userId={}, type={}, useInventory={}, inventoryListQuery={}, activeIngredientCount={}",
                userId, resolvedType, useInventory, inventoryListQuery, userIngredients.size());

        String aiText;
        ChatMessageResponse.AiPayloadResponse aiPayload;
        String storedAiPayload;
        String uiType = isGeneralChat ? "GENERAL" : "RECIPE";

        if (isGeneralChat) {
            boolean useGrounding = intentClassifier.needsWebSearch(request.message());
            String systemPrompt;
            if (inventoryListQuery) {
                String ownedList = userIngredients.stream()
                        .map(Ingredient::getName)
                        .collect(Collectors.joining(", "));
                if (ownedList.isBlank()) {
                    systemPrompt = """
                            친절하게 한국어로 답변해줘.
                            사용자가 본인의 보유 식재료(재고)를 물었으나, 등록된 재료가 없다.
                            "현재 등록된 식재료가 없습니다"라고 안내하고, 등록을 권유하는 한 줄을 덧붙여라.
                            절대 임의로 재료를 만들어 답하지 마라.
                            """;
                } else {
                    systemPrompt = """
                            친절하게 한국어로 답변해줘.
                            사용자가 본인의 보유 식재료(재고)를 묻고 있다. 아래 "보유 재료" 목록만 근거로
                            사실 그대로 나열·요약하고, 목록에 없는 재료는 절대 추가하지 마라.
                            보유 재료: %s
                            """.formatted(ownedList);
                }
            } else {
                systemPrompt = useGrounding
                        ? "친절하게 한국어로 답변해줘. 최신 정보가 필요한 질문에는 구글 검색 결과를 활용하고, 답변 끝에 참고한 출처 URL을 함께 적어줘."
                        : "친절하게 한국어로 답변해줘.";
            }

            systemPrompt = systemPrompt + responseStyleDirective(loadSettingValues(userId));

            try {
                ChatClient.ChatClientRequestSpec spec = this.chatClient.prompt()
                        .system(systemPrompt)
                        .user(request.message())
                        .advisors(chatMemoryAdvisor)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(roomId)));

                if (useGrounding) {
                    spec = spec.options(GoogleGenAiChatOptions.builder()
                            .googleSearchRetrieval(true)
                            .build());
                }

                aiText = spec.call().content();
            } catch (ChatException e) {
                throw new ChatException(ChatErrorCode.GEMINI_QUOTA_EXCEEDED);
            }
            aiPayload = null;
            storedAiPayload = null;
        } else {
            String ingredientList = useInventory
                    ? userIngredients.stream()
                      .map(Ingredient::getName)
                      .collect(Collectors.joining(", "))
                    : "";

            Set<String> ownedIngredientNames = userIngredients.stream()
                    .map(i -> normalizeIngredientName(i.getName()))
                    .collect(Collectors.toSet());

            AiSettingSaveRequest settingValues = loadSettingValues(userId);
            UserProfile profile = userRepository.findById(userId)
                    .map(User::getProfile)
                    .orElse(null);
            boolean inventoryRequestedButEmpty = useInventory && userIngredients.isEmpty();
            String systemPrompt = buildSystemPrompt(settingValues, profile, ingredientList, inventoryRequestedButEmpty);

            searchSimilarDocuments(request.message(), resolvedType.vectorStoreType());

            String aiResponseJson;
            try {
                RetrievalAugmentationAdvisor ragAdvisor = selectRagAdvisor(request.message());
                aiResponseJson = this.chatClient.prompt()
                        .system(systemPrompt)
                        .user(request.message())
                        .advisors(a -> a
                                .advisors(chatMemoryAdvisor, ragAdvisor)
                                .param(ChatMemory.CONVERSATION_ID, String.valueOf(roomId))
                                .param(VectorStoreDocumentRetriever.FILTER_EXPRESSION,
                                        "type == '%s'".formatted(resolvedType.vectorStoreType())))
                        .call()
                        .content();
            } catch (ChatException e) {
                throw new ChatException(ChatErrorCode.GEMINI_QUOTA_EXCEEDED);
            }

            String cleanJson = cleanJsonResponse(aiResponseJson);
            ChatMessageResponse.AiPayloadResponse parsed = tryParsePayload(cleanJson);

            if (parsed != null && parsed.recipes() != null) {
                Set<String> missing = parsed.recipes().stream()
                        .flatMap(recipe -> recipe.ingredients() == null
                                ? Stream.<String>empty()
                                : recipe.ingredients().stream())
                        .map(ChatService::stripQuantityAndParens)
                        .filter(ingredient -> !ingredient.isBlank())
                        .filter(ingredient -> !isOwnedIngredient(ingredient, ownedIngredientNames))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                LinkedHashMap<Long, ChatMessageResponse.MatchedItemResponse> matched = new LinkedHashMap<>();
                parsed.recipes().stream()
                        .flatMap(recipe -> recipe.ingredients() == null
                                ? Stream.<String>empty()
                                : recipe.ingredients().stream())
                        .map(ChatService::stripQuantityAndParens)
                        .filter(ingredient -> !ingredient.isBlank())
                        .forEach(ingredient -> {
                            Ingredient owned = findMatchedOwnedIngredient(ingredient, userIngredients);
                            if (owned != null) {
                                matched.putIfAbsent(owned.getId(),
                                        new ChatMessageResponse.MatchedItemResponse(owned.getId(), owned.getName()));
                            }
                        });

                aiPayload = new ChatMessageResponse.AiPayloadResponse(
                        parsed.recipes(),
                        parsed.tips(),
                        new ArrayList<>(missing),
                        new ArrayList<>(matched.values()));
            } else {
                aiPayload = new ChatMessageResponse.AiPayloadResponse(List.of(), List.of(), List.of(), List.of());
            }

            try {
                storedAiPayload = objectMapper.writeValueAsString(aiPayload);
            } catch (Exception e) {
                log.error("aiPayload 직렬화 실패");
                storedAiPayload = cleanJson;
            }
            aiText = storedAiPayload;
        }

        long previousMessageCount = messageRepository.countByRoomId(roomId);

        ChatMessage userMessage = ChatMessage.create(
                request.message(),
                Sender.USER,
                null,
                chatRoom,
                chatRoom.getUser()
        );
        messageRepository.save(userMessage);

        ChatMessage chatMessage = ChatMessage.create(
                aiText,
                Sender.AI,
                storedAiPayload,
                chatRoom,
                chatRoom.getUser()
        );

        ChatMessage saved = messageRepository.save(chatMessage);

        if (previousMessageCount == 0) {
            String autoTitle = generateRoomTitle(request.message());
            chatRoom.updateTitle(autoTitle);
            chatRoomRepository.save(chatRoom);
        }

        return new ChatMessageResponse(
                chatRoom.getTitle(),

                new ChatMessageResponse.AiMessageResponse(
                        saved.getId(), "AI", aiText, aiPayload, uiType, saved.getCreatedAt().toString()));
    }

    @Transactional(readOnly = true)
    public ChatRoomListResponse getRoomList(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime startOf7Days = now.minusDays(7);
        OffsetDateTime startOf30Days = now.minusDays(30);

        List<ChatRoomListResponse.ChatMessageGroup> today = rooms.stream()
                .filter(r -> r.getUpdatedAt().isAfter(startOfToday))
                .map(this::toChatMessageGroup)
                .collect(Collectors.toList());

        List<ChatRoomListResponse.ChatMessageGroup> last7Days = rooms.stream()
                .filter(r -> r.getUpdatedAt().isAfter(startOf7Days) && r.getUpdatedAt().isBefore(startOfToday))
                .map(this::toChatMessageGroup)
                .collect(Collectors.toList());

        List<ChatRoomListResponse.ChatMessageGroup> last30Days = rooms.stream()
                .filter(r -> r.getUpdatedAt().isAfter(startOf30Days) && r.getUpdatedAt().isBefore(startOf7Days))
                .map(this::toChatMessageGroup)
                .collect(Collectors.toList());

        return new ChatRoomListResponse(today, last7Days, last30Days);
    }

    private ChatRoomListResponse.ChatMessageGroup toChatMessageGroup(ChatRoom room) {
        return new ChatRoomListResponse.ChatMessageGroup(
                room.getId(), room.getTitle(), room.getUpdatedAt().toString(), null, null);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        // 현재 ACTIVE 식재료 이름 목록 — missingIngredients 재계산용
        Long userId = chatRoom.getUser().getId();
        Set<String> currentOwnedNames = ingredientRepository
                .findAllByUserIdAndStatus(userId, IngredientStatus.ACTIVE)
                .stream()
                .map(i -> normalizeIngredientName(i.getName()))
                .collect(Collectors.toSet());

        List<ChatHistoryResponse.MessageResponse> messages = chatRoom.getMessages().stream()
                .map(m -> {
                    ChatMessageResponse.AiPayloadResponse aiPayload = null;
                    if (m.getAiPayload() != null && !m.getAiPayload().isBlank()) {
                        try {
                            ChatMessageResponse.AiPayloadResponse parsed =
                                    objectMapper.readValue(m.getAiPayload(), ChatMessageResponse.AiPayloadResponse.class);
                            aiPayload = recalculateMissing(parsed, currentOwnedNames);
                        } catch (Exception e) {
                            log.error("aiPayload 파싱 실패: {}", m.getAiPayload());
                            aiPayload = new ChatMessageResponse.AiPayloadResponse(List.of(), List.of(), List.of(), List.of());
                        }
                    }
                    return new ChatHistoryResponse.MessageResponse(
                            m.getId(), m.getSender().name(), m.getContent(), aiPayload, m.getCreatedAt().toString());
                })
                .collect(Collectors.toList());

        return new ChatHistoryResponse(chatRoom.getTitle(), messages);
    }

    /**
     * 저장된 aiPayload의 missingIngredients를 현재 보유 식재료 기준으로 재계산한다.
     * 소비/폐기된 식재료는 currentOwnedNames에 없으므로 missing에 자동 포함된다.
     */
    private ChatMessageResponse.AiPayloadResponse recalculateMissing(
            ChatMessageResponse.AiPayloadResponse original,
            Set<String> currentOwnedNames
    ) {
        if (original == null || original.recipes() == null || original.recipes().isEmpty()) {
            return original;
        }

        Set<String> recalculated = original.recipes().stream()
                .flatMap(recipe -> recipe.ingredients() == null
                        ? Stream.<String>empty()
                        : recipe.ingredients().stream())
                .map(ChatService::stripQuantityAndParens)
                .filter(ingredient -> !ingredient.isBlank())
                .filter(ingredient -> !isOwnedIngredient(ingredient, currentOwnedNames))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ChatMessageResponse.AiPayloadResponse(
                original.recipes(),
                original.tips(),
                new ArrayList<>(recalculated),
                original.matchedItems()
        );
    }

    @Transactional
    public ChatRoomResponse updateRoomTitle(Long roomId, UpdateRoomTitleRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        chatRoom.updateTitle(request.title());

        return new ChatRoomResponse(chatRoom.getId(), chatRoom.getTitle(), chatRoom.getCreatedAt());
    }

    @Transactional
    public void deleteRoom(Long userId, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatRoom.getUser().getId().equals(userId)) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_OWNED_BY_USER);
        }

        chatRoomRepository.delete(chatRoom);
        chatMemory.clear(String.valueOf(roomId));
    }

    @Transactional
    public ConsumeItemsResponse bulkConsume(Long userId, List<Long> itemIds) {
        List<Long> consumed = new ArrayList<>();
        List<ConsumeItemsResponse.SkippedItem> skipped = new ArrayList<>();
        LocalDate consumedAt = null;

        for (Long itemId : itemIds) {
            if (itemId == null) {
                skipped.add(new ConsumeItemsResponse.SkippedItem(null, "itemId must not be null"));
                continue;
            }
            try {
                ConsumeIngredientUseCase.ConsumeResult result =
                        consumeIngredientUseCase.consume(new ConsumeIngredientUseCase.Command(itemId, userId));
                consumed.add(itemId);
                consumedAt = result.consumedAt();
            } catch (IngredientException e) {
                log.info("bulkConsume skip itemId={} reason={}", itemId, e.getErrorCode().code());
                skipped.add(new ConsumeItemsResponse.SkippedItem(itemId, e.getErrorCode().code()));
            }
        }

        return new ConsumeItemsResponse(consumedAt, consumed, skipped);
    }

}