package com.fpoly.marcusstore.entity.contact;

import com.fpoly.marcusstore.entity.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "User_Notifications")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
// Marcus thêm bảng chuông riêng theo khách hàng; không dùng chung dữ liệu
// chuông
// Admin để tránh lộ thông báo giữa các tài khoản.
public class UserNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "reference_id", length = 50)
    private String referenceId;

    // Marcus thêm: khóa sự kiện theo từng khách, chống chuông trùng do callback retry.
    @Column(name = "event_key", length = 180, unique = true)
    private String eventKey;

    @Column(name = "category", nullable = false, length = 20)
    private String category = "INFO";

    @Column(name = "icon", length = 80)
    private String icon;

    @Column(name = "deep_link", length = 300)
    private String deepLink;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
