package com.example.freshkitchen.domain.user.repository;

import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    boolean existsByProviderAndProviderUserId(Provider provider, String providerUserId);

    @Query("""
            select distinct user
            from User user
            left join fetch user.profile profile
            left join fetch profile.preferredIngredients
            left join fetch profile.foodStyles
            left join fetch profile.allergies
            left join fetch profile.cookingTools
            where user.id = :userId
            """)
    Optional<User> findByIdWithProfile(@Param("userId") Long userId);
}
