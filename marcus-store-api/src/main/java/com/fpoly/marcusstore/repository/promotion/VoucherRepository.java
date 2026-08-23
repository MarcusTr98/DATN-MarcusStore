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
                          AND (:isActive IS NULL OR v.isActive = :isActive)
                        ORDER BY
                            CASE
                                WHEN v.isActive = true
                                 AND v.quantity > 0
                                 AND v.startDate <= CURRENT_TIMESTAMP
                                 AND v.endDate >= CURRENT_TIMESTAMP
                                THEN 0 ELSE 1
                            END,
                            v.startDate DESC,
                            v.endDate ASC
                        """)
        Page<Voucher> searchVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("isActive") Boolean isActive,
                        Pageable pageable);

        @Query("""
                        SELECT COUNT(v) FROM Voucher v
                        WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:discountType IS NULL OR v.discountType = :discountType)
                          AND (:isActive IS NULL OR v.isActive = :isActive)
                        """)
        long countVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("isActive") Boolean isActive);

        @Query("""
                        SELECT COUNT(v) FROM Voucher v
                        WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:discountType IS NULL OR v.discountType = :discountType)
                          AND (:isActive IS NULL OR v.isActive = :isActive)
                          AND v.isActive = true
                        """)
        long countActiveVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("isActive") Boolean isActive);

        @Query("""
                        SELECT COUNT(v) FROM Voucher v
                        WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:discountType IS NULL OR v.discountType = :discountType)
                          AND (:isActive IS NULL OR v.isActive = :isActive)
                          AND v.discountType = 'PERCENT'
                        """)
        long countPercentVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("isActive") Boolean isActive);

        @Query("""
                        SELECT COUNT(v) FROM Voucher v
                        WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:discountType IS NULL OR v.discountType = :discountType)
                          AND (:isActive IS NULL OR v.isActive = :isActive)
                          AND v.discountType = 'AMOUNT'
                        """)
        long countAmountVouchers(
                        @Param("keyword") String keyword,
                        @Param("discountType") String discountType,
                        @Param("isActive") Boolean isActive);

        // MArcus Bổ sung hàm tìm Voucher
        Optional<Voucher> findByVoucherCode(String voucherCode);

        // logic lấy voucher hiển thị client
        @Query("""
                            SELECT v FROM Voucher v
                            WHERE v.isActive = true
                              AND v.quantity > 0
                              AND v.startDate <= CURRENT_TIMESTAMP
                              AND v.endDate >= CURRENT_TIMESTAMP
                              AND v.targetType = 'ALL'
                            ORDER BY v.startDate DESC, v.endDate ASC
                        """)
        List<Voucher> findAvailableVouchers();

        // Marcus thêm: tìm voucher đã hết hạn và đang active
        @Query("""
                        SELECT v FROM Voucher v
                        WHERE v.isActive = true
                          AND v.endDate < :now
                        """)
        List<Voucher> findExpiredAndActive(@Param("now") java.time.LocalDateTime now);

        // Marcus thêm: tìm voucher đã hết quantity và đang active
        @Query("""
                        SELECT v FROM Voucher v
                        WHERE v.isActive = true
                          AND v.quantity <= 0
                        """)
        List<Voucher> findOutOfStockAndActive();
}
