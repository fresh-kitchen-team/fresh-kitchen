package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.DeleteUserUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.enums.UserStatus;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class DeleteUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);

    private final DeleteUserService service = new DeleteUserService(userRepository, refreshTokenRepository);

    @Test
    void delete_deactivatesUserAndDeletesRefreshToken() {
        User user = User.create(new User.CreateCommand("provider-id", Provider.GOOGLE));
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        service.delete(new DeleteUserUseCase.Command(1L));

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        then(refreshTokenRepository).should().deleteByUserId(1L);
    }

    @Test
    void delete_throwsUserException_whenUserNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(new DeleteUserUseCase.Command(99L)))
                .isInstanceOf(UserException.class);
    }

    @Test
    void delete_throwsBusinessValidationException_whenCommandIsNull() {
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void delete_throwsBusinessValidationException_whenUserIdIsNull() {
        assertThatThrownBy(() -> service.delete(new DeleteUserUseCase.Command(null)))
                .isInstanceOf(BusinessValidationException.class);
    }
}
