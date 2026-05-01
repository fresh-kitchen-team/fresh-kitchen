package com.example.freshkitchen.domain.user.repository;

import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    Optional<User> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    boolean existsByProviderAndProviderUserId(Provider provider, String providerUserId);
}
