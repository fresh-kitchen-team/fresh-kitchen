package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.PrematureJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final int CLOCK_SKEW_SECONDS = 30;
    private static final MacAlgorithm SIGNATURE_ALGORITHM = Jwts.SIG.HS256;

    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-minutes}") long accessExpirationMinutes,
            @Value("${jwt.refresh-expiration-days}") long refreshExpirationDays
    ) {
        Assert.hasText(secret, "jwt.secret must not be blank");
        Assert.isTrue(
                secret.getBytes(StandardCharsets.UTF_8).length >= MINIMUM_SECRET_BYTES,
                "jwt.secret must be at least 32 bytes for HMAC-SHA256"
        );
        Assert.isTrue(accessExpirationMinutes > 0, "jwt.access-expiration-minutes must be positive");
        Assert.isTrue(refreshExpirationDays > 0, "jwt.refresh-expiration-days must be positive");
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser().verifyWith(this.secretKey).clockSkewSeconds(CLOCK_SKEW_SECONDS).build();
        this.accessExpirationMillis = Duration.ofMinutes(accessExpirationMinutes).toMillis();
        this.refreshExpirationMillis = Duration.ofDays(refreshExpirationDays).toMillis();
    }

    public String generateAccessToken(Long userId, Role role) {
        validateUserId(userId);
        if (role == null) {
            throw new BusinessValidationException("role must not be null");
        }
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessExpirationMillis);

        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, SIGNATURE_ALGORITHM)
                .compact();
    }

    public String generateDevAccessToken(Long userId, Role role, long expirationMinutes) {
        validateUserId(userId);
        if (role == null) {
            throw new BusinessValidationException("role must not be null");
        }
        Date now = new Date();
        Date expiration = new Date(now.getTime() + Duration.ofMinutes(expirationMinutes).toMillis());

        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, SIGNATURE_ALGORITHM)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        validateUserId(userId);
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshExpirationMillis);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, SIGNATURE_ALGORITHM)
                .compact();
    }

    public TokenPayload validateAccessToken(String token) {
        Claims claims = parseClaims(token);
        assertTokenType(claims, TOKEN_TYPE_ACCESS);
        Role role = extractRole(claims);
        Long userId = extractUserId(claims);
        return new TokenPayload(userId, role);
    }

    public Long validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        assertTokenType(claims, TOKEN_TYPE_REFRESH);
        return extractUserId(claims);
    }

    private void assertTokenType(Claims claims, String expectedType) {
        Object tokenTypeClaim = claims.get(CLAIM_TOKEN_TYPE);
        if (!(tokenTypeClaim instanceof String tokenType) || !tokenType.equals(expectedType)) {
            throw new JwtTokenException(JwtErrorCode.MALFORMED_TOKEN);
        }
    }

    private Role extractRole(Claims claims) {
        Object roleClaim = claims.get(CLAIM_ROLE);
        if (!(roleClaim instanceof String roleStr) || roleStr.isBlank()) {
            throw new JwtTokenException(JwtErrorCode.MALFORMED_TOKEN);
        }
        try {
            return Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new JwtTokenException(JwtErrorCode.MALFORMED_TOKEN, e);
        }
    }

    private Long extractUserId(Claims claims) {
        Object userIdClaim = claims.get(CLAIM_USER_ID);
        long id;
        if (userIdClaim instanceof Long l) {
            id = l;
        } else if (userIdClaim instanceof Integer i) {
            id = i.longValue();
        } else {
            throw new JwtTokenException(JwtErrorCode.MALFORMED_TOKEN);
        }
        if (id <= 0) {
            throw new JwtTokenException(JwtErrorCode.MALFORMED_TOKEN);
        }
        return id;
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        if (userId <= 0) {
            throw new BusinessValidationException("userId must be positive");
        }
    }

    private Claims parseClaims(String token) {
        try {
            Claims claims = jwtParser
                    .parseSignedClaims(token)
                    .getPayload();
            if (claims.getExpiration() == null) {
                throw new JwtTokenException(JwtErrorCode.MALFORMED_TOKEN);
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new JwtTokenException(JwtErrorCode.EXPIRED_TOKEN, e);
        } catch (PrematureJwtException e) {
            throw new JwtTokenException(JwtErrorCode.NOT_YET_VALID_TOKEN, e);
        } catch (SignatureException e) {
            throw new JwtTokenException(JwtErrorCode.INVALID_SIGNATURE, e);
        } catch (MalformedJwtException e) {
            throw new JwtTokenException(JwtErrorCode.MALFORMED_TOKEN, e);
        } catch (UnsupportedJwtException e) {
            throw new JwtTokenException(JwtErrorCode.UNSUPPORTED_TOKEN, e);
        } catch (JwtException e) {
            throw new JwtTokenException(JwtErrorCode.MALFORMED_TOKEN, e);
        } catch (IllegalArgumentException e) {
            throw new JwtTokenException(JwtErrorCode.EMPTY_CLAIMS, e);
        }
    }
}
