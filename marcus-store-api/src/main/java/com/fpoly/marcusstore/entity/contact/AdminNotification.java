package com.fpoly.marcusstore.entity.contact;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "Admin_Notifications")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String type; // 'ORDER' hoặc 'CONTACT'

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "reference_id", length = 50)
    private String referenceId; // Lưu mã đơn hoặc ID liên hệ để FE làm chức năng Click chuyển trang

    // Marcus thêm: eventKey chống ghi trùng khi webhook/scheduler retry cùng sự kiện.
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

    @Column(name = "is_read")
    private Boolean isRead = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
