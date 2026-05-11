package com.example.freshkitchen.infrastructure.gemini;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring AI ChatClient for Gemini API calls.
 * Applies RAG advisor for ingredient-aware responses.
 */
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
        return chatClient.prompt()
                .user(prompt)
                .advisors(ragAdvisor)
                .call()
                .content();
    }

    /**
     * Send a prompt to Gemini without RAG (simple chat).
     */
    public String chatWithoutRag(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
