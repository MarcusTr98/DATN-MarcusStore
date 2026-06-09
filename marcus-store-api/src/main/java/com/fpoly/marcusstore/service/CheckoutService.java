package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.CheckoutRequestDTO;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.CartItem;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Order processCheckout(CheckoutRequestDTO req) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

        // Lấy danh sách CartItem khách muốn mua
        List<CartItem> cartItems = cartItemRepository.findAllById(req.getCartItemIds());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng rỗng hoặc không tìm thấy sản phẩm hợp lệ.");
        }

        // Lấy danh sách SKU ID và sắp xếp để tránh Deadlock trong DB
        List<Integer> skuIds = cartItems.stream()
                .map(item -> item.getSku().getSkuId())
                .sorted()
                .collect(Collectors.toList());

        // Pessimistic Lock-Khóa chặt các SKU này trong db
        List<ProductSku> lockedSkus = productSkuRepository.findByIdsForUpdate(skuIds);
        Map<Integer, ProductSku> skuMap = lockedSkus.stream()
                .collect(Collectors.toMap(ProductSku::getSkuId, sku -> sku));

        // tạo Đơn hàng
        Order order = new Order();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(user);
        order.setRecipientName(req.getRecipientName());
        order.setRecipientPhone(req.getRecipientPhone());
        order.setShippingAddress(req.getShippingAddress());
        order.setPaymentMethod(req.getPaymentMethod());
        order.setPaymentStatus("COD".equalsIgnoreCase(req.getPaymentMethod()) ? "UNPAID" : "PENDING");
        order.setOrderStatus("PENDING");

        BigDecimal totalAmount = BigDecimal.ZERO;

        // duyệt từng món trong giỏ, kiểm tra tồn kho & trừ kho
        for (CartItem cartItem : cartItems) {
            ProductSku sku = skuMap.get(cartItem.getSku().getSkuId());
            if (sku == null || !sku.getIsActive()) {
                throw new RuntimeException(
                        "Sản phẩm " + cartItem.getSku().getSkuCode() + " không còn tồn tại hoặc đã bị ẩn.");
            }

            int buyQuantity = cartItem.getQuantity();
            int currentStock = sku.getStockQuantity();

            // Bán âm kho bị chặn tại đây
            if (currentStock < buyQuantity) {
                throw new RuntimeException(
                        "Sản phẩm " + sku.getSkuCode() + " không đủ số lượng. Tồn kho chỉ còn: " + currentStock);
            }

            // Trừ kho (Deduct Stock)
            sku.setStockQuantity(currentStock - buyQuantity);

            // Tạo OrderItem (Snapshot giá tại thời điểm mua)
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setSku(sku);
            orderItem.setQuantity(buyQuantity);
            orderItem.setPriceAtPurchase(sku.getPrice());

            order.getOrderItems().add(orderItem);

            // Tính tiền: (Giá * Số lượng)
            BigDecimal itemTotal = sku.getPrice().multiply(BigDecimal.valueOf(buyQuantity));
            totalAmount = totalAmount.add(itemTotal);
        }

        // Tính tổng tiền cuối cùng (Đạt sẽ nhúng logic Voucher vào sau)
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO); // Tạm set 0
        order.setFinalAmount(totalAmount.subtract(order.getDiscountAmount()));

        // Lưu đơn hàng
        Order savedOrder = orderRepository.save(order);

        // Xóa các món đã mua khỏi giỏ hàng
        cartItemRepository.deleteAll(cartItems);

        return savedOrder;
    }
}