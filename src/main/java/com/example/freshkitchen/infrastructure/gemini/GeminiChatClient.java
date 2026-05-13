package com.example.freshkitchen.infrastructure.gemini;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring AI ChatClient for Gemini API calls.
 * Applies RAG advisor for ingredient-aware responses.
 */
@Slf4j
@Component
public class GeminiChatClient {

    private final ChatClient chatClient;
    private final RetrievalAugmentationAdvisor ragAdvisor;

    public GeminiChatClient(ChatClient chatClient, RetrievalAugmentationAdvisor ragAdvisor) {
        this.chatClient = chatClient;
        this.ragAdvisor = ragAdvisor;
    }

    /**
     * Send a prompt to Gemini with RAG context and return the response text.
     */
    public String chat(String prompt) {
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .advisors(ragAdvisor)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("RAG chat error [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    /**
     * Send a prompt to Gemini without RAG (simple chat).
     */
    public String chatWithoutRag(String prompt) {
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Simple chat error [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}

