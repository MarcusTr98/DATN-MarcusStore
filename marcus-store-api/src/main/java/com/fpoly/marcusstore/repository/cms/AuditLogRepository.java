package com.fpoly.marcusstore.repository.cms;
import com.fpoly.marcusstore.entity.interaction.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<AuditLog> findAll(Pageable pageable);

    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.user ORDER BY a.createdAt DESC")
    List<AuditLog> findAllForExport();
}