package com.fpoly.marcusstore.repository.contact;

import com.fpoly.marcusstore.entity.contact.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Integer> {

    // Quả chuông chỉ đếm những thằng chưa đọc
    long countByIsReadFalse();

    // Marcus thêm: chuông có lịch sử, phân trang và bộ lọc chưa đọc.
    Page<AdminNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AdminNotification> findByIsReadFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<AdminNotification> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    Optional<AdminNotification> findByEventKey(String eventKey);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("UPDATE AdminNotification n SET n.isRead = true WHERE n.isRead = false")
    int markAllAsRead();
}
