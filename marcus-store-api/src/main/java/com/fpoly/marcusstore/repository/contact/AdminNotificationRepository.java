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

    // lấy 20 thông báo chưa đọc mới nhất, sắp xếp từ mới nhất đến cũ
    List<AdminNotification> findTop20ByIsReadFalseOrderByCreatedAtDesc();

    // UPDATE tất cả là đã đọc
    @Modifying
    @Transactional
    @Query("UPDATE AdminNotification n SET n.isRead = true WHERE n.isRead = false")
    void markAllAsRead();
}