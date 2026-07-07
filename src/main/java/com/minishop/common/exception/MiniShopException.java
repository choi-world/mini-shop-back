package com.minishop.common.exception;

import org.springframework.http.HttpStatus;

public class MiniShopException extends RuntimeException {

    private final HttpStatus status;

    public MiniShopException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
