package com.fpoly.marcusstore.repository.statistics;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fpoly.marcusstore.entity.interaction.ReviewReply;



@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Integer>{

    Optional<ReviewReply> findByReviewReviewId(Integer reviewId);

    boolean existsByReviewReviewId(Integer reviewId);

    

}