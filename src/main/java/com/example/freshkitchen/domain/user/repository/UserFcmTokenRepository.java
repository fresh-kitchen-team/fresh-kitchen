package com.example.freshkitchen.domain.user.repository;

import com.example.freshkitchen.domain.user.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    Optional<UserFcmToken> findByTokenValue(String tokenValue);

    List<UserFcmToken> findByUser_IdIn(Collection<Long> userIds);

    void deleteByTokenValueIn(Collection<String> tokenValues);
}