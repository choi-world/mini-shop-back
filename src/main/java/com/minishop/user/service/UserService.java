package com.minishop.user.service;

import com.minishop.common.exception.MiniShopException;
import com.minishop.user.entity.User;
import com.minishop.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String username, String email, String phoneNumber,
                           LocalDate birthday, Integer gender) {
        if (userRepository.existsByEmail(email)) {
            throw new MiniShopException("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new MiniShopException("이미 사용 중인 전화번호입니다.", HttpStatus.CONFLICT);
        }
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .phoneNumber(phoneNumber)
                .birthday(birthday)
                .gender(gender)
                .build());
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new MiniShopException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    public User findActiveById(Long userId) {
        User user = findById(userId);
        if (user.isDeleted()) {
            throw new MiniShopException("탈퇴한 사용자입니다.", HttpStatus.GONE);
        }
        return user;
    }
}
