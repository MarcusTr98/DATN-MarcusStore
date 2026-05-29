package com.fpoly.marcusstore.entity.interaction;

import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Comments_Evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Integer rating;

    @Column(name = "comment_text", columnDefinition = "NVARCHAR(MAX)")
    private String commentText;

    @Column(name = "is_approved")
    private Boolean isApproved = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}