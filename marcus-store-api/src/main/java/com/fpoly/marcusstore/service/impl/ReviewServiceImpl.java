package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.CreateReviewRequest;
import com.fpoly.marcusstore.dto.response.ReviewResponse;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.interaction.CommentEvaluation;
import com.fpoly.marcusstore.entity.interaction.ReviewImage;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.statistics.CommentEvaluationRepository;
import com.fpoly.marcusstore.repository.statistics.ReviewImageRepository;
import com.fpoly.marcusstore.service.CloudinaryService;
import com.fpoly.marcusstore.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final CommentEvaluationRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public ReviewResponse createReview(
            String username,
            Integer orderItemId,
            CreateReviewRequest request) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy người dùng."));

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm trong đơn hàng."));

        if (!orderItem.getOrder().getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Bạn không có quyền đánh giá sản phẩm này.");
        }

        if (!"COMPLETED".equals(orderItem.getOrder().getOrderStatus())) {
            throw new RuntimeException("Chỉ được đánh giá khi đơn hàng đã hoàn thành.");
        }

        if (reviewRepository.existsByOrderItemOrderItemId(orderItemId)) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này.");
        }

        Product product = orderItem.getSku().getProduct();

        CommentEvaluation review = new CommentEvaluation();
        review.setOrderItem(orderItem);
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setCommentText(request.getCommentText());
        review.setIsApproved(true);

        review = reviewRepository.save(review);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {

            int displayOrder = 0;

            for (String imageUrl : request.getImageUrls()) {

                ReviewImage image = ReviewImage.builder()
                        .review(review)
                        .imageUrl(imageUrl)
                        .displayOrder(displayOrder++)
                        .build();

                reviewImageRepository.save(image);
            }
        }

        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(
            String username,
            Integer orderItemId) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy người dùng."));

        CommentEvaluation review = reviewRepository
                .findByOrderItemOrderItemId(orderItemId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy đánh giá."));

        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Bạn không có quyền xem đánh giá này.");
        }

        return toResponse(review);
    }

    /**
     * Entity -> DTO
     */
    private ReviewResponse toResponse(CommentEvaluation review) {

        ReviewResponse.ReviewResponseBuilder builder =
                ReviewResponse.builder()
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
                        );

        if (review.getReply() != null) {

            builder.replyContent(
                    review.getReply().getReplyText());

            builder.replyStaffName(
                    review.getReply().getStaff().getFullName());

            builder.replyCreatedAt(
                    review.getReply().getCreatedAt());
        }

        return builder.build();
    }
 @Override
public ReviewResponse updateReview(
        String username,
        Integer reviewId,
        CreateReviewRequest request) {

    User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy người dùng."));

    CommentEvaluation review = reviewRepository.findById(reviewId)
            .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy đánh giá."));

    if (!review.getUser().getUserId().equals(user.getUserId())) {
        throw new RuntimeException("Bạn không có quyền sửa đánh giá.");
    }

    // ==========================
    // Cập nhật nội dung đánh giá
    // ==========================

    review.setRating(request.getRating());
    review.setCommentText(request.getCommentText());

    reviewRepository.save(review);

    // ==========================
    // Danh sách ảnh cũ
    // ==========================

    List<ReviewImage> oldImages =
            reviewImageRepository
                    .findByReviewReviewIdOrderByDisplayOrderAsc(reviewId);

    List<String> newUrls = request.getImageUrls();

    if (newUrls == null) {
        newUrls = List.of();
    }

    // ==========================
    // Xóa ảnh không còn sử dụng
    // ==========================

    for (ReviewImage oldImage : oldImages) {

        if (!newUrls.contains(oldImage.getImageUrl())) {

            try {

                String publicId =
                        extractPublicId(oldImage.getImageUrl());

                cloudinaryService.deleteImage(publicId);

            } catch (Exception e) {

                e.printStackTrace();

            }

            reviewImageRepository.delete(oldImage);
        }
    }

    // Đẩy lệnh DELETE xuống database
    reviewImageRepository.flush();

    // ==========================
    // Thêm ảnh mới
    // ==========================

    int displayOrder = 0;

    for (String url : newUrls) {

        boolean existed = oldImages.stream()
                .anyMatch(img -> img.getImageUrl().equals(url));

        if (!existed) {

            ReviewImage image = ReviewImage.builder()
                    .review(review)
                    .imageUrl(url)
                    .displayOrder(displayOrder++)
                    .build();

            reviewImageRepository.save(image);

        } else {

            displayOrder++;

        }
    }

    // Đẩy lệnh INSERT xuống database
    reviewImageRepository.flush();

    // ==========================
    // Load lại entity mới nhất
    // ==========================

    review = reviewRepository.findById(reviewId)
            .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy đánh giá."));

    return toResponse(review);
}

    @Override
    public void deleteReview(String username,
                             Integer reviewId) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy người dùng."));

        CommentEvaluation review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy đánh giá."));

        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá.");
        }

        // ===============================
        // Xóa ảnh trên Cloudinary
        // ===============================
        List<ReviewImage> images =
                reviewImageRepository
                        .findByReviewReviewIdOrderByDisplayOrderAsc(reviewId);

        for (ReviewImage image : images) {

            try {

                String publicId = extractPublicId(image.getImageUrl());

                cloudinaryService.deleteImage(publicId);

            } catch (Exception e) {

                e.printStackTrace();

            }

        }

        // Xóa review (Cascade sẽ xóa ReviewImage nếu đã cấu hình)
        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Integer productId) {

        return reviewRepository
                .findByProductProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy public_id từ URL Cloudinary
     *
     * Ví dụ:
     * https://res.cloudinary.com/.../upload/v1753000/marcus-store/abc123.jpg
     *
     * =>
     * marcus-store/abc123
     */
    private String extractPublicId(String imageUrl) {

        String temp = imageUrl.substring(
                imageUrl.indexOf("/upload/") + 8
        );

        // bỏ version nếu có
        if (temp.matches("^v\\d+/.*")) {
            temp = temp.substring(temp.indexOf("/") + 1);
        }

        return temp.replaceFirst("\\.[^.]+$", "");
    }
}