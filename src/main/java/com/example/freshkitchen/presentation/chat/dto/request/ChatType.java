package com.example.freshkitchen.presentation.chat.dto.request;

public enum ChatType {
    RECIPE("recipe"),
    GENERAL(null);

    private final String vectorStoreType;

    ChatType(String vectorStoreType) {
        this.vectorStoreType = vectorStoreType;
    }

    public String vectorStoreType() {
        return vectorStoreType;
    }
}