package com.example.freshkitchen.application.user.service;

import com.example.freshkitchen.application.user.usecase.HardDeleteUserUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HardDeleteUserService implements HardDeleteUserUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EntityManager entityManager;
    private final boolean hardDeleteEnabled;

    public HardDeleteUserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            EntityManager entityManager,
            @Value("${dev.hard-delete.enabled:false}") boolean hardDeleteEnabled
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.entityManager = entityManager;
        this.hardDeleteEnabled = hardDeleteEnabled;
    }

    @Override
    public void delete(Command command) {
        if (!hardDeleteEnabled) {
            throw new UserException(UserErrorCode.HARD_DELETE_DISABLED);
        }

        if (command == null || command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        Long userId = command.userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

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

        userRepository.delete(user);
        refreshTokenRepository.deleteByUserId(userId);
    }
}
