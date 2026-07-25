package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.ReviewReplyRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ProductReviewItemResponse;
import com.fpoly.marcusstore.dto.response.RatingStatisticResponse;
import com.fpoly.marcusstore.dto.response.ReviewAdminResponse;
import com.fpoly.marcusstore.dto.response.ReviewResponse;
import com.fpoly.marcusstore.security.CustomUserDetails;
import com.fpoly.marcusstore.service.ReviewAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class ReviewAdminController {

    private final ReviewAdminService reviewAdminService;
@GetMapping
public ApiResponse<Page<ReviewAdminResponse>> getAllReviews(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size) {

    return ApiResponse.success(
            reviewAdminService.getAllReviews(page, size)
    );
}

    @PostMapping("/{reviewId}/reply")
public ResponseEntity<ApiResponse<?>> replyReview(
        @PathVariable Integer reviewId,
        @Valid @RequestBody ReviewReplyRequest request,
        Authentication authentication) {

    CustomUserDetails user =
            (CustomUserDetails) authentication.getPrincipal();

    reviewAdminService.replyReview(
            reviewId,
            user.getUserId(),
            request);

    return ResponseEntity.ok(
            ApiResponse.success("Trả lời thành công"));
}
@DeleteMapping("/{reviewId}")
public ApiResponse<?> deleteReview(
        @PathVariable Integer reviewId) {

    reviewAdminService.deleteReview(reviewId);

    return ApiResponse.success("Xóa đánh giá thành công");
}
@PutMapping("/{reviewId}/reply")
public ApiResponse<?> updateReply(
        @PathVariable Integer reviewId,
        @Valid @RequestBody ReviewReplyRequest request,
        Authentication authentication){

    CustomUserDetails user =
            (CustomUserDetails) authentication.getPrincipal();

    reviewAdminService.updateReply(
            reviewId,
            user.getUserId(),
            request);

    return ApiResponse.success("Sửa phản hồi thành công");
}
@GetMapping("/search")
public ApiResponse<?> search(

        @RequestParam(required = false) String keyword,

        @RequestParam(required = false) Integer rating,

        @RequestParam(required = false) Boolean replied,

        @RequestParam(defaultValue="0") int page,

        @RequestParam(defaultValue="10") int size){

    return ApiResponse.success(

            reviewAdminService.search(
                    keyword,
                    rating,
                    replied,
                    PageRequest.of(page,size)
            )

    );
}

@GetMapping("/statistics/{productId}")
public ApiResponse<List<RatingStatisticResponse>> statistics(
        @PathVariable Integer productId){

    return ApiResponse.success(
            reviewAdminService.getRatingStatistics(productId)
    );

}

@GetMapping("/products")
public ApiResponse<List<ProductReviewItemResponse>> products(){

    return ApiResponse.success(
            reviewAdminService.getProductsHasReview()
    );

}
}