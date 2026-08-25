package com.fpoly.marcusstore.repository.promotion;

import com.fpoly.marcusstore.entity.shopping.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
        Boolean existsByVoucherCode(String voucherCode);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT v FROM Voucher v WHERE v.voucherId = :voucherId")
        Optional<Voucher> findByIdForUpdate(@Param("voucherId") Integer voucherId);

        @Query("""
                        SELECT v FROM Voucher v
                        WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:discountType IS NULL OR v.discountType = :discountType)
                          AND (:status IS NULL OR v.status = :status)
                        ORDER BY
                            CASE
                                WHEN v.status = 'ACTIVE'
                                 AND v.quantity > 0
                                 AND v.startDate <= CURRENT_TIMESTAMP
                                 AND v.endDate >= CURRENT_TIMESTAMP
                                THEN 0
                                WHEN v.status = 'SCHEDULED'
                                THEN 1
                                ELSE 2
                            END,
                            v.startDate DESC,
                            v.endDate ASC
                        """)
        Page<Voucher> searchVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("status") String status,
                        Pageable pageable);

        @Query("""
                        SELECT COUNT(v) FROM Voucher v
                        WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:discountType IS NULL OR v.discountType = :discountType)
                          AND (:status IS NULL OR v.status = :status)
                        """)
        long countVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("status") String status);

        @Query("""
                        SELECT COUNT(v) FROM Voucher v
                        WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:discountType IS NULL OR v.discountType = :discountType)
                          AND (:status IS NULL OR v.status = :status)
                          AND v.status = 'ACTIVE'
                        """)
        long countActiveVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("status") String status);

        @Query("""
                        SELECT COUNT(v) FROM Voucher v
                        WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:discountType IS NULL OR v.discountType = :discountType)
                          AND (:status IS NULL OR v.status = :status)
                          AND v.discountType = 'PERCENT'
                        """)
        long countPercentVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("status") String status);

        @Query("""
                        SELECT COUNT(v) FROM Voucher v
                        WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:discountType IS NULL OR v.discountType = :discountType)
                          AND (:status IS NULL OR v.status = :status)
                          AND v.discountType = 'AMOUNT'
                        """)
        long countAmountVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("status") String status);

        // Marcus Bổ sung hàm tìm Voucher
        Optional<Voucher> findByVoucherCode(String voucherCode);

        // logic lấy voucher hiển thị client
        @Query("""
                            SELECT v FROM Voucher v
                            WHERE v.status = 'ACTIVE'
                              AND v.quantity > 0
                              AND v.startDate <= CURRENT_TIMESTAMP
                              AND v.endDate >= CURRENT_TIMESTAMP
                              AND v.targetType = 'ALL'
                            ORDER BY v.startDate DESC, v.endDate ASC
                        """)
        List<Voucher> findAvailableVouchers();

        // Marcus thêm: tìm voucher đã hết hạn và đang active hoặc đã lên lịch
        @Query("""
                        SELECT v FROM Voucher v
                        WHERE (v.status = 'ACTIVE' OR v.status = 'SCHEDULED')
                          AND v.endDate < :now
                        """)
        List<Voucher> findExpiredAndActive(@Param("now") java.time.LocalDateTime now);

        // Marcus thêm: tìm voucher đã hết quantity và đang active
        @Query("""
                        SELECT v FROM Voucher v
                        WHERE v.status = 'ACTIVE'
                          AND v.quantity <= 0
                        """)
        List<Voucher> findOutOfStockAndActive();

        // Tìm voucher đã đến ngày bắt đầu và đang ở trạng thái SCHEDULED -> chuyển sang ACTIVE
        @Query("""
                        SELECT v FROM Voucher v
                        WHERE v.status = 'SCHEDULED'
                          AND v.startDate <= :now
                          AND v.endDate >= :now
                        """)
        List<Voucher> findScheduledToActivate(@Param("now") java.time.LocalDateTime now);
}
