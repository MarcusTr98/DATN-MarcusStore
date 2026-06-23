package com.fpoly.marcusstore.repository.promotion;

import com.fpoly.marcusstore.entity.shopping.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Integer> {

    List<UserVoucher> findByUserUserId(Integer userId);

    List<UserVoucher> findByVoucherVoucherId(Integer voucherId);

    Optional<UserVoucher> findByVoucherVoucherIdAndUserUserId(Integer voucherId, Integer userId);

    boolean existsByVoucherVoucherIdAndUserUserId(Integer voucherId, Integer userId);

    @Query("SELECT uv FROM UserVoucher uv JOIN FETCH uv.voucher v " +
           "WHERE uv.user.userId = :userId " +
           "AND uv.isUsed = false " +
           "AND v.isActive = true " +
           "AND v.startDate <= :now " +
           "AND v.endDate >= :now")
    List<UserVoucher> findAvailableVouchersByUserId(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
}
