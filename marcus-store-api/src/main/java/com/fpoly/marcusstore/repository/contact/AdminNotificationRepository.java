package com.fpoly.marcusstore.repository.contact;

import com.fpoly.marcusstore.entity.contact.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Integer> {

    // Luôn luôn lấy 10 thông báo mới nhất bất kể đọc hay chưa để làm lịch sử dòng
    // thời gian
    List<AdminNotification> findTop10ByOrderByCreatedAtDesc();

    List<AdminNotification> findTop20ByIsReadFalseOrderByCreatedAtDesc();

    // Quả chuông chỉ đếm những thằng chưa đọc
    long countByIsReadFalse();

    @Modifying
    @Transactional
    @Query("UPDATE AdminNotification n SET n.isRead = true WHERE n.isRead = false")
    void markAllAsRead();
}