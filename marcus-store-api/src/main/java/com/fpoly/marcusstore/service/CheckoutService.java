package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.CheckoutRequestDTO;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.CartItem;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private GhnService ghnService;

    @Transactional
    public Order processCheckout(CheckoutRequestDTO req) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

        List<CartItem> cartItems = cartItemRepository.findAllById(req.getCartItemIds());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng rỗng hoặc không tìm thấy sản phẩm hợp lệ.");
        }

        List<Integer> skuIds = cartItems.stream()
                .map(item -> item.getSku().getSkuId())
                .sorted()
                .collect(Collectors.toList());

        // Chống âm kho
        List<ProductSku> lockedSkus = productSkuRepository.findByIdsForUpdate(skuIds);
        Map<Integer, ProductSku> skuMap = lockedSkus.stream()
                .collect(Collectors.toMap(ProductSku::getSkuId, sku -> sku));

        Order order = new Order();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(user);
        order.setRecipientName(req.getRecipientName());
        order.setRecipientPhone(req.getRecipientPhone());
        order.setShippingAddress(req.getShippingAddress());
        order.setPaymentMethod(req.getPaymentMethod());
        order.setPaymentStatus("COD".equalsIgnoreCase(req.getPaymentMethod()) ? "UNPAID" : "PENDING");
        order.setOrderStatus("PENDING");

        BigDecimal totalAmount = BigDecimal.ZERO; // Tổng tiền hàng
        int totalWeightGram = 0; // Tổng khối lượng (GHN)

        // 1. DUYỆT GIỎ HÀNG, TÍNH TIỀN VÀ TRỪ KHO
        for (CartItem cartItem : cartItems) {
            ProductSku sku = skuMap.get(cartItem.getSku().getSkuId());
            if (sku == null || !sku.getIsActive()) {
                throw new RuntimeException("Sản phẩm " + cartItem.getSku().getSkuCode() + " không còn tồn tại.");
            }

            int buyQuantity = cartItem.getQuantity();
            int currentStock = sku.getStockQuantity();

            if (currentStock < buyQuantity) {
                throw new RuntimeException(
                        "Sản phẩm " + sku.getSkuCode() + " không đủ số lượng. Tồn kho: " + currentStock);
            }

            // Trừ kho
            sku.setStockQuantity(currentStock - buyQuantity);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setSku(sku);
            orderItem.setQuantity(buyQuantity);
            orderItem.setPriceAtPurchase(sku.getPrice());
            order.getOrderItems().add(orderItem);

            // Cộng dồn tiền và khối lượng
            totalAmount = totalAmount.add(sku.getPrice().multiply(BigDecimal.valueOf(buyQuantity)));

            int itemWeight = (sku.getWeightGram() != null ? sku.getWeightGram() : 500) * buyQuantity;
            totalWeightGram += itemWeight;
        }

        // 2. TÍNH PHÍ SHIP (GHN)
        Integer shippingFee = ghnService.calculateShippingFee(req.getToDistrictId(), req.getToWardCode(),
                totalWeightGram);
        BigDecimal shippingFeeDecimal = BigDecimal.valueOf(shippingFee);

        // 3. XỬ LÝ VOUCHER
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (req.getVoucherCode() != null && !req.getVoucherCode().trim().isEmpty()) {
            Voucher voucher = voucherRepository.findByVoucherCode(req.getVoucherCode())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại."));

            LocalDateTime now = LocalDateTime.now();

            // Validate Trạng thái & Thời gian
            if (!Boolean.TRUE.equals(voucher.getIsActive()) ||
                    (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) ||
                    (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate()))) {
                throw new RuntimeException("Mã giảm giá đã hết hạn hoặc không hoạt động.");
            }

            // Validate Số lượng (Dùng logic Countdown)
            if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
                throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng.");
            }

            // Validate Đơn tối thiểu
            if (voucher.getMinOrderValue() != null && totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để sử dụng mã này.");
            }

            // Tính số tiền giảm
            if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
                // Giảm theo %
                BigDecimal percent = voucher.getDiscountValue().divide(BigDecimal.valueOf(100), 2,
                        RoundingMode.HALF_UP);
                discountAmount = totalAmount.multiply(percent);

                // Giới hạn số tiền giảm tối đa (Nếu có)
                if (voucher.getMaxDiscountAmount() != null
                        && discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                    discountAmount = voucher.getMaxDiscountAmount();
                }
            } else if ("AMOUNT".equalsIgnoreCase(voucher.getDiscountType())) {
                // Giảm tiền mặt
                discountAmount = voucher.getDiscountValue();
            }

            // Tránh trường hợp tiền giảm lớn hơn tiền hàng
            if (discountAmount.compareTo(totalAmount) > 0) {
                discountAmount = totalAmount;
            }

            // Ghi nhận Voucher vào Order và TRỪ LÙI số lượng
            order.setVoucher(voucher);
            voucher.setQuantity(voucher.getQuantity() - 1);
        }

        // 4. CHỐT SỔ ĐƠN HÀNG
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFeeDecimal);
        order.setDiscountAmount(discountAmount);

        // Final Amount = (Tiền hàng + Phí Ship) - Khuyến mãi
        BigDecimal finalAmount = totalAmount.add(shippingFeeDecimal).subtract(discountAmount);
        // Đảm bảo không bị số âm
        order.setFinalAmount(finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount);

        // Lưu đơn hàng & dọn Giỏ hàng
        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);

        return savedOrder;
    }
}