package com.minishop.auth.repository;

import com.minishop.auth.entity.Auth;
import com.minishop.auth.entity.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Optional<Auth> findByLoginIdAndProvider(String loginId, AuthProvider provider);
    boolean existsByLoginIdAndProvider(String loginId, AuthProvider provider);
}
