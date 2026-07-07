package com.minishop.auth.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RefreshRequest {

    @NotBlank
    private String refreshToken;

    protected RefreshRequest() {}
}
