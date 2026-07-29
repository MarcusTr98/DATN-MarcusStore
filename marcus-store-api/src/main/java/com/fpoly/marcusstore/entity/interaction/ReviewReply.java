package com.fpoly.marcusstore.entity.interaction;

import java.time.LocalDateTime;

import com.fpoly.marcusstore.entity.auth.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Review_Replies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Integer replyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private CommentEvaluation review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private User staff;

    @Column(name = "reply_text", columnDefinition = "NVARCHAR(MAX)")
    private String replyText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}