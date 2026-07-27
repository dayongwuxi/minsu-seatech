package com.seatech.minsu.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具：subject=用户id，audience 区分端（member/admin）。
 */
@Component
public class JwtUtil {

    public static final String AUD_MEMBER = "member";
    public static final String AUD_ADMIN = "admin";

    private final SecretKey key;
    private final Duration expire;

    public JwtUtil(@Value("${minsu.jwt.secret}") String secret,
                   @Value("${minsu.jwt.expire-hours}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = Duration.ofHours(expireHours);
    }

    public String createToken(Long userId, String audience) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key)
                .compact();
    }

    /** 解析 token，校验签名与所属端；无效返回 null */
    public Long parseUserId(String token, String audience) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
                return null;
            }
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
