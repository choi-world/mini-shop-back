package com.minishop.auth.entity;

import com.minishop.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_auth_login_id_provider", columnNames = {"login_id", "provider"}),
                @UniqueConstraint(name = "uq_auth_provider_id", columnNames = {"provider", "provider_id"})
        }
)
public class Auth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "login_id", length = 255)
    private String loginId;

    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(length = 255)
    private String ci;

    @Column(name = "is_delete", nullable = false)
    private boolean deleted;

    @Column(name = "delete_dt")
    private LocalDateTime deleteDt;

    protected Auth() {}

    @Builder
    private Auth(Long userId, String loginId, String password, AuthProvider provider,
                 String providerId, String ci) {
        this.userId = userId;
        this.loginId = loginId;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
        this.ci = ci;
        this.deleted = false;
    }
}
