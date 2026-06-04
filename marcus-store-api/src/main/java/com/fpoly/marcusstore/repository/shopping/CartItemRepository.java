package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
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

}