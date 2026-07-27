package com.fpoly.marcusstore.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "AI_Product_Clicks")
@Getter
@Setter
public class AiProductClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "click_id")
    private Long clickId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    // Marcus thêm: UUID phiên ẩn danh chỉ phục vụ chống ghi click trùng, không gắn
    // với tài khoản hay nội dung hội thoại.
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @PrePersist
    void initializeClickedAt() {
        if (clickedAt == null) {
            clickedAt = LocalDateTime.now();
        }
    }
}
