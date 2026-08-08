package com.fpoly.marcusstore.entity.shopping;

import com.fpoly.marcusstore.entity.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Warranty_Returns")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class WarrantyReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warranty_id")
    private Integer warrantyId;

    @Column(name = "reason", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private WarrantyReason reason;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private WarrantyStatus status = WarrantyStatus.PENDING;

    @Column(name = "admin_note", columnDefinition = "NVARCHAR(MAX)")
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "warrantyReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarrantyAttachment> attachments = new ArrayList<>();

    public enum WarrantyReason {
        DEFECTIVE,           // Sản phẩm lỗi
        DAMAGED,             // Bị hư hỏng
        WRONG_ITEM,          // Giao sai sản phẩm
        NOT_AS_DESCRIBED,    // Không đúng mô tả
        ACCESSORY_MISSING,    // Thiếu phụ kiện
        OTHER                // Lý do khác
    }

    public enum WarrantyStatus {
        PENDING,     // Chờ admin xác nhận tiếp nhận
        CONFIRMED,   // Admin đã xác nhận - đang xử lý
        APPROVED,    // Đồng ý bảo hành
        REJECTED     // Từ chối
    }
}
