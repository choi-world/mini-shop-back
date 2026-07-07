package com.minishop.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private static final long REFRESH_TOKEN_EXPIRATION = 86400000L;

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("Refresh Token을 Redis에 TTL과 함께 저장한다")
        void save() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            TokenService tokenService = new TokenService(redisTemplate, REFRESH_TOKEN_EXPIRATION);

            tokenService.save(1L, "refresh-token");

            verify(valueOperations).set("rt:1", "refresh-token", REFRESH_TOKEN_EXPIRATION, TimeUnit.MILLISECONDS);
        }
    }

    @Nested
    @DisplayName("get()")
    class Get {

        @Test
        @DisplayName("Redis에서 Refresh Token을 조회한다")
        void get() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("rt:1")).willReturn("refresh-token");
            TokenService tokenService = new TokenService(redisTemplate, REFRESH_TOKEN_EXPIRATION);

            String result = tokenService.get(1L);

            assertThat(result).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("저장된 토큰이 없으면 null을 반환한다")
        void get_returnsNull_whenNotExists() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("rt:999")).willReturn(null);
            TokenService tokenService = new TokenService(redisTemplate, REFRESH_TOKEN_EXPIRATION);

            String result = tokenService.get(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Redis에서 Refresh Token을 삭제한다")
        void delete() {
            TokenService tokenService = new TokenService(redisTemplate, REFRESH_TOKEN_EXPIRATION);

            tokenService.delete(1L);

            verify(redisTemplate).delete("rt:1");
        }
    }
}
