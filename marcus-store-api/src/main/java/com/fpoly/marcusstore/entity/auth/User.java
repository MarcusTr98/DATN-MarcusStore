package com.fpoly.marcusstore.entity.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Users")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", length = 255)
    @JsonIgnore // BẮT BUỘC: Không bao giờ trả password hash ra API
    private String passwordHash;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnore
    private Role role;
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
        name = "User_Permissions",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
)
@JsonIgnore
private Set<Permission> permissions = new HashSet<>();
    @Column(name = "email_verified")
    private Boolean emailVerified;
    
    @Column(name = "google_account_id")
    private String googleAccountId;

    @Column(name = "facebook_account_id")
    private String facebookAccountId;

    @ManyToOne(fetch = FetchType.LAZY)

@JoinColumn(name = "position_id")

private EmployeePosition position;
@Column(name = "use_default_permission", nullable = false)
private Boolean useDefaultPermission = true;
}