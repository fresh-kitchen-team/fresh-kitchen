package com.example.freshkitchen.domain.chat.service;


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
        JsonNode rootNode = objectMapper.readTree(attach.getInputStream());
        List<Document> allDocuments = new ArrayList<>();
        if (rootNode.isArray()) {
            rootNode.forEach(node -> allDocuments.add(buildDocument(node)));
        } else {
            allDocuments.add(buildDocument(rootNode));
        }

        int totalSize = allDocuments.size();
// 수정: 배치 사이즈를 더 작게 줄입니다 (5~10 권장)
        int batchSize = 10;

        for (int i = 0; i < totalSize; i += batchSize) {
            int end = Math.min(i + batchSize, totalSize);
            List<Document> batch = allDocuments.subList(i, end);

            // 로직 핵심: 성공할 때까지 혹은 최대 재시도까지 반복
            boolean success = false;
            int retryCount = 0;

            while (!success && retryCount < 3) {
                try {
                    vectorStore.add(batch);
                    success = true;
                    log.info("성공: ({}/{})", end, totalSize);
                } catch (Exception e) {
                    if (e.getMessage().contains("429")) {
                        retryCount++;
                        log.warn("429 에러 발생! {}초 후 다시 시도합니다. (시도: {}/3)", retryCount * 30, retryCount);
                        try {
                            // 에러 발생 시 대기 시간을 더 길게 (30초, 60초...)
                            Thread.sleep(retryCount * 30000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        throw e; // 429 외의 에러는 중단
                    }
                }
            }

            // 정상 처리 후에도 짧은 휴식
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

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

        log.info("content={}", content);

        return Document.builder()
                .text(content)
                .metadata(Map.of(
                        "type", TYPE,
                        "name", name
                ))
                .build();
    }
}