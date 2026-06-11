package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Integer> {

    // Lấy danh sách địa chỉ của User, xếp cái Mặc định lên đầu tiên
    List<UserAddress> findByUser_UserIdOrderByIsDefaultDesc(Integer userId);

    // Tìm 1 địa chỉ, nhưng BẮT BUỘC phải thuộc về User đang đăng nhập (Bảo mật)
    Optional<UserAddress> findByAddressIdAndUser_UserId(Integer addressId, Integer userId);

    // Đếm xem User này có bao nhiêu địa chỉ
    long countByUser_UserId(Integer userId);

    // Reset toàn bộ địa chỉ của User này về isDefault = false
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.userId = :userId")
    void clearDefaultAddress(@Param("userId") Integer userId);
}