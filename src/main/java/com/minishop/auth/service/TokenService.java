package com.minishop.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private static final String REFRESH_TOKEN_PREFIX = "rt:";

    private final RedisTemplate<String, String> redisTemplate;
    private final long refreshTokenExpiration;

    public TokenService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue()
                .set(REFRESH_TOKEN_PREFIX + userId, refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    public String get(Long userId) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
    }

    public void delete(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}
