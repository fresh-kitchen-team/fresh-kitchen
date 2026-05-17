package com.example.freshkitchen.application.inquiry.usecase;

public interface SendInquiryUseCase {

    void send(Command command);

    record Command(
            Long userId,
            String title,
            String content,
            String contactEmail
    ) {
    }
}
