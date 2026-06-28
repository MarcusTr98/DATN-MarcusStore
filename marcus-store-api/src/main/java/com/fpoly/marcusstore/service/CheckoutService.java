package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.ApplyVoucherRequest;
import com.fpoly.marcusstore.dto.request.CalculateFeeRequestDTO;
import com.fpoly.marcusstore.dto.request.CheckoutRequestDTO;
import com.fpoly.marcusstore.dto.response.VoucherApplyResult;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.CartItem;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
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
    private CartRepository cartRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private GhnService ghnService;

    // Bổ sung Inject Notification Service để bắn thông báo
    @Autowired
    private AdminNotificationService notificationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate getTransactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    // Hàm tính phí ship cho Frontend gọi real-time
    @Transactional(readOnly = true)
    public Integer calculateShippingFeeForCart(CalculateFeeRequestDTO req) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();

        // 1. Lấy giỏ hàng của User
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(
                cartRepository.findByUserUserId(currentUserId)
                        .orElseThrow(() -> new RuntimeException("Giỏ hàng rỗng"))
                        .getCartId());

        if (cartItems.isEmpty())
            return 0;

        // 2. Tính tổng khối lượng ship
        int totalWeightGram = 0;
        for (CartItem item : cartItems) {
            int weight = item.getSku().getWeightGram() != null ? item.getSku().getWeightGram() : 500;
            totalWeightGram += (weight * item.getQuantity());
        }

        // 3. GHN Service
        return ghnService.calculateShippingFee(req.getToDistrictId(), req.getToWardCode(), totalWeightGram);
    }

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

        // 2. TÍNH PHÍ SHIP GHN
        Integer shippingFee = ghnService.calculateShippingFee(req.getToDistrictId(), req.getToWardCode(),
                totalWeightGram);
        BigDecimal shippingFeeDecimal = BigDecimal.valueOf(shippingFee);

        // 3. XỬ LÝ VOUCHER QUA VOUCHERSERVICE
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal freeshipAmount = BigDecimal.ZERO;
        Voucher voucher = null;
        Integer appliedVoucherId = null;
        if (req.getVoucherCode() != null && !req.getVoucherCode().trim().isEmpty()) {
            ApplyVoucherRequest applyReq = ApplyVoucherRequest.builder()
                    .voucherCode(req.getVoucherCode())
                    .orderAmount(totalAmount)
                    .shippingFee(shippingFeeDecimal)
                    .build();

            VoucherApplyResult result = voucherService.applyVoucher(applyReq, currentUserId);

            if (!result.isApplied()) {
                throw new RuntimeException(result.getMessage());
            }

            discountAmount = result.getDiscountAmount() != null ? result.getDiscountAmount() : BigDecimal.ZERO;
            freeshipAmount = result.getFreeshipAmount() != null ? result.getFreeshipAmount() : BigDecimal.ZERO;
            appliedVoucherId = result.getVoucherId();
            voucher = voucherRepository.findById(appliedVoucherId).orElse(null);
            order.setVoucher(voucher);
        }

        // 4. CHỐT SỔ ĐƠN HÀNG
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFeeDecimal);
        order.setDiscountAmount(discountAmount);

        // Final Amount = (Tiền hàng + Phí Ship - Giảm giá) - Miễn phí ship
        BigDecimal finalAmount = totalAmount.add(shippingFeeDecimal)
                .subtract(discountAmount)
                .subtract(freeshipAmount);

        // Đảm bảo không bị số âm
        order.setFinalAmount(finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount);

        // Lưu đơn hàng & dọn Giỏ hàng
        Order savedOrder = orderRepository.save(order);

        // Confirm voucher usage sau khi order lưu thành công
        // Sử dụng TransactionTemplate để confirm trong transaction riêng biệt
        if (appliedVoucherId != null) {
            final Integer finalVoucherId = appliedVoucherId;
            try {
                getTransactionTemplate().executeWithoutResult(status -> {
                    voucherService.confirmVoucherUsage(finalVoucherId, currentUserId);
                });
            } catch (Exception e) {
                System.err.println("[Cảnh báo] Lỗi khi confirm voucher, voucher vẫn được áp dụng: " + e.getMessage());
            }
        }

        OrderStatusHistory createdHistory = new OrderStatusHistory();
        createdHistory.setOrder(savedOrder);
        createdHistory.setStatus("CREATED");
        createdHistory.setTitle("Tạo đơn");
        createdHistory.setCreatedBy(user);
        orderStatusHistoryRepository.save(createdHistory);

        cartItemRepository.deleteAll(cartItems);

        // BẮN THÔNG BÁO CHO ADMIN KHI CÓ ĐƠN HÀNG MỚI
        try {
            String notifTitle = "Đơn hàng mới: " + savedOrder.getOrderCode();

            // Format số tiền theo chuẩn Việt Nam (VD: 36.540.500)
            java.text.NumberFormat formatVN = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
            String formattedAmount = formatVN.format(savedOrder.getFinalAmount());

            String notifMessage = "Khách hàng " + savedOrder.getRecipientName() + " vừa đặt một đơn hàng trị giá "
                    + formattedAmount + "đ.";

            notificationService.createAndSendNotification(
                    "ORDER",
                    notifTitle,
                    notifMessage,
                    savedOrder.getOrderCode());
        } catch (Exception e) {
            System.err.println("[Cảnh báo] Lỗi khi bắn thông báo WebSocket, đơn hàng vẫn được tạo thành công.");
            e.printStackTrace();
        }

        return savedOrder;
    }
}
