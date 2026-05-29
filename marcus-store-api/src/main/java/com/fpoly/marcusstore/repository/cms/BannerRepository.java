package com.fpoly.marcusstore.repository.cms;

import com.fpoly.marcusstore.entity.cms.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {
    // Lấy Banner đang Active và nằm trong thời gian cho phép hiển thị
    @Query("SELECT b FROM Banner b WHERE b.bannerPosition.positionCode = :positionCode " +
            "AND b.isActive = true " +
            "AND (b.startDate IS NULL OR b.startDate <= :now) " +
            "AND (b.endDate IS NULL OR b.endDate >= :now) " +
            "ORDER BY b.displayOrder ASC")
    List<Banner> findActiveBannersByPosition(String positionCode, LocalDateTime now);
}