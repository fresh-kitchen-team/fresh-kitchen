package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.HardDeleteUserUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class HardDeleteUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);

    @Test
    void delete_throwsHardDeleteDisabled_whenFeatureIsOff() {
        HardDeleteUserService service = createService(false);

        assertThatThrownBy(() -> service.delete(new HardDeleteUserUseCase.Command(1L)))
                .isInstanceOf(UserException.class)
                .satisfies(ex -> assertThat(((UserException) ex).getErrorCode())
                        .isEqualTo(UserErrorCode.HARD_DELETE_DISABLED));

        then(userRepository).should(never()).findById(1L);
    }

    @Test
    void delete_throwsBusinessValidationException_whenCommandIsNull() {
        HardDeleteUserService service = createService(true);

        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void delete_throwsBusinessValidationException_whenUserIdIsNull() {
        HardDeleteUserService service = createService(true);

        assertThatThrownBy(() -> service.delete(new HardDeleteUserUseCase.Command(null)))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void delete_throwsUserNotFound_whenUserDoesNotExist() {
        HardDeleteUserService service = createService(true);
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(new HardDeleteUserUseCase.Command(99L)))
                .isInstanceOf(UserException.class)
                .satisfies(ex -> assertThat(((UserException) ex).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    @Test
    void delete_executesAllBulkDeletesAndRemovesUser() {
        HardDeleteUserService service = createService(true);

        User user = User.create(new User.CreateCommand("sub", Provider.GOOGLE));
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        Query mockQuery = mock(Query.class);
        given(mockQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).willReturn(mockQuery);
        given(mockQuery.executeUpdate()).willReturn(0);
        given(entityManager.createQuery(anyString())).willReturn(mockQuery);

        service.delete(new HardDeleteUserUseCase.Command(1L));

        then(userRepository).should().delete(user);
        then(refreshTokenRepository).should().deleteByUserId(1L);
    }

    private HardDeleteUserService createService(boolean enabled) {
        HardDeleteUserService service = new HardDeleteUserService(
                userRepository, refreshTokenRepository, entityManager
        );
        ReflectionTestUtils.setField(service, "hardDeleteEnabled", enabled);
        return service;
    }
}
