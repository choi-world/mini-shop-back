package com.minishop.auth.service;

import com.minishop.auth.entity.Auth;
import com.minishop.auth.entity.AuthProvider;
import com.minishop.auth.repository.AuthRepository;
import com.minishop.common.exception.MiniShopException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Auth createLocalAuth(Long userId, String loginId, String rawPassword) {
        if (authRepository.existsByLoginIdAndProvider(loginId, AuthProvider.LOCAL)) {
            throw new MiniShopException("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);
        }
        return authRepository.save(Auth.builder()
                .userId(userId)
                .loginId(loginId)
                .password(passwordEncoder.encode(rawPassword))
                .provider(AuthProvider.LOCAL)
                .build());
    }

    public Auth findLocalAuthByLoginId(String loginId) {
        Auth auth = authRepository.findByLoginIdAndProvider(loginId, AuthProvider.LOCAL)
                .orElseThrow(() -> new MiniShopException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED));
        if (auth.isDeleted()) {
            throw new MiniShopException("탈퇴한 계정입니다.", HttpStatus.GONE);
        }
        return auth;
    }

    public void verifyPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new MiniShopException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
    }
}
