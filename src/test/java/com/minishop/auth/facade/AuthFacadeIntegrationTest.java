package com.minishop.auth.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minishop.auth.dto.LoginRequest;
import com.minishop.auth.dto.RefreshRequest;
import com.minishop.auth.dto.SignupRequest;
import com.minishop.auth.dto.TokenResponse;
import com.minishop.auth.entity.Auth;
import com.minishop.auth.entity.AuthProvider;
import com.minishop.auth.repository.AuthRepository;
import com.minishop.common.exception.MiniShopException;
import com.minishop.common.util.JwtUtil;
import com.minishop.user.entity.User;
import com.minishop.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class AuthFacadeIntegrationTest {

    private static final String TEST_EMAIL     = "test@minishop-test.com";
    private static final String TEST_PASSWORD  = "Test1234!";
    private static final String TEST_USERNAME  = "테스트_계정";
    private static final String TEST_PHONE     = "01000000000";
    private static final String TEST_NEW_EMAIL = "new@minishop-test.com";
    private static final String TEST_NEW_PHONE = "01011111111";

    @Autowired private AuthFacade authFacade;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthRepository authRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .phoneNumber(TEST_PHONE)
                .birthday(LocalDate.of(1990, 1, 1))
                .gender(1)
                .build());
        authRepository.save(Auth.builder()
                .userId(user.getId())
                .loginId(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .provider(AuthProvider.LOCAL)
                .build());
    }

    @AfterEach
    void tearDown() {
        authRepository.deleteAll();
        userRepository.deleteAll();
        Set<String> keys = redisTemplate.keys("rt:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("성공 - 토큰을 발급하고 Redis에 Refresh Token을 저장한다")
        void login_success() {
            TokenResponse response = authFacade.login(toLoginRequest(TEST_EMAIL, TEST_PASSWORD));

            assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);

            Long userId = response.getUser().getId();
            assertThat(jwtUtil.extractUserId(response.getAccessToken())).isEqualTo(userId);
            assertThat(jwtUtil.isExpired(response.getAccessToken())).isFalse();

            String storedToken = redisTemplate.opsForValue().get("rt:" + userId);
            assertThat(storedToken).isEqualTo(response.getRefreshToken());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 계정이면 401을 던진다")
        void login_fail_accountNotFound() {
            assertThatThrownBy(() -> authFacade.login(toLoginRequest("notfound@minishop-test.com", TEST_PASSWORD)))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패 - 비밀번호가 틀리면 401을 던진다")
        void login_fail_wrongPassword() {
            assertThatThrownBy(() -> authFacade.login(toLoginRequest(TEST_EMAIL, "wrongPassword!")))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패 - 탈퇴한 Auth이면 410을 던진다")
        void login_fail_deletedAuth() {
            jdbcTemplate.update("UPDATE auth SET is_delete = 1 WHERE login_id = ?", TEST_EMAIL);

            assertThatThrownBy(() -> authFacade.login(toLoginRequest(TEST_EMAIL, TEST_PASSWORD)))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.GONE);
        }

        @Test
        @DisplayName("실패 - 탈퇴한 User이면 410을 던진다")
        void login_fail_deletedUser() {
            jdbcTemplate.update("UPDATE `user` SET is_delete = 1 WHERE email = ?", TEST_EMAIL);

            assertThatThrownBy(() -> authFacade.login(toLoginRequest(TEST_EMAIL, TEST_PASSWORD)))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.GONE);
        }
    }

    @Nested
    @DisplayName("signup()")
    class Signup {

        @Test
        @DisplayName("성공 - User/Auth를 저장하고 토큰을 발급하며 Redis에 Refresh Token을 저장한다")
        void signup_success() {
            TokenResponse response = authFacade.signup(toSignupRequest(TEST_NEW_EMAIL, TEST_NEW_PHONE));

            assertThat(response.getUser().getEmail()).isEqualTo(TEST_NEW_EMAIL);

            Long userId = response.getUser().getId();
            assertThat(jwtUtil.extractUserId(response.getAccessToken())).isEqualTo(userId);
            assertThat(jwtUtil.isExpired(response.getAccessToken())).isFalse();

            String storedToken = redisTemplate.opsForValue().get("rt:" + userId);
            assertThat(storedToken).isEqualTo(response.getRefreshToken());
        }

        @Test
        @DisplayName("실패 - 이미 사용 중인 이메일이면 409를 던진다")
        void signup_fail_duplicateEmail() {
            assertThatThrownBy(() -> authFacade.signup(toSignupRequest(TEST_EMAIL, TEST_NEW_PHONE)))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("실패 - 이미 사용 중인 전화번호이면 409를 던진다")
        void signup_fail_duplicatePhone() {
            assertThatThrownBy(() -> authFacade.signup(toSignupRequest(TEST_NEW_EMAIL, TEST_PHONE)))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("성공 - 유효한 Refresh Token으로 새 토큰을 발급하고 Redis를 갱신한다")
        void refresh_success() {
            TokenResponse loginResponse = authFacade.login(toLoginRequest(TEST_EMAIL, TEST_PASSWORD));
            String originalRefreshToken = loginResponse.getRefreshToken();
            Long userId = loginResponse.getUser().getId();

            TokenResponse refreshResponse = authFacade.refresh(toRefreshRequest(originalRefreshToken));

            assertThat(jwtUtil.extractUserId(refreshResponse.getAccessToken())).isEqualTo(userId);
            assertThat(jwtUtil.isExpired(refreshResponse.getAccessToken())).isFalse();

            String storedToken = redisTemplate.opsForValue().get("rt:" + userId);
            assertThat(storedToken).isEqualTo(refreshResponse.getRefreshToken());
        }

        @Test
        @DisplayName("실패 - Redis에 저장된 토큰과 불일치하면 401을 던진다")
        void refresh_fail_tokenMismatch() {
            authFacade.login(toLoginRequest(TEST_EMAIL, TEST_PASSWORD));

            String mismatchedToken = jwtUtil.generateRefreshToken(999L);

            assertThatThrownBy(() -> authFacade.refresh(toRefreshRequest(mismatchedToken)))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("성공 - 로그아웃하면 Redis에서 Refresh Token이 삭제된다")
        void logout_success() {
            TokenResponse response = authFacade.login(toLoginRequest(TEST_EMAIL, TEST_PASSWORD));
            Long userId = response.getUser().getId();

            assertThat(redisTemplate.opsForValue().get("rt:" + userId)).isNotNull();

            authFacade.logout(userId);

            assertThat(redisTemplate.opsForValue().get("rt:" + userId)).isNull();
        }
    }

    private LoginRequest toLoginRequest(String email, String password) {
        return objectMapper.convertValue(Map.of("email", email, "password", password), LoginRequest.class);
    }

    private RefreshRequest toRefreshRequest(String refreshToken) {
        return objectMapper.convertValue(Map.of("refreshToken", refreshToken), RefreshRequest.class);
    }

    private SignupRequest toSignupRequest(String email, String phoneNumber) {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("password", TEST_PASSWORD);
        map.put("username", TEST_USERNAME);
        map.put("phoneNumber", phoneNumber);
        map.put("birthday", LocalDate.of(1990, 1, 1));
        map.put("gender", 1);
        return objectMapper.convertValue(map, SignupRequest.class);
    }
}
