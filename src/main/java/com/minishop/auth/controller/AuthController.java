package com.minishop.auth.controller;

import com.minishop.auth.dto.LoginRequest;
import com.minishop.auth.dto.RefreshRequest;
import com.minishop.auth.dto.SignupRequest;
import com.minishop.auth.dto.TokenResponse;
import com.minishop.auth.facade.AuthFacade;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthFacade authFacade;

    public AuthController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    /**
     * @API Endpoint POST /auth/signup
     * @Request SignupRequest
     * @Description 일반 회원가입 (LOCAL provider)
     * @Author csd
     */
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity.ok(authFacade.signup(request));
    }

    /**
     * @API Endpoint POST /auth/login
     * @Request LoginRequest
     * @Description 이메일/비밀번호 로그인
     * @Author csd
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authFacade.login(request));
    }

    /**
     * @API Endpoint POST /auth/refresh
     * @Request RefreshRequest
     * @Description Refresh Token으로 Access Token 재발급
     * @Author csd
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(authFacade.refresh(request));
    }

    /**
     * @API Endpoint POST /auth/logout
     * @Request -
     * @Description 로그아웃 - Redis에서 Refresh Token 삭제
     * @Author csd
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
        authFacade.logout(userId);
        return ResponseEntity.ok().build();
    }
}
