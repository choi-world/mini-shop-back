package com.minishop.user.dto;

import com.minishop.user.entity.User;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class UserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final String phoneNumber;
    private final LocalDate birthday;
    private final Integer gender;
    private final LocalDateTime createdDt;

    private UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.birthday = user.getBirthday();
        this.gender = user.getGender();
        this.createdDt = user.getCreatedDt();
    }

    public static UserResponse from(User user) {
        return new UserResponse(user);
    }
}
