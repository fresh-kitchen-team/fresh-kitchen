package com.example.freshkitchen.domain.chat.serivce;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class ETLPipelineService {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;

    @Value("${spring.ai.google.genai.embedding.options.model:NOT_SET}")
    private String configuredModel;

    private static final String TYPE = "recipe";  // ← 고정

    @PostConstruct
    public void logDiagnostics() {
        log.info("========== ETL 진단 정보 ==========");
        log.info("VectorStore 구현체: {}", vectorStore.getClass().getName());
        log.info("EmbeddingModel 구현체: {}", embeddingModel.getClass().getName());
        log.info("설정된 모델명: {}", configuredModel);
        log.info("===================================");
    }

    public String addVectorStore(MultipartFile attach) throws IOException {
        JsonNode rootNode = objectMapper.readTree(attach.getInputStream());
        List<Document> allDocuments = new ArrayList<>();
        if (rootNode.isArray()) {
            rootNode.forEach(node -> allDocuments.add(buildDocument(node)));
        } else {
            allDocuments.add(buildDocument(rootNode));
        }
        if (attach == null || attach.isEmpty()) {
            throw new IOException("유효하지 않은 입력입니다. 업로드 파일이 필요합니다.");
        }
        int totalSize = allDocuments.size();
        log.info("총 문서 수: {}, 임베딩 모델: {}", totalSize, configuredModel);

        // 1건으로 먼저 임베딩만 단독 테스트
        try {
            Document testDoc = allDocuments.get(0);
            log.info("▶ 단독 임베딩 테스트 시작 (vectorStore.add 전에 embeddingModel 직접 호출)");
            float[] embedding = embeddingModel.embed(testDoc);
            log.info("✅ 단독 임베딩 성공! 차원수: {}", embedding.length);
        } catch (Exception e) {
            log.error("❌ 단독 임베딩 실패 — 이 에러가 429의 원인입니다");
            logFullException(e);
        }

        int batchSize = 5;  // 50→5로 축소하여 rate limit 회피


        for (int i = 0; i < totalSize; i += batchSize) {
            int end = Math.min(i + batchSize, totalSize);
            List<Document> batch = allDocuments.subList(i, end);

            // 로직 핵심: 성공할 때까지 혹은 최대 재시도까지 반복
            boolean success = false;
            int retryCount = 0;

            while (!success && retryCount < 5) {
                try {
                    vectorStore.add(batch);
                    success = true;
                } catch (Exception e) {
                    log.error("===== 배치 ({}-{}/{}) 실패 =====", i, end, totalSize);
                    logFullException(e);
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        retryCount++;
                 log.warn("429 에러 발생! {}초 후 다시 시도합니다. (시도: {}/5)", retryCount * 10, retryCount);
                        try {

                            Thread.sleep(retryCount * 10000);

                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        throw e;
                    }
                }
            }

            if (!success) {
                log.error("배치 ({}/{}) 저장 실패 — 5번 재시도 모두 실패. 중단합니다.", end, totalSize);
                throw new RuntimeException("임베딩 저장 실패: 배치 " + i + "-" + end);
            }

            // 정상 처리 후에도 짧은 휴식
            try {

                Thread.sleep(30000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return "저장 완료: " + totalSize + "건";
    }

    private void logFullException(Exception e) {
        log.error("예외 클래스: {}", e.getClass().getName());
        log.error("메시지: {}", e.getMessage());
        Throwable cause = e.getCause();
        int depth = 0;
        while (cause != null && depth < 5) {
            log.error("  cause[{}]: [{}] {}", depth, cause.getClass().getName(), cause.getMessage());
            cause = cause.getCause();
            depth++;
        }
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log.error("전체 스택트레이스:\n{}", sw.toString());
    }

    private Document buildDocument(JsonNode node) {
        String name = node.has("name") ? node.get("name").asText() : "unknown";

        // ingredients 정제
        List<String> ingredients = new ArrayList<>();
        if (node.has("ingredients")) {
            node.get("ingredients").forEach(ingredient -> {
                String cleaned = ingredient.asText()
                        .replaceAll("\\s+", " ")  // 연속 공백 제거
                        .trim();
                ingredients.add(cleaned);
            });
        }

        // steps 정제
        List<String> steps = new ArrayList<>();
        if (node.has("steps")) {
            node.get("steps").forEach(step -> {
                String cleaned = step.asText().trim();
                steps.add(cleaned);
            });
        }

        // 정제된 내용으로 content 구성
        String content = String.format("""
            레시피명: %s
            재료: %s
            조리순서: %s
            """,
                name,
                String.join(", ", ingredients),
                String.join(" ", steps)
        );


        return Document.builder()
                .text(content)
                .metadata(Map.of(
                        "type", TYPE,
                        "name", name
                ))
                .build();
    }
}