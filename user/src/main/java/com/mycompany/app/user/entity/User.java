package com.mycompany.app.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column
    private String phone;

    @Column
    private String address;

    @Column
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private Role role;


    @Column(nullable = false)
    private boolean enabled = true;

    @Column
    @JsonIgnore
    private String verificationCode;

    @Column
    @JsonIgnore
    private LocalDateTime verificationCodeExpiry;

    @Column
    @JsonIgnore
    private String passwordResetToken;

    @Column
    @JsonIgnore
    private LocalDateTime passwordResetTokenExpiry;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
