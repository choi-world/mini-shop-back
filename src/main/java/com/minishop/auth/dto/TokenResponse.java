package com.minishop.auth.dto;

import com.minishop.user.dto.UserResponse;
import lombok.Getter;

@Getter
public class TokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final UserResponse user;

    private TokenResponse(String accessToken, String refreshToken, UserResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public static TokenResponse of(String accessToken, String refreshToken, UserResponse user) {
        return new TokenResponse(accessToken, refreshToken, user);
    }
}
