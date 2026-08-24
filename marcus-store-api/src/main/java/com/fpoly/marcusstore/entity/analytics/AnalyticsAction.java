package com.fpoly.marcusstore.entity.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "Analytics_Actions")
@Getter
@Setter
public class AnalyticsAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(nullable = false, length = 300)
    private String reason;
    @Column(nullable = false, length = 20)
    private String priority;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "owner_username", nullable = false, length = 100)
    private String ownerUsername;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null)
            status = "ACCEPTED";
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
