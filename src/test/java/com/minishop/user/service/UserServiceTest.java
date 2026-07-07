package com.minishop.user.service;

import com.minishop.common.exception.MiniShopException;
import com.minishop.user.entity.User;
import com.minishop.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks private UserService userService;
    @Mock private UserRepository userRepository;

    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("성공 - User를 생성하고 저장한다")
        void createUser_success() {
            given(userRepository.existsByEmail("test@test.com")).willReturn(false);
            given(userRepository.existsByPhoneNumber("01012345678")).willReturn(false);
            given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            userService.createUser("테스터", "test@test.com", "01012345678", LocalDate.of(1990, 1, 1), 1);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("실패 - 이미 사용 중인 이메일이면 409를 던진다")
        void createUser_fail_duplicateEmail() {
            given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

            assertThatThrownBy(() -> userService.createUser("테스터", "dup@test.com", "01012345678", LocalDate.of(1990, 1, 1), 1))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("실패 - 이미 사용 중인 전화번호이면 409를 던진다")
        void createUser_fail_duplicatePhone() {
            given(userRepository.existsByEmail("test@test.com")).willReturn(false);
            given(userRepository.existsByPhoneNumber("01012345678")).willReturn(true);

            assertThatThrownBy(() -> userService.createUser("테스터", "test@test.com", "01012345678", LocalDate.of(1990, 1, 1), 1))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("성공 - userId로 User를 조회한다")
        void findById_success() {
            User user = activeUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            User result = userService.findById(1L);

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 userId이면 404를 던진다")
        void findById_fail_notFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(999L))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("findActiveById()")
    class FindActiveById {

        @Test
        @DisplayName("성공 - 활성 User를 조회한다")
        void findActiveById_success() {
            User user = activeUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            User result = userService.findActiveById(1L);

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("실패 - 탈퇴한 User이면 410을 던진다")
        void findActiveById_fail_deleted() {
            User deletedUser = deletedUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(deletedUser));

            assertThatThrownBy(() -> userService.findActiveById(1L))
                    .isInstanceOf(MiniShopException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.GONE);
        }
    }

    private User activeUser() {
        return User.builder()
                .username("테스터")
                .email("test@test.com")
                .phoneNumber("01012345678")
                .birthday(LocalDate.of(1990, 1, 1))
                .gender(1)
                .build();
    }

    private User deletedUser() {
        User user = mock(User.class);
        given(user.isDeleted()).willReturn(true);
        return user;
    }
}
