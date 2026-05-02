package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.security.exception.JwtException;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final int MINIMUM_SECRET_BYTES = 32;
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
        this.jwtParser = Jwts.parser().verifyWith(this.secretKey).build();
        this.accessExpirationMillis = Duration.ofMinutes(accessExpirationMinutes).toMillis();
        this.refreshExpirationMillis = Duration.ofDays(refreshExpirationDays).toMillis();
    }

    public String generateAccessToken(Long userId, String role) {
        Assert.notNull(userId, "userId must not be null");
        Assert.hasText(role, "role must not be blank");
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessExpirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, SIGNATURE_ALGORITHM)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Assert.notNull(userId, "userId must not be null");
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshExpirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, SIGNATURE_ALGORITHM)
                .compact();
    }

    public void validateToken(String token) {
        parseClaims(token);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        Object userIdClaim = claims.get(CLAIM_USER_ID);
        if (userIdClaim instanceof Long id) {
            return id;
        }
        if (userIdClaim instanceof Integer id) {
            return id.longValue();
        }
        throw new JwtException(JwtErrorCode.MALFORMED_TOKEN);
    }

    private Claims parseClaims(String token) {
        try {
            return jwtParser
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT: {}", e.getMessage());
            throw new JwtException(JwtErrorCode.EXPIRED_TOKEN);
        } catch (SignatureException e) {
            log.debug("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException(JwtErrorCode.INVALID_SIGNATURE);
        } catch (MalformedJwtException e) {
            log.debug("Malformed JWT: {}", e.getMessage());
            throw new JwtException(JwtErrorCode.MALFORMED_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported JWT: {}", e.getMessage());
            throw new JwtException(JwtErrorCode.UNSUPPORTED_TOKEN);
        } catch (IllegalArgumentException e) {
            log.debug("Empty JWT claims: {}", e.getMessage());
            throw new JwtException(JwtErrorCode.EMPTY_CLAIMS);
        }
    }
}
