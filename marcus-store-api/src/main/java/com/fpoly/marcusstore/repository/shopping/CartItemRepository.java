package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCart_CartId(Integer CartId);

    Optional<CartItem> findByCart_CartIdAndSku_SkuId(Integer cartId, Integer skuId);

    void deleteByCart_CartIdAndSku_SkuId(Integer cartId, Integer skuId);

    void deleteByCart_CartId(Integer cartId);

    void deleteByCart_CartIdAndSku_SkuIdIn(Integer cartId, List<Integer> skuIds);


    // Marcus thêm
    List<CartItem> findAllById(Iterable<Integer> ids);
    List<CartItem> findByCart_User_UserIdAndCartItemIdIn(Integer userId, List<Integer> cartItemIds);

    // Marcus thêm: Clear tham chiếu Flash Sale trên cart items khi admin hủy slot.
    // Khi slot bị CANCELLED, set flash_sale_slot_id = NULL và flash_sale_price = NULL
    // cho tất cả cart_item đang trỏ vào slot đó (chưa thanh toán/đặt hàng).
    // Lý do: tránh trường hợp giá đã revert về gốc trên server nhưng cart_item vẫn giữ FK
    // làm CheckoutService throw 409 FLASH_SALE_CANCELLED dù giá đã là giá gốc.
    @Modifying
    @Query("UPDATE CartItem c SET c.flashSaleSlot = NULL, c.flashSalePrice = NULL " +
           "WHERE c.flashSaleSlot.slotId = :slotId")
    int clearFlashSaleReference(@Param("slotId") Integer slotId);


}
