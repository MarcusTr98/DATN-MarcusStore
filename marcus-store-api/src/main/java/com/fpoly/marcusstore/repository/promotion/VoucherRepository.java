package com.fpoly.marcusstore.repository.promotion;

import com.fpoly.marcusstore.entity.shopping.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Boolean existsByVoucherCode(String voucherCode);
}
