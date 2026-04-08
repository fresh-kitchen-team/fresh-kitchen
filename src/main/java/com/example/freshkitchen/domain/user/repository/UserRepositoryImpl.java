package com.example.freshkitchen.domain.user.repository;

import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.entity.UserProfile;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
class UserRepositoryImpl implements UserRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByIdWithProfile(Long userId) {
        Optional<User> user = entityManager.createQuery("""
                select user
                from User user
                left join fetch user.profile profile
                where user.id = :userId
                """, User.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .findFirst();

        user.map(User::getProfile)
                // Loading the collections separately avoids a cartesian-product query
                // across the four element collections on UserProfile.
                .ifPresent(this::initializeProfileCollections);

        return user;
    }

    private void initializeProfileCollections(UserProfile profile) {
        Hibernate.initialize(profile.getPreferredIngredients());
        Hibernate.initialize(profile.getFoodStyles());
        Hibernate.initialize(profile.getAllergies());
        Hibernate.initialize(profile.getCookingTools());
    }
}
