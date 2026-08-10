package com.fpoly.marcusstore.entity.interaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Marcus thêm: chỉ lưu metadata đo chất lượng Live Chat, tuyệt đối không lưu nội
 * dung, tên đăng nhập, email hay số điện thoại của khách.
 */
@Entity
@Table(name = "Chat_Session_Metrics")
@Getter
@Setter
public class ChatSessionMetric {
    @Id
    @Column(name = "session_id", length = 36)
    private String sessionId;
    @Column(name = "customer_hash", nullable = false, length = 64)
    private String customerHash;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;
    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "answered", nullable = false)
    private Boolean answered = false;
    @Column(name = "closed_by", length = 20)
    private String closedBy;
}
