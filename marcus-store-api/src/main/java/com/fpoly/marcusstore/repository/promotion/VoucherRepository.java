package com.fpoly.marcusstore.repository.promotion;

import com.fpoly.marcusstore.entity.shopping.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Boolean existsByVoucherCode(String voucherCode);

    @Query("""
            SELECT v FROM Voucher v
            WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:discountType IS NULL OR v.discountType = :discountType)
              AND (:isActive IS NULL OR v.isActive = :isActive)
            """)
    Page<Voucher> searchVouchers(
            @Param("keyword") String keyword,
            @Param("discountType") String discountType,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(v) FROM Voucher v
            WHERE (:keyword IS NULL OR LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:discountType IS NULL OR v.discountType = :discountType)
              AND (:isActive IS NULL OR v.isActive = :isActive)
            """)
    long countVouchers(
            @Param("keyword") String keyword,
            @Param("discountType") String discountType,
            @Param("isActive") Boolean isActive
    );

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
            @Param("isActive") Boolean isActive
    );

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
            @Param("isActive") Boolean isActive
    );

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
            @Param("isActive") Boolean isActive
    );
}
