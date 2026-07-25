package com.fpoly.marcusstore.entity.interaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.shopping.OrderItem;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "Comments_Evaluations")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class CommentEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment_text", columnDefinition = "NVARCHAR(MAX)")
    private String commentText;

    @Column(name = "is_approved", columnDefinition = "BIT DEFAULT 1")
    private Boolean isApproved = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    @JsonIgnore
    private OrderItem orderItem;


    @PrePersist
    public void prePersist() {
        if (isApproved == null) {
            isApproved = false;
        }
    }


    @OneToOne(
            mappedBy = "review",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private ReviewReply reply;


    @OneToMany(
            mappedBy = "review",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<ReviewImage> images = new ArrayList<>();
}