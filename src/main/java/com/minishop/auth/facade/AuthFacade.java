package com.minishop.auth.facade;

import com.minishop.auth.dto.LoginRequest;
import com.minishop.auth.dto.RefreshRequest;
import com.minishop.auth.dto.SignupRequest;
import com.minishop.auth.dto.TokenResponse;
import com.minishop.auth.entity.Auth;
import com.minishop.auth.service.AuthService;
import com.minishop.auth.service.TokenService;
import com.minishop.common.exception.MiniShopException;
import com.minishop.common.util.JwtUtil;
import com.minishop.user.dto.UserResponse;
import com.minishop.user.entity.User;
import com.minishop.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthFacade {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    public AuthFacade(AuthService authService, UserService userService,
                      JwtUtil jwtUtil, TokenService tokenService) {
        this.authService = authService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
    }

    /**
     * @Description 일반 회원가입. User 생성 후 LOCAL Auth 생성, 토큰 발급.
     * @Author csd
     */
    @Transactional
    public TokenResponse signup(SignupRequest request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getBirthday(),
                request.getGender()
        );
        authService.createLocalAuth(user.getId(), request.getEmail(), request.getPassword());
        return issueTokens(user);
    }

    /**
     * @Description 이메일/비밀번호 로그인. 비밀번호 검증 후 토큰 발급.
     * @Author csd
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Auth auth = authService.findLocalAuthByLoginId(request.getEmail());
        authService.verifyPassword(request.getPassword(), auth.getPassword());
        User user = userService.findActiveById(auth.getUserId());
        return issueTokens(user);
    }

    /**
     * @Description Refresh Token 검증 후 Access Token 재발급 (Token Rotation 적용).
     * @Author csd
     */
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (jwtUtil.isExpired(refreshToken)) {
            throw new MiniShopException("Refresh Token이 만료되었습니다.", HttpStatus.UNAUTHORIZED);
        }
        Long userId = jwtUtil.extractUserId(refreshToken);
        String storedToken = tokenService.get(userId);
        if (!refreshToken.equals(storedToken)) {
            throw new MiniShopException("유효하지 않은 Refresh Token입니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userService.findById(userId);
        return issueTokens(user);
    }

    /**
     * @Description 로그아웃. Redis에서 Refresh Token 삭제.
     * @Author csd
     */
    public void logout(Long userId) {
        tokenService.delete(userId);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        tokenService.save(user.getId(), refreshToken);
        return TokenResponse.of(accessToken, refreshToken, UserResponse.from(user));
    }
}
