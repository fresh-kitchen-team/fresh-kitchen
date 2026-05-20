package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.enums.UserStatus;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

/**
 * OAuth 로그인 시 유저 조회/생성/재활성화를 처리합니다.
 * <p>
 * 각 DB 작업은 {@code REQUIRES_NEW} 전파 옵션의 {@link TransactionTemplate}으로
 * 독립 트랜잭션을 보장하여, 동시 회원가입 충돌(DataIntegrityViolationException) 발생 시
 * PostgreSQL의 aborted-transaction 상태가 후속 쿼리에 영향을 주지 않습니다.
 */
@Slf4j
@Component
public class OAuthUserResolver {

    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public OAuthUserResolver(UserRepository userRepository, PlatformTransactionManager txManager) {
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(txManager);
        this.transactionTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    }

    public Result resolve(String providerUserId, Provider provider) {
        // 1차: 기존 유저 조회 (독립 트랜잭션)
        User existing = transactionTemplate.execute(status ->
                userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                        .orElse(null)
        );

        if (existing != null) {
            return handleExistingUser(existing, provider, providerUserId);
        }

        // 2차: 신규 유저 생성 시도 (독립 트랜잭션)
        try {
            User created = transactionTemplate.execute(status ->
                    userRepository.saveAndFlush(
                            User.create(new User.CreateCommand(providerUserId, provider))
                    )
            );
            return new Result(created, true);
        } catch (DataIntegrityViolationException e) {
            log.warn("동시 회원가입 충돌 감지, 새 트랜잭션으로 재조회. provider={}, providerUserId={}",
                    provider, providerUserId, e);
        }

        // 3차: 충돌 후 재조회 (별도 독립 트랜잭션 — aborted 세션 영향 없음)
        User recovered = transactionTemplate.execute(status ->
                userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                        .orElseThrow(() -> new IllegalStateException(
                                "동시 가입 충돌 후 유저 재조회 실패. provider=" + provider
                                        + ", providerUserId=" + providerUserId))
        );
        return new Result(recovered, false);
    }

    private Result handleExistingUser(User user, Provider provider, String providerUserId) {
        if (user.getStatus() == UserStatus.INACTIVE) {
            User reactivated = transactionTemplate.execute(status -> {
                User managed = userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                        .orElseThrow(() -> new IllegalStateException(
                                "재활성화 대상 유저 조회 실패. provider=" + provider
                                        + ", providerUserId=" + providerUserId));
                managed.reactivate();
                return managed;
            });
            return new Result(reactivated, false);
        }
        return new Result(user, false);
    }

    public record Result(User user, boolean isNew) {
    }
}
