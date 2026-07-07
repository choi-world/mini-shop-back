package com.minishop.user.entity;

import com.minishop.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uq_user_phone_number", columnNames = {"phone_number"})
        }
)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    private LocalDate birthday;

    @Column(columnDefinition = "TINYINT")
    private Integer gender;

    @Column(name = "is_delete", nullable = false)
    private boolean deleted;

    @Column(name = "delete_dt")
    private LocalDateTime deleteDt;

    protected User() {}

    @Builder
    private User(String username, String email, String phoneNumber,
                 LocalDate birthday, Integer gender) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.birthday = birthday;
        this.gender = gender;
        this.deleted = false;
    }
}
