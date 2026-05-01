package com.example.freshkitchen.global.exception.config;


import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Spring Security의 UserDetails를 상속하여 userId를 추가로 저장하는 커스텀 클래스입니다.
 */
@Getter
public class CustomUserDetails extends User {

    private final Long userId;

    public CustomUserDetails(
            Long userId,
            String username,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, "", authorities);
        this.userId = userId;
    }
}
