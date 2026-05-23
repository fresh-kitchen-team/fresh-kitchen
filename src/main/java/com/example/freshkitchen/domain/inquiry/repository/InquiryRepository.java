package com.example.freshkitchen.domain.inquiry.repository;

import com.example.freshkitchen.domain.inquiry.entity.Inquiry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inquiry i WHERE i.id = :id")
    Optional<Inquiry> findByIdForUpdate(Long id);
}
