package com.example.freshkitchen.application.inquiry.usecase;

public interface ReplyInquiryUseCase {

    void reply(Command command);

    record Command(Long inquiryId, String reply) {
    }
}
