package com.fpoly.marcusstore.repository.statistics;

import com.fpoly.marcusstore.entity.interaction.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository
        extends JpaRepository<ReviewImage, Integer> {

    List<ReviewImage> findByReviewReviewIdOrderByDisplayOrderAsc(Integer reviewId);
    void deleteByReviewReviewId(Integer reviewId);

    void delete(ReviewImage image);
}