package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.ReviewReplyRequest;
import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.interaction.CommentEvaluation;
import com.fpoly.marcusstore.entity.interaction.ReviewImage;
import com.fpoly.marcusstore.entity.interaction.ReviewReply;
import com.fpoly.marcusstore.entity.shopping.*;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.statistics.CommentEvaluationRepository;
import com.fpoly.marcusstore.repository.statistics.ReviewReplyRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewAdminService {

    private final CommentEvaluationRepository reviewRepository;
private final ReviewReplyRepository replyRepository;

private final UserRepository userRepository;
public Page<ReviewAdminResponse> getAllReviews(int page, int size) {

    return reviewRepository
            .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
            .map(this::mapToResponse);

}

   private ReviewAdminResponse mapToResponse(CommentEvaluation review) {

    ReviewReplyResponse reply = null;

    if (review.getReply() != null) {

        reply = ReviewReplyResponse.builder()
                .replyId(review.getReply().getReplyId())
                .staffId(review.getReply().getStaff().getUserId())
                .staffName(review.getReply().getStaff().getFullName())
                .replyText(review.getReply().getReplyText())
                .createdAt(review.getReply().getCreatedAt())
                .build();

    }

    return ReviewAdminResponse.builder()
            .reviewId(review.getReviewId())
            .userId(review.getUser().getUserId())
            .username(review.getUser().getUsername())
            .fullName(review.getUser().getFullName())

            .productId(review.getProduct().getProductId())
            .productName(review.getProduct().getProductName())

            .rating(review.getRating())
            .commentText(review.getCommentText())
            .createdAt(review.getCreatedAt())

            .orderItemId(review.getOrderItem().getOrderItemId())

            .images(
                    review.getImages()
                            .stream()
                            .map(ReviewImage::getImageUrl)
                            .toList()
            )

            .reply(reply)

            .build();
}
    @Transactional
public void replyReview(Integer reviewId,
                        Integer staffId,
                        ReviewReplyRequest request) {

    CommentEvaluation review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

    User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

    if (review.getReply() != null) {
        throw new RuntimeException("Đánh giá này đã được trả lời");
    }

    ReviewReply reply = ReviewReply.builder()
            .review(review)
            .staff(staff)
            .replyText(request.getReplyText())
            .build();

    replyRepository.save(reply);
}
@Transactional
public void deleteReview(Integer reviewId) {

    CommentEvaluation review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

    // Nếu có reply thì xóa trước
    if (review.getReply() != null) {
        review.setReply(null);
    }

    reviewRepository.delete(review);
}

@Transactional
public void updateReply(
        Integer reviewId,
        Integer staffId,
        ReviewReplyRequest request) {

    ReviewReply reply = replyRepository
            .findByReviewReviewId(reviewId)
            .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy phản hồi"));

    // chỉ người tạo mới được sửa
    if (!reply.getStaff().getUserId().equals(staffId)) {
        throw new RuntimeException("Bạn không có quyền sửa phản hồi này");
    }

    reply.setReplyText(request.getReplyText());

    replyRepository.save(reply);
}
public Page<ReviewAdminResponse> search(
        String keyword,
        Integer productId,
        Integer rating,
        Boolean replied,
        Pageable pageable){

return reviewRepository
        .search(
                keyword,    
                productId,
                rating,
                replied,
                pageable
        )
        .map(this::mapToResponse);
    }
public List<RatingStatisticResponse> getRatingStatistics(Integer productId){

    List<Object[]> result =
            reviewRepository.getRatingStatistics(productId);

    List<RatingStatisticResponse> list = new ArrayList<>();

    for(int star = 5 ; star >= 1 ; star--){

        long count = 0;

        for(Object[] row : result){

            Integer rating = (Integer) row[0];

            Long total = (Long) row[1];

            if(rating.equals(star)){
                count = total;
                break;
            }

        }

        list.add(
                RatingStatisticResponse.builder()
                        .star(star)
                        .count(count)
                        .build()
        );

    }

    return list;

}

public List<ProductReviewItemResponse> getProductsHasReview(){

    return reviewRepository
            .getProductsHasReview()
            .stream()
            .map(row -> ProductReviewItemResponse.builder()
                    .productId((Integer) row[0])
                    .productName((String) row[1])
                    .reviewCount((Long) row[2])
                    .build())
            .toList();

}
}