package com.example.freshkitchen.domain.chat.service;


import com.example.freshkitchen.domain.chat.dto.request.ChatMessageRequest;
import com.example.freshkitchen.domain.chat.dto.request.UpdateRoomTitleRequest;
import com.example.freshkitchen.domain.chat.dto.response.ChatHistoryResponse;
import com.example.freshkitchen.domain.chat.dto.response.ChatMessageResponse;
import com.example.freshkitchen.domain.chat.dto.response.ChatRoomListResponse;
import com.example.freshkitchen.domain.chat.dto.response.ChatRoomResponse;
import com.example.freshkitchen.domain.chat.entity.AiSetting;
import com.example.freshkitchen.domain.chat.entity.ChatMessage;
import com.example.freshkitchen.domain.chat.entity.ChatRoom;
import com.example.freshkitchen.domain.chat.enums.Sender;
import com.example.freshkitchen.domain.chat.exception.ChatErrorCode;
import com.example.freshkitchen.domain.chat.exception.ChatException;
import com.example.freshkitchen.domain.chat.repository.AiSettingRepository;
import com.example.freshkitchen.domain.chat.repository.ChatRoomRepository;
import com.example.freshkitchen.domain.chat.repository.MessageRepository;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.aop.Advisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final AiSettingRepository aiSettingRepository;
    private final IngredientRepository ingredientRepository;

    public ChatService(ObjectMapper objectMapper, ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
                       UserRepository userRepository, ChatRoomRepository chatRoomRepository,
                       MessageRepository messageRepository, AiSettingRepository aiSettingRepository,
                       IngredientRepository ingredientRepository) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
        this.aiSettingRepository = aiSettingRepository;
        this.ingredientRepository = ingredientRepository;

        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "[SimpleLoggerAdvisor] Custom request: " + request.prompt().getUserMessage(),
                response -> "[SimpleLoggerAdvisor] Custom response: " + response.getResult(),
                Ordered.HIGHEST_PRECEDENCE);

        retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(chatClientBuilder)
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .topK(2)
                        .similarityThreshold(0.6)
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(customLogger)
                .build();
    }

    @Transactional
    public ChatRoomResponse createChatRoom(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
                .user(user)
                .title("AI 요리비서")
                .build();

        ChatRoom saved = chatRoomRepository.save(chatRoom);

        return new ChatRoomResponse(saved.getId(), saved.getTitle(), saved.getCreatedAt());
    }

    public Flux<String> ragChat(String question, String type, String conversationId) {
        return this.chatClient.prompt()
                .system("친절하게 한국어로 답변해줘.")
                .user(question)
                .advisors(retrievalAugmentationAdvisor)
                .stream()
                .content();
    }

    private String buildSystemPrompt(AiSetting setting, String userIngredients) {
        StringBuilder sb = new StringBuilder();
        sb.append("친절하게 한국어로 답변해줘.\n");

        if (userIngredients != null && !userIngredients.isBlank()) {
            sb.append("사용자가 현재 보유한 재료: ").append(userIngredients).append("\n");
            sb.append("보유 재료를 최대한 활용한 레시피를 추천해줘.\n");
            sb.append("레시피에 필요하지만 보유하지 않은 재료는 missingIngredients에 넣어줘.\n");
        } else {
            sb.append("보유 재료 정보가 없으니 일반적인 레시피를 추천해줘.\n");
            sb.append("missingIngredients는 빈 배열로 반환해줘.\n");
        }

        if (setting != null && setting.getResponseStyle() != null) {
            if (setting.getResponseStyle().equals("간단")) {
                sb.append("답변은 간결하고 핵심만 전달해줘.\n");
            } else {
                sb.append("답변은 친절하고 자세하게 설명해줘.\n");
            }
        }

        if (setting != null && setting.isPriorityExpiration()) {
            sb.append("유통기한이 임박한 재료를 우선적으로 사용하는 레시피를 추천해줘.\n");
        }
        if (setting != null && setting.isPriorityNutrition()) {
            sb.append("영양 균형을 고려한 레시피를 추천해줘.\n");
        }
        if (setting != null && setting.isPriorityFrequent()) {
            sb.append("자주 사용하는 재료를 우선으로 활용한 레시피를 추천해줘.\n");
        }
        if (setting != null && setting.isProvideExtraInfo()) {
            sb.append("레시피 외에도 관련 영양 정보, 보관 방법, 요리 팁 등 추가 정보도 함께 제공해줘.\n");
        }

        sb.append("""
        반드시 아래 JSON 형식으로만 응답해. 마크다운 코드블록 없이 순수 JSON만 반환해:
        {
          "recipes": [
            {
              "name": "레시피명",
              "ingredients": ["재료1", "재료2"],
              "steps": ["단계1", "단계2"],
              "time": "30분"
            }
          ],
          "tips": ["팁1", "팁2"],
          "missingIngredients": ["부족한재료1", "부족한재료2"]
        }
        """);

        return sb.toString();
    }

    // ✅ [추가] 첫 메시지 기반 채팅방 제목 자동 생성
    private String generateRoomTitle(String userMessage) {
        String titlePrompt = """
                다음 메시지를 보고 채팅방 제목을 10자 이내로 만들어줘.
                마크다운, 따옴표, 특수문자 없이 순수 텍스트로만 반환해.
                메시지: %s
                """.formatted(userMessage);

        String title = this.chatClient.prompt()
                .user(titlePrompt)
                .call()
                .content();

        return title != null ? title.trim() : "AI 요리비서";
    }

    @Transactional
    public ChatMessageResponse sendAiMessage(Long userId, Long roomId, ChatMessageRequest request) {

        List<Ingredient> userIngredients = ingredientRepository.findByUserIdAndStatus(userId, IngredientStatus.ACTIVE);
        String ingredientList = userIngredients.stream()
                .map(Ingredient::getName)
                .collect(Collectors.joining(", "));

        Set<String> ownedIngredientNames = userIngredients.stream()
                .map(i -> i.getName().trim().toLowerCase())
                .collect(Collectors.toSet());

        AiSetting setting = aiSettingRepository.findByUserId(userId).orElse(null);
        String systemPrompt = buildSystemPrompt(setting, ingredientList);

        String aiResponseJson;
        try {
            aiResponseJson = this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.message())
                    .advisors(retrievalAugmentationAdvisor)
                    .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION,
                            "type == '%s'".formatted(request.type())))
                    .call()
                    .content();
        } catch (ChatException e) {
            throw new ChatException(ChatErrorCode.GEMINI_QUOTA_EXCEEDED);
        }

        String cleanJson = aiResponseJson
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("```", "")
                .trim();

        ObjectMapper objectMapper = new ObjectMapper();
        ChatMessageResponse.AiPayloadResponse aiPayload;
        try {
            ChatMessageResponse.AiPayloadResponse parsed = objectMapper.readValue(cleanJson, ChatMessageResponse.AiPayloadResponse.class);

            Set<String> missingIngredients = parsed.recipes().stream()
                    .flatMap(recipe -> recipe.ingredients().stream())
                    .map(ingredient -> ingredient.replaceAll("\\s*[\\d/.]+\\s*(g|kg|ml|l|개|컵|큰술|작은술|줌|마리|장|포기|모|대|쪽|알|봉|캔|팩)?.*$", "").trim())
                    .filter(ingredient -> !ingredient.isBlank())
                    .filter(ingredient -> !ownedIngredientNames.contains(ingredient.toLowerCase()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            aiPayload = new ChatMessageResponse.AiPayloadResponse(
                    parsed.recipes(),
                    parsed.tips(),
                    new ArrayList<>(missingIngredients));

        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", cleanJson);
            aiPayload = new ChatMessageResponse.AiPayloadResponse(List.of(), List.of(), List.of());
        }

        String finalJson;
        try {
            finalJson = objectMapper.writeValueAsString(aiPayload);
        } catch (Exception e) {
            log.error("aiPayload 직렬화 실패");
            finalJson = cleanJson;
        }

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .user(chatRoom.getUser())
                .content(finalJson)
                .sender(Sender.AI)
                .aiPayload(finalJson)
                .build();

        ChatMessage saved = messageRepository.save(chatMessage);

        long messageCount = messageRepository.countByChatRoomId(roomId);
        if (messageCount == 1) {
            String autoTitle = generateRoomTitle(request.message());
            chatRoom.updateTitle(autoTitle);
            chatRoomRepository.save(chatRoom);
        }

        return new ChatMessageResponse(
                chatRoom.getTitle(),
                new ChatMessageResponse.AiMessageResponse(
                        saved.getId(), "AI", finalJson, aiPayload, saved.getCreatedAt().toString()));
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

        List<ChatHistoryResponse.MessageResponse> messages = chatRoom.getMessages().stream()
                .map(m -> {
                    ChatMessageResponse.AiPayloadResponse aiPayload = null;
                    if (m.getAiPayload() != null && !m.getAiPayload().isBlank()) {
                        try {
                            aiPayload = objectMapper.readValue(m.getAiPayload(), ChatMessageResponse.AiPayloadResponse.class);
                        } catch (Exception e) {
                            log.error("aiPayload 파싱 실패: {}", m.getAiPayload());
                            aiPayload = new ChatMessageResponse.AiPayloadResponse(List.of(), List.of(), List.of());
                        }
                    }
                    return new ChatHistoryResponse.MessageResponse(
                            m.getId(), m.getSender().name(), m.getContent(), aiPayload, m.getCreatedAt().toString());
                })
                .collect(Collectors.toList());

        return new ChatHistoryResponse(chatRoom.getTitle(), messages);
    }

    @Transactional
    public ChatRoomResponse updateRoomTitle(Long roomId, UpdateRoomTitleRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        chatRoom.updateTitle(request.title());

        return new ChatRoomResponse(chatRoom.getId(), chatRoom.getTitle(), chatRoom.getCreatedAt());
    }
}