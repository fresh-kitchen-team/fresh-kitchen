//package com.example.freshkitchen.domain.chat.service;
//
//import com.example.freshkitchen.domain.chat.gemini.GeminiMessageRole;
//import com.example.freshkitchen.domain.chat.gemini.response.GeminiEmbeddingResponse;
//import com.example.freshkitchen.domain.chat.gemini.response.GeminiPart;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//public class EmbeddingService {
//
//    @Value("${gemini.api.key}")
//    private String apiKey;
//
//    private final WebClient webClient;
//
//    public EmbeddingService(WebClient.Builder webClientBuilder) {
//        this.webClient = webClientBuilder
//                .baseUrl("https://generativelanguage.googleapis.com")
//                .build();
//    }
//
//    public float[] embed(String text) {
//        String url = "/v1beta/models/text-embedding-004:embedContent?key=" + apiKey;
//
//        Map<String, Object> request = Map.of(
//                "model", "models/text-embedding-004",
//                "content", Map.of("parts", List.of(Map.of("text", text)))
//        );
//
//        GeminiEmbeddingResponse response = webClient.post()
//                .uri(url)
//                .bodyValue(request)
//                .retrieve()
//                .bodyToMono(GeminiEmbeddingResponse.class)
//                .block();
//
//        List<Float> values = response.getEmbedding().getValues();
//        float[] result = new float[values.size()];
//        for (int i = 0; i < values.size(); i++) {
//            result[i] = values.get(i);
//        }
//        return result;
//    }
//
//    public String toVectorString(float[] embedding) {
//        StringBuilder sb = new StringBuilder("[");
//        for (int i = 0; i < embedding.length; i++) {
//            if (i > 0) sb.append(",");
//            sb.append(embedding[i]);
//        }
//        sb.append("]");
//        return sb.toString();
//    }
//}