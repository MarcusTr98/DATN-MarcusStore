package com.fpoly.marcusstore.entity.shopping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Marcus thêm: lưu sự kiện hủy có cấu trúc riêng, không tiếp tục đưa lý do và
 * nguồn hủy vào bảng Orders. Khóa dùng order_id bảo đảm một đơn chỉ hoàn tài
 * nguyên theo một quyết định hủy.
 */
@Entity
@Table(name = "Order_Cancellations")
@Getter
@Setter
public class OrderCancellation {

    @Id
    @Column(name = "order_id")
    private Integer orderId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Column(name = "actor_type", nullable = false, length = 20)
    private String actorType;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt;
}
