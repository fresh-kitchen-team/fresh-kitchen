package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.enums.UserStatus;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class OAuthUserResolverTest {

    private final UserRepository userRepository = mock(UserRepository.class);

    private final OAuthUserResolver resolver = createResolver();

    private OAuthUserResolver createResolver() {
        // TransactionTemplate that just executes the callback directly (no real TX)
        TransactionTemplate txTemplate = new TransactionTemplate() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }
        };
        OAuthUserResolver r = new OAuthUserResolver(userRepository, mock(org.springframework.transaction.PlatformTransactionManager.class));
        ReflectionTestUtils.setField(r, "transactionTemplate", txTemplate);
        return r;
    }

    @Test
    void resolve_returnsExistingUser_whenFound() {
        User existing = User.create(new User.CreateCommand("sub-123", Provider.GOOGLE));
        ReflectionTestUtils.setField(existing, "id", 1L);
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "sub-123"))
                .willReturn(Optional.of(existing));

        OAuthUserResolver.Result result = resolver.resolve("sub-123", Provider.GOOGLE);

        assertThat(result.user()).isEqualTo(existing);
        assertThat(result.isNew()).isFalse();
    }

    @Test
    void resolve_createsNewUser_whenNotFound() {
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "new-sub"))
                .willReturn(Optional.empty());

        User savedUser = User.create(new User.CreateCommand("new-sub", Provider.GOOGLE));
        ReflectionTestUtils.setField(savedUser, "id", 2L);
        given(userRepository.saveAndFlush(any(User.class))).willReturn(savedUser);

        OAuthUserResolver.Result result = resolver.resolve("new-sub", Provider.GOOGLE);

        assertThat(result.user().getId()).isEqualTo(2L);
        assertThat(result.isNew()).isTrue();
    }

    @Test
    void resolve_recoversFromConcurrentSignup() {
        given(userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "race-sub"))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(createUserWithId("race-sub", Provider.KAKAO, 3L)));

        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("unique constraint"));

        OAuthUserResolver.Result result = resolver.resolve("race-sub", Provider.KAKAO);

        assertThat(result.user().getId()).isEqualTo(3L);
        assertThat(result.isNew()).isFalse();
    }

    @Test
    void resolve_reactivatesInactiveUser() {
        User inactive = User.create(new User.CreateCommand("inactive-sub", Provider.GOOGLE));
        ReflectionTestUtils.setField(inactive, "id", 4L);
        inactive.deactivate();

        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "inactive-sub"))
                .willReturn(Optional.of(inactive));

        OAuthUserResolver.Result result = resolver.resolve("inactive-sub", Provider.GOOGLE);

        assertThat(result.user().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.isNew()).isFalse();
    }

    @Test
    void resolve_throwsIllegalState_whenRecoveryFails() {
        given(userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "ghost-sub"))
                .willReturn(Optional.empty())
                .willReturn(Optional.empty());

        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> resolver.resolve("ghost-sub", Provider.KAKAO))
                .isInstanceOf(IllegalStateException.class);
    }

    private User createUserWithId(String providerUserId, Provider provider, Long id) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
