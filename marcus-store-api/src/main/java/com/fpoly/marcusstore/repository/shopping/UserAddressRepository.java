package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Integer> {

    // Lấy toàn bộ địa chỉ của user, sắp xếp cái mặc định lên đầu
    @Query("SELECT u FROM UserAddress u WHERE u.user.userId = :userId ORDER BY u.isDefault DESC, u.createdAt DESC")
    List<UserAddress> findByUserIdOrderByIsDefaultDesc(Integer userId);

    // Tìm 1 địa chỉ cụ thể của đúng User đó
    Optional<UserAddress> findByAddressIdAndUser_UserId(Integer addressId, Integer userId);

    @Modifying
    @Query("UPDATE UserAddress u SET u.isDefault = false WHERE u.user.userId = :userId")
    void resetDefaultAddressForUser(Integer userId);
}