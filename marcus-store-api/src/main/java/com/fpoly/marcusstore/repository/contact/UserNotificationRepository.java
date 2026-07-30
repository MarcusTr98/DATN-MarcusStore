package com.fpoly.marcusstore.repository.contact;

import com.fpoly.marcusstore.entity.contact.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {
    Page<UserNotification> findByUserUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByUserUserIdAndIsReadFalse(Integer userId);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Integer userId);
}
