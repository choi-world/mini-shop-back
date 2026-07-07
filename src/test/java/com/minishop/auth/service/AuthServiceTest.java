package com.minishop.auth.service;

import com.minishop.auth.entity.Auth;
import com.minishop.auth.entity.AuthProvider;
import com.minishop.auth.repository.AuthRepository;
import com.minishop.common.exception.MiniShopException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks private AuthService authService;
    @Mock private AuthRepository authRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("createLocalAuth()")
    class CreateLocalAuth {

        @Test
        @DisplayName("성공 - Auth를 생성하고 저장한다")
        void createLocalAuth_success() {
            given(authRepository.existsByLoginIdAndProvider("test@test.com", AuthProvider.LOCAL)).willReturn(false);
            given(passwordEncoder.encode("rawPassword")).willReturn("encodedPassword");
            given(authRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            authService.createLocalAuth(1L, "test@test.com", "rawPassword");

            verify(authRepository).save(any(Auth.class));
        }

        @Test
        @DisplayName("실패 - 이미 사용 중인 loginId이면 409를 던진다")
        void createLocalAuth_fail_duplicateLoginId() {
            given(authRepository.existsByLoginIdAndProvider("test@test.com", AuthProvider.LOCAL)).willReturn(true);

            assertThatThrownBy(() -> authService.createLocalAuth(1L, "test@test.com", "rawPassword"))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("findLocalAuthByLoginId()")
    class FindLocalAuthByLoginId {

        @Test
        @DisplayName("성공 - loginId로 Auth를 조회한다")
        void findLocalAuthByLoginId_success() {
            Auth auth = activeAuth();
            given(authRepository.findByLoginIdAndProvider("test@test.com", AuthProvider.LOCAL))
                    .willReturn(Optional.of(auth));

            Auth result = authService.findLocalAuthByLoginId("test@test.com");

            assertThat(result).isEqualTo(auth);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 loginId이면 401을 던진다")
        void findLocalAuthByLoginId_fail_notFound() {
            given(authRepository.findByLoginIdAndProvider("notfound@test.com", AuthProvider.LOCAL))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.findLocalAuthByLoginId("notfound@test.com"))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패 - 탈퇴한 Auth이면 410을 던진다")
        void findLocalAuthByLoginId_fail_deleted() {
            Auth deletedAuth = deletedAuth();
            given(authRepository.findByLoginIdAndProvider("deleted@test.com", AuthProvider.LOCAL))
                    .willReturn(Optional.of(deletedAuth));

            assertThatThrownBy(() -> authService.findLocalAuthByLoginId("deleted@test.com"))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.GONE);
        }
    }

    @Nested
    @DisplayName("verifyPassword()")
    class VerifyPassword {

        @Test
        @DisplayName("성공 - 비밀번호가 일치하면 예외를 던지지 않는다")
        void verifyPassword_success() {
            given(passwordEncoder.matches("rawPassword", "encodedPassword")).willReturn(true);

            authService.verifyPassword("rawPassword", "encodedPassword");
        }

        @Test
        @DisplayName("실패 - 비밀번호가 불일치하면 401을 던진다")
        void verifyPassword_fail_wrongPassword() {
            given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

            assertThatThrownBy(() -> authService.verifyPassword("wrongPassword", "encodedPassword"))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }
    }

    private Auth activeAuth() {
        return Auth.builder()
                .userId(1L)
                .loginId("test@test.com")
                .password("encodedPassword")
                .provider(AuthProvider.LOCAL)
                .build();
    }

    private Auth deletedAuth() {
        Auth auth = mock(Auth.class);
        given(auth.isDeleted()).willReturn(true);
        return auth;
    }
}
