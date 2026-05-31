package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    Optional<CartItem> findByCart_CartIdAndSku_SkuId(Integer cartId, Integer skuId);

}