package com.fpoly.marcusstore.entity.contact;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "Contact_Requests")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class ContactRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private Integer contactId;

    // Liên kết với tài khoản đang đăng nhập, khách vãng lai thì null
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "status", length = 50)
    private String status = "NEW";

    // Marcus thêm: dấu vết vận hành của module liên hệ.
    @Column(name = "handled_by", length = 100)
    private String handledBy;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
