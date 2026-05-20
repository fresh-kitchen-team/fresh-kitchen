package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.enums.UserStatus;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 로그인 시 유저 조회/생성/재활성화를 독립 트랜잭션으로 처리합니다.
 * <p>
 * 동시 회원가입 충돌(DataIntegrityViolationException) 발생 시
 * rollback-only 트랜잭션이 호출자에게 전파되지 않도록 REQUIRES_NEW로 격리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthUserResolver {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result resolve(String providerUserId, Provider provider) {
        return userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(user -> handleExistingUser(user))
                .orElseGet(() -> createNewUser(providerUserId, provider));
    }

    private Result handleExistingUser(User user) {
        if (user.getStatus() == UserStatus.INACTIVE) {
            user.reactivate();
        }
        return new Result(user, false);
    }

    private Result createNewUser(String providerUserId, Provider provider) {
        try {
            User user = userRepository.saveAndFlush(
                    User.create(new User.CreateCommand(providerUserId, provider))
            );
            return new Result(user, true);
        } catch (DataIntegrityViolationException e) {
            log.warn("동시 회원가입 충돌 감지, 기존 유저 재조회. provider={}, providerUserId={}",
                    provider, providerUserId, e);
            User user = userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                    .orElseThrow(() -> e);
            return new Result(user, false);
        }
    }

    public record Result(User user, boolean isNew) {
    }
}
