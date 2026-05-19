package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.DeleteUserUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteUserService implements DeleteUserUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EntityManager entityManager;

    @Value("${dev.hard-delete.enabled:false}")
    private boolean hardDeleteEnabled;

    @Override
    public void delete(Command command) {
        Long userId = requireUserId(command);

        if (command.hard()) {
            hardDelete(userId);
        } else {
            softDelete(userId);
        }
    }

    private void softDelete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        user.deactivate();
        refreshTokenRepository.deleteByUserId(userId);
    }

    private void hardDelete(Long userId) {
        if (!hardDeleteEnabled) {
            throw new BusinessValidationException("hard delete is disabled in this environment");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // FK 의존 순서대로 벌크 삭제
        entityManager.createQuery("DELETE FROM ChatMessage m WHERE m.room.id IN (SELECT r.id FROM ChatRoom r WHERE r.user.id = :userId)")
                .setParameter("userId", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM ChatRoom r WHERE r.user.id = :userId")
                .setParameter("userId", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM AiSetting a WHERE a.user.id = :userId")
                .setParameter("userId", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM IngredientImage ii WHERE ii.ingredient.id IN (SELECT i.id FROM Ingredient i WHERE i.user.id = :userId)")
                .setParameter("userId", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM Ingredient i WHERE i.user.id = :userId")
                .setParameter("userId", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM ImageVariant v WHERE v.imageAsset.id IN (SELECT a.id FROM ImageAsset a WHERE a.user.id = :userId)")
                .setParameter("userId", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM ImageAsset a WHERE a.user.id = :userId")
                .setParameter("userId", userId).executeUpdate();
        entityManager.createQuery("DELETE FROM Storage s WHERE s.user.id = :userId")
                .setParameter("userId", userId).executeUpdate();

        userRepository.delete(user); // UserProfile은 cascade로 자동 삭제
        refreshTokenRepository.deleteByUserId(userId);
    }

    private static Long requireUserId(Command command) {
        if (command == null || command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        return command.userId();
    }
}
