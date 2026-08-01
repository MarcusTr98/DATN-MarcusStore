package com.fpoly.marcusstore.repository.cms;

import com.fpoly.marcusstore.entity.cms.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<Banner> findActiveBannersByPosition(@Param("positionCode") String positionCode,
                                             @Param("now") LocalDateTime now);

    // Kiểm tra trùng displayOrder trong cùng 1 vị trí slider khi THÊM mới
    @Query("SELECT COUNT(b) > 0 FROM Banner b " +
            "WHERE b.bannerPosition.positionId = :positionId " +
            "AND b.displayOrder = :displayOrder")
    boolean existsByPositionIdAndDisplayOrder(@Param("positionId") Integer positionId,
                                              @Param("displayOrder") Integer displayOrder);

    // Kiểm tra trùng displayOrder trong cùng 1 vị trí slider khi SỬA
    @Query("SELECT COUNT(b) > 0 FROM Banner b " +
            "WHERE b.bannerPosition.positionId = :positionId " +
            "AND b.displayOrder = :displayOrder " +
            "AND b.bannerId <> :excludeId")
    boolean existsByPositionIdAndDisplayOrderExcluding(@Param("positionId") Integer positionId,
                                                       @Param("displayOrder") Integer displayOrder,
                                                       @Param("excludeId") Integer excludeId);
}