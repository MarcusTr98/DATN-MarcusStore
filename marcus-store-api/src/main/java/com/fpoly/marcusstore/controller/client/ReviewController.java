package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.CreateReviewRequest;
import com.fpoly.marcusstore.dto.request.ReviewRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ReviewResponse;
import com.fpoly.marcusstore.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Xem đánh giá của sản phẩm
     */
    @GetMapping("/product/{productId}")
    public ApiResponse<List<ReviewResponse>> getReviews(
            @PathVariable Integer productId) {

        return ApiResponse.success(
                reviewService.getReviewsByProduct(productId)
        );
    }

    /**
     * Đánh giá sản phẩm từ OrderItem
     */
    @PostMapping("/order-items/{orderItemId}")
    public ApiResponse<ReviewResponse> createReview(
            Authentication authentication,
            @PathVariable Integer orderItemId,
            @Valid @RequestBody CreateReviewRequest request) {

        return ApiResponse.success(
                reviewService.createReview(
                        authentication.getName(),
                        orderItemId,
                        request
                )
        );
    }
/**
 * Lấy đánh giá của tôi theo OrderItem
 */
@GetMapping("/order-items/{orderItemId}")
public ApiResponse<ReviewResponse> getMyReview(
        Authentication authentication,
        @PathVariable Integer orderItemId) {

    return ApiResponse.success(
            reviewService.getMyReview(
                    authentication.getName(),
                    orderItemId
            )
    );
}
    /**
     * Sửa đánh giá
     */
    @PutMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(
            Authentication authentication,
            @PathVariable Integer reviewId,
            @Valid @RequestBody CreateReviewRequest request) {

        return ApiResponse.success(
                reviewService.updateReview(
                        authentication.getName(),
                        reviewId,
                        request
                )
        );
    }

    /**
     * Xóa đánh giá
     */
    @DeleteMapping("/{reviewId}")
    public ApiResponse<String> deleteReview(
            Authentication authentication,
            @PathVariable Integer reviewId) {

        reviewService.deleteReview(
                authentication.getName(),
                reviewId
        );

        return ApiResponse.success("Xóa đánh giá thành công.");
    }

    
}