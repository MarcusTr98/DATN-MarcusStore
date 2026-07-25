package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.CreateReviewRequest;
import com.fpoly.marcusstore.dto.request.ReviewRequest;
import com.fpoly.marcusstore.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {


    ReviewResponse createReview(String username,
                            Integer orderItemId,
                            CreateReviewRequest request);


    ReviewResponse updateReview(String username,
                                Integer reviewId,
                                CreateReviewRequest request);


    void deleteReview(String username,
                      Integer reviewId);

                      
    List<ReviewResponse> getReviewsByProduct(Integer productId);

    ReviewResponse getMyReview(
        String username,
        Integer orderItemId
);

}