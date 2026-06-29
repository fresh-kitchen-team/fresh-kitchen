package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.security.Role;

public record TokenPayload(Long userId, Role role) {
}
