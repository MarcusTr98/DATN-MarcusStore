package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.WarrantyAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarrantyAttachmentRepository extends JpaRepository<WarrantyAttachment, Integer> {
    
    List<WarrantyAttachment> findByWarrantyReturnWarrantyIdOrderByCreatedAtAsc(Integer warrantyId);
    
    void deleteByWarrantyReturnWarrantyId(Integer warrantyId);
}
