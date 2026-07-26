package com.fpoly.marcusstore.repository.contact;

import com.fpoly.marcusstore.entity.contact.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Integer> {

    // Quả chuông chỉ đếm những thằng chưa đọc
    long countByIsReadFalse();

    // Marcus thêm: chuông có lịch sử, phân trang và bộ lọc chưa đọc.
    Page<AdminNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AdminNotification> findByIsReadFalseOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE AdminNotification n SET n.isRead = true WHERE n.isRead = false")
    int markAllAsRead();
}
