package com.minishop.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minishop.auth.dto.TokenResponse;
import com.minishop.auth.facade.AuthFacade;
import com.minishop.common.config.SecurityConfig;
import com.minishop.common.exception.MiniShopException;
import com.minishop.common.util.JwtUtil;
import com.minishop.user.dto.UserResponse;
import com.minishop.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthFacade authFacade;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("성공 - 정상 이메일/비밀번호로 토큰을 발급한다")
        void login_success() throws Exception {
            UserResponse userResponse = UserResponse.from(createUser());
            TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", userResponse);

            given(authFacade.login(any())).willReturn(tokenResponse);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest("test@test.com", "password123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.user").exists());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 계정이면 401을 반환한다")
        void login_fail_accountNotFound() throws Exception {
            given(authFacade.login(any()))
                    .willThrow(new MiniShopException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest("notfound@test.com", "password123"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 비밀번호가 틀리면 401을 반환한다")
        void login_fail_wrongPassword() throws Exception {
            given(authFacade.login(any()))
                    .willThrow(new MiniShopException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest("test@test.com", "wrongpassword"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 탈퇴한 Auth 계정이면 410을 반환한다")
        void login_fail_deletedAuth() throws Exception {
            given(authFacade.login(any()))
                    .willThrow(new MiniShopException("탈퇴한 계정입니다.", HttpStatus.GONE));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest("deleted@test.com", "password123"))))
                    .andExpect(status().isGone());
        }

        @Test
        @DisplayName("실패 - 탈퇴한 User이면 410을 반환한다")
        void login_fail_deletedUser() throws Exception {
            given(authFacade.login(any()))
                    .willThrow(new MiniShopException("탈퇴한 사용자입니다.", HttpStatus.GONE));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest("deleted@test.com", "password123"))))
                    .andExpect(status().isGone());
        }

        @Test
        @DisplayName("실패 - 이메일 형식이 올바르지 않으면 400을 반환한다")
        void login_fail_invalidEmailFormat() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest("invalid-email", "password123"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 이메일이 빈값이면 400을 반환한다")
        void login_fail_emptyEmail() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest("", "password123"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 비밀번호가 빈값이면 400을 반환한다")
        void login_fail_emptyPassword() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest("test@test.com", ""))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signup")
    class Signup {

        @Test
        @DisplayName("성공 - 정상 정보로 회원가입 후 토큰을 발급한다")
        void signup_success() throws Exception {
            UserResponse userResponse = UserResponse.from(createUser());
            TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", userResponse);

            given(authFacade.signup(any())).willReturn(tokenResponse);

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest("new@test.com", "01099999999"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.user").exists());
        }

        @Test
        @DisplayName("실패 - 이미 사용 중인 이메일이면 409를 반환한다")
        void signup_fail_duplicateEmail() throws Exception {
            given(authFacade.signup(any()))
                    .willThrow(new MiniShopException("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT));

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest("dup@test.com", "01099999999"))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("실패 - 이미 사용 중인 전화번호이면 409를 반환한다")
        void signup_fail_duplicatePhone() throws Exception {
            given(authFacade.signup(any()))
                    .willThrow(new MiniShopException("이미 사용 중인 전화번호입니다.", HttpStatus.CONFLICT));

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest("new@test.com", "01012345678"))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("실패 - 이메일이 빈값이면 400을 반환한다")
        void signup_fail_emptyEmail() throws Exception {
            Map<String, Object> body = signupRequest("", "01099999999");

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 이메일 형식이 올바르지 않으면 400을 반환한다")
        void signup_fail_invalidEmailFormat() throws Exception {
            Map<String, Object> body = signupRequest("not-an-email", "01099999999");

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 필수 필드가 누락되면 400을 반환한다")
        void signup_fail_missingRequiredField() throws Exception {
            Map<String, Object> body = signupRequest("new@test.com", "01099999999");
            body.remove("username");

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("성공 - 유효한 Refresh Token으로 새 토큰을 발급한다")
        void refresh_success() throws Exception {
            UserResponse userResponse = UserResponse.from(createUser());
            TokenResponse tokenResponse = TokenResponse.of("new-access-token", "new-refresh-token", userResponse);
            given(authFacade.refresh(any())).willReturn(tokenResponse);

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", "valid-refresh-token"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
        }

        @Test
        @DisplayName("실패 - 만료된 Refresh Token이면 401을 반환한다")
        void refresh_fail_expiredToken() throws Exception {
            given(authFacade.refresh(any()))
                    .willThrow(new MiniShopException("Refresh Token이 만료되었습니다.", HttpStatus.UNAUTHORIZED));

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", "expired-token"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - Redis에 저장된 토큰과 불일치하면 401을 반환한다")
        void refresh_fail_tokenMismatch() throws Exception {
            given(authFacade.refresh(any()))
                    .willThrow(new MiniShopException("유효하지 않은 Refresh Token입니다.", HttpStatus.UNAUTHORIZED));

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", "mismatched-token"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - refreshToken이 빈값이면 400을 반환한다")
        void refresh_fail_emptyToken() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", ""))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("성공 - 인증된 사용자가 로그아웃하면 200을 반환한다")
        void logout_success() throws Exception {
            given(jwtUtil.isExpired("test-token")).willReturn(false);
            given(jwtUtil.extractUserId("test-token")).willReturn(1L);

            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 - 인증 없이 요청하면 401을 반환한다")
        void logout_fail_unauthenticated() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private Map<String, String> loginRequest(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    private Map<String, Object> signupRequest(String email, String phoneNumber) {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("password", "password123");
        map.put("username", "테스터");
        map.put("phoneNumber", phoneNumber);
        map.put("birthday", "1990-01-01");
        map.put("gender", 1);
        return map;
    }

    private User createUser() {
        return User.builder()
                .username("테스터")
                .email("test@test.com")
                .phoneNumber("01012345678")
                .birthday(LocalDate.of(1990, 1, 1))
                .gender(1)
                .build();
    }
}
