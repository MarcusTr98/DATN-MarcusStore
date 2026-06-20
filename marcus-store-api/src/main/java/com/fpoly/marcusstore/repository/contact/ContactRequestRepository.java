package com.fpoly.marcusstore.repository.contact;

import com.fpoly.marcusstore.entity.contact.ContactRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, Integer> {

    // Lấy danh sách đổ lên bảng Admin, sắp xếp cái nào mới gửi lên đầu
    Page<ContactRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}