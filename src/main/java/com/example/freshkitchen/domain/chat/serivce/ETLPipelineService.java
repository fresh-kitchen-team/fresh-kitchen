package com.example.freshkitchen.domain.chat.serivce;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class ETLPipelineService {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    private static final String TYPE = "recipe";  // ← 고정

    public String addVectorStore(MultipartFile attach) throws IOException {
        log.info("=== addVectorStore 시작 ===");

        if (attach == null || attach.isEmpty()) {
            throw new IOException("유효하지 않은 입력입니다. 업로드 파일이 필요합니다.");
        }

        JsonNode rootNode = objectMapper.readTree(attach.getInputStream());
        List<Document> allDocuments = new ArrayList<>();
        if (rootNode.isArray()) {
            rootNode.forEach(node -> allDocuments.add(buildDocument(node)));
        } else {
            allDocuments.add(buildDocument(rootNode));
        }

        log.info("파싱된 문서 수: {}건", allDocuments.size());

        int totalSize = allDocuments.size();
        int batchSize = 50;

        for (int i = 0; i < totalSize; i += batchSize) {
            int end = Math.min(i + batchSize, totalSize);
            List<Document> batch = allDocuments.subList(i, end);

            log.info("배치 처리 중: {}/{} ({}~{}번째)", end, totalSize, i + 1, end);

            boolean success = false;
            int retryCount = 0;

            while (!success && retryCount < 5) {
                try {
                    log.info("vectorStore.add() 호출 - 배치 크기: {}", batch.size());
                    vectorStore.add(batch);
                    log.info("vectorStore.add() 성공! ({}/{})", end, totalSize);
                    success = true;
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        retryCount++;
                        log.warn("429 에러 발생! {}초 후 다시 시도합니다. (시도: {}/5)", retryCount * 10, retryCount);
                        try {
                            Thread.sleep(retryCount * 10000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        log.error("vectorStore.add() 실패 - 에러: {}", e.getMessage(), e);
                        throw e;
                    }
                }
            }

            if (!success) {
                log.error("배치 ({}/{}) 저장 실패 - 5번 재시도 모두 실패", end, totalSize);
            }

            if (i + batchSize < totalSize) {
                log.info("다음 배치 전 30초 대기...");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("=== addVectorStore 완료: {}건 저장 ===", totalSize);
        return "저장 완료: " + totalSize + "건";
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