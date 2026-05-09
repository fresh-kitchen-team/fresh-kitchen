package com.example.freshkitchen.domain.user.repository;

import com.example.freshkitchen.domain.user.entity.User;

import java.util.Optional;

public interface UserRepositoryCustom {

    Optional<User> findByIdWithProfile(Long userId);
}
