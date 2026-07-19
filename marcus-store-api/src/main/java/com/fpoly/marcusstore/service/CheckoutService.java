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
import com.fpoly.marcusstore.entity.promotion.FlashSaleItem;
import com.fpoly.marcusstore.entity.promotion.FlashSaleSlot;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleSlotRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
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
        @Autowired
        private AdminNotificationService notificationService;
        @Autowired
        private ShippingService shippingService; // NÂNG CẤP: Thêm ShippingService để tính trợ giá
        @Autowired
        private OrderTransactionService orderTransactionService;
        @Autowired
        private FlashSaleItemRepository flashSaleItemRepository;
        @Autowired
        private FlashSaleSlotRepository flashSaleSlotRepository;

        @Transactional(readOnly = true)
        public Integer calculateShippingFeeForCart(CalculateFeeRequestDTO req) {
                Integer currentUserId = SecurityUtils.getCurrentUserId();
                List<CartItem> cartItems = cartItemRepository.findByCart_CartId(
                                cartRepository.findByUserUserId(currentUserId)
                                                .orElseThrow(() -> new RuntimeException("Giỏ hàng rỗng"))
                                                .getCartId());

                if (cartItems.isEmpty())
                        return 0;

                int totalWeightGram = 0;
                int totalAmount = 0;

                for (CartItem item : cartItems) {
                        int weight = item.getSku().getWeightGram() != null ? item.getSku().getWeightGram() : 500;
                        totalWeightGram += (weight * item.getQuantity());
                        totalAmount += item.getSku().getPrice().intValue() * item.getQuantity();
                }

                return ghnService.calculateShippingFee(req.getToDistrictId(), req.getToWardCode(), totalWeightGram,
                                totalAmount);
        }

        @Transactional
        public Order processCheckout(CheckoutRequestDTO req) {
                log.info("📥 [CHECKOUT API] Dữ liệu Frontend gửi lên: Name={}, Phone={}, District={}, Ward={}",
                                req.getRecipientName(), req.getRecipientPhone(), req.getToDistrictId(),
                                req.getToWardCode());

                if (req.getToDistrictId() == null || req.getToWardCode() == null || req.getToWardCode().isBlank()) {
                        throw new RuntimeException(
                                        "Lỗi hệ thống: Dữ liệu Quận/Huyện hoặc Phường/Xã bị trống từ Frontend gửi lên!");
                }

                Integer currentUserId = SecurityUtils.getCurrentUserId();
                User user = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

                List<Integer> requestedCartItemIds = req.getCartItemIds();
                List<CartItem> cartItems = cartItemRepository
                                .findByCart_User_UserIdAndCartItemIdIn(currentUserId, requestedCartItemIds);

                int requestedItemCount = new HashSet<>(requestedCartItemIds).size();
                if (cartItems.isEmpty() || cartItems.size() != requestedItemCount) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Giỏ hàng chứa sản phẩm không hợp lệ hoặc không thuộc tài khoản hiện tại.");
                }

                List<Integer> skuIds = cartItems.stream()
                                .map(item -> item.getSku().getSkuId())
                                .sorted().collect(Collectors.toList());

                List<ProductSku> lockedSkus = productSkuRepository.findByIdsForUpdate(skuIds);
                Map<Integer, ProductSku> skuMap = lockedSkus.stream()
                                .collect(Collectors.toMap(ProductSku::getSkuId, sku -> sku));

                Order order = new Order();
                order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                order.setUser(user);
                order.setRecipientName(req.getRecipientName());
                order.setRecipientPhone(req.getRecipientPhone());
                order.setShippingAddress(req.getShippingAddress());
                order.setToDistrictId(req.getToDistrictId());
                order.setToWardCode(req.getToWardCode());
                order.setPaymentMethod(req.getPaymentMethod());
                order.setPaymentStatus("COD".equalsIgnoreCase(req.getPaymentMethod()) ? "UNPAID" : "PENDING");
                order.setOrderStatus("PENDING");
                order.setDeliveryNote(req.getNote());

                BigDecimal totalAmount = BigDecimal.ZERO;
                int totalWeightGram = 0;

                for (CartItem cartItem : cartItems) {
                        ProductSku sku = skuMap.get(cartItem.getSku().getSkuId());
                        if (sku == null || !sku.getIsActive()) {
                                throw new RuntimeException(
                                                "Sản phẩm " + cartItem.getSku().getSkuCode() + " không còn tồn tại.");
                        }

                        int buyQuantity = cartItem.getQuantity();

                        // CHECK FLASH SALE: khoá dòng + kiểm tra còn hàng trước khi cộng soldQuantity
                        // Tránh lỗi CHECK constraint CK_FlashSaleItems_Qty khi khách khác đã mua hết
                        if (cartItem.getFlashSaleSlot() != null) {
                                Integer slotId = cartItem.getFlashSaleSlot().getSlotId();
                                Integer skuId = sku.getSkuId();

                                // VALIDATE SLOT: chặn đặt hàng khi admin đã hủy Flash Sale (status=4)
                                // hoặc slot đã kết thúc/hết hạn (status=3) hoặc ngoài khung giờ.
                                // Tầng bảo vệ quan trọng nhất — bắt buộc phải có vì:
                                // 1. User có thể bypass UI modal (DevTools, refresh nhanh, gọi thẳng API).
                                // 2. Cart có thể chứa SP FS từ slot đã bị admin hủy SAU khi user thêm vào giỏ.
                                // Nếu vi phạm → throw 409 CONFLICT với mã lỗi FS_CANCELLED để FE nhận biết.
                                FlashSaleSlot slot = flashSaleSlotRepository.findById(slotId)
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.CONFLICT,
                                                                "FLASH_SALE_NOT_FOUND|Slot Flash Sale #" + slotId
                                                                                + " không còn tồn tại."));

                                Short slotStatus = slot.getStatus();
                                if (slotStatus == null) {
                                        throw new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "FLASH_SALE_INVALID|Slot Flash Sale '" + slot.getName()
                                                                        + "' có trạng thái không hợp lệ.");
                                }

                                // Status 4 = CANCELLED → admin đã hủy
                                if (slotStatus == 4) {
                                        throw new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "FLASH_SALE_CANCELLED|Flash Sale '" + slot.getName()
                                                                        + "' đã bị admin hủy. Vui lòng xóa sản phẩm khỏi giỏ hàng.");
                                }

                                // Status 3 = ENDED → slot đã kết thúc (scheduler tự chuyển)
                                if (slotStatus == 3) {
                                        throw new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "FLASH_SALE_ENDED|Flash Sale '" + slot.getName()
                                                                        + "' đã kết thúc. Vui lòng xóa sản phẩm khỏi giỏ hàng.");
                                }

                                // Status 0 = xóa mềm / archived
                                if (slotStatus == 0) {
                                        throw new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "FLASH_SALE_UNAVAILABLE|Flash Sale '" + slot.getName()
                                                                        + "' không còn khả dụng.");
                                }

                                // Check thời gian hiệu lực (phòng trường hợp scheduler chưa kịp chuyển status)
                                LocalDateTime now = LocalDateTime.now();
                                if (now.isBefore(slot.getStartDate())) {
                                        throw new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "FLASH_SALE_NOT_STARTED|Flash Sale '" + slot.getName()
                                                                        + "' chưa bắt đầu.");
                                }
                                if (!now.isBefore(slot.getEndDate())) {
                                        throw new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "FLASH_SALE_ENDED|Flash Sale '" + slot.getName()
                                                                        + "' đã kết thúc.");
                                }

                                FlashSaleItem fsi = flashSaleItemRepository
                                                .findForUpdate(slotId, skuId)
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.CONFLICT,
                                                                "FLASH_SALE_CANCELLED|Flash Sale cho sản phẩm "
                                                                                + sku.getSkuCode()
                                                                                + " không còn khả dụng (có thể đã bị admin hủy)."));

                                int remaining = fsi.getFlashSaleQuantity() - fsi.getSoldQuantity();
                                if (remaining < buyQuantity) {
                                        throw new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "FLASH_SALE_OUT_OF_STOCK|Sản phẩm " + sku.getSkuCode()
                                                                        + " đã hết Flash Sale (còn " + remaining
                                                                        + ", bạn đặt " + buyQuantity
                                                                        + "). Vui lòng chọn sản phẩm khác.");
                                }

                                fsi.setSoldQuantity(fsi.getSoldQuantity() + buyQuantity);
                                flashSaleItemRepository.save(fsi);
                        }

                        int currentStock = sku.getStockQuantity();

                        if (currentStock < buyQuantity) {
                                throw new RuntimeException(
                                                "Sản phẩm " + sku.getSkuCode() + " không đủ số lượng. Tồn kho: "
                                                                + currentStock);
                        }

                        sku.setStockQuantity(currentStock - buyQuantity);
                        // XỬ LÝ GIÁ FLASH SALE (THÊM MỚI)
                        BigDecimal priceAtPurchase;
                        Boolean isFlashSale = false;
                        BigDecimal originalPrice = null;
                        String flashSaleSlotName = null;

                        if (cartItem.getFlashSaleSlot() != null && cartItem.getFlashSalePrice() != null) {
                                // Sản phẩm Flash Sale - dùng giá Flash Sale
                                priceAtPurchase = cartItem.getFlashSalePrice();
                                isFlashSale = true;
                                originalPrice = sku.getPrice();
                                flashSaleSlotName = cartItem.getFlashSaleSlot().getName();
                        } else {
                                // Sản phẩm thường - dùng giá SKU
                                priceAtPurchase = sku.getPrice();
                        }

                        OrderItem orderItem = new OrderItem();
                        orderItem.setOrder(order);
                        orderItem.setSku(sku);
                        orderItem.setQuantity(buyQuantity);
                        orderItem.setPriceAtPurchase(priceAtPurchase);
                        // Set thông tin Flash Sale (THÊM MỚI)
                        orderItem.setIsFlashSale(isFlashSale);
                        orderItem.setOriginalPrice(originalPrice);
                        orderItem.setFlashSaleSlotName(flashSaleSlotName);
                        orderItem.setFlashSaleSlot(cartItem.getFlashSaleSlot());
                        order.getOrderItems().add(orderItem);

                        totalAmount = totalAmount.add(priceAtPurchase.multiply(BigDecimal.valueOf(buyQuantity)));
                        int itemWeight = (sku.getWeightGram() != null ? sku.getWeightGram() : 500) * buyQuantity;
                        totalWeightGram += itemWeight;
                }

                Integer rawShippingFee = ghnService.calculateShippingFee(
                                req.getToDistrictId(), req.getToWardCode(), totalWeightGram, totalAmount.intValue());
                BigDecimal ghnStandardFee = BigDecimal.valueOf(rawShippingFee);

                var shippingCalc = shippingService.calculateFinalShipping(totalAmount, ghnStandardFee);
                BigDecimal discountedShippingFee = shippingCalc.getDiscountedShippingFee(); // Phí khách thực trả
                BigDecimal shopShippingSubsidy = ghnStandardFee.subtract(discountedShippingFee); // Tiền shop bù

                BigDecimal discountAmount = BigDecimal.ZERO;
                BigDecimal freeshipAmount = BigDecimal.ZERO;
                Voucher voucher = null;
                Integer appliedVoucherId = null;

                if (req.getVoucherCode() != null && !req.getVoucherCode().trim().isEmpty()) {
                        ApplyVoucherRequest applyReq = ApplyVoucherRequest.builder()
                                        .voucherCode(req.getVoucherCode())
                                        .orderAmount(totalAmount)
                                        .shippingFee(ghnStandardFee) // Gửi phí ship gốc để Voucher xử lý
                                        .build();

                        VoucherApplyResult result = voucherService.applyVoucher(applyReq, currentUserId);

                        if (!result.isApplied()) {
                                String reason = result.getMessage();
                                String code = result.getErrorCode();
                                String fullReason = (code != null && !code.isBlank()) ? reason + "|" + code : reason;
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fullReason);
                        }

                        discountAmount = result.getDiscountAmount() != null ? result.getDiscountAmount()
                                        : BigDecimal.ZERO;
                        freeshipAmount = result.getFreeshipAmount() != null ? result.getFreeshipAmount()
                                        : BigDecimal.ZERO;
                        appliedVoucherId = result.getVoucherId();
                        voucher = voucherRepository.findById(appliedVoucherId).orElse(null);
                        order.setVoucher(voucher);
                }

                // Lưu giữ liệu dòng tiền gốc vào Database
                order.setTotalAmount(totalAmount);
                order.setShippingFee(ghnStandardFee);
                order.setShippingSubsidy(shopShippingSubsidy); // Lưu 60K trợ giá vào DB
                order.setDiscountAmount(discountAmount);

                // Tính Final Amount bằng toán học chặt chẽ
                BigDecimal finalAmount = totalAmount
                                .add(discountedShippingFee) // Cộng phí ship ĐÃ TRỪ TRỢ GIÁ
                                .subtract(discountAmount)
                                .subtract(freeshipAmount);

                order.setFinalAmount(finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount);

                Order savedOrder = orderRepository.save(order);

                // (Đã cập nhật soldQuantity cho Flash Sale ngay trong vòng lặp kiểm tra cart
                // phía trên
                // để tránh vượt quá flashSaleQuantity và nổ CHECK constraint
                // CK_FlashSaleItems_Qty)

                String transactionType = "COD".equalsIgnoreCase(savedOrder.getPaymentMethod())
                                ? "COD_COLLECTION"
                                : savedOrder.getPaymentMethod() + "_PAYMENT";

                orderTransactionService.recordTransaction(
                                savedOrder,
                                savedOrder.getFinalAmount(),
                                transactionType,
                                "PENDING",
                                "Khởi tạo giao dịch chờ thanh toán cho đơn hàng mới");
                if (appliedVoucherId != null) {
                        voucherService.confirmVoucherUsage(appliedVoucherId, currentUserId);
                }

                OrderStatusHistory createdHistory = new OrderStatusHistory();
                createdHistory.setOrder(savedOrder);
                createdHistory.setStatus("CREATED");
                createdHistory.setTitle("Tạo đơn");
                createdHistory.setCreatedBy(user);
                orderStatusHistoryRepository.save(createdHistory);

                OrderStatusHistory pendingHistory = new OrderStatusHistory();
                pendingHistory.setOrder(savedOrder);
                pendingHistory.setStatus("PENDING");
                pendingHistory.setTitle("Đơn hàng đã đặt");
                pendingHistory.setCreatedBy(user);
                orderStatusHistoryRepository.save(pendingHistory);

                cartItemRepository.deleteAll(cartItems);

                try {
                        String notifTitle = "Đơn hàng mới: " + savedOrder.getOrderCode();
                        java.text.NumberFormat formatVN = java.text.NumberFormat
                                        .getInstance(new java.util.Locale("vi", "VN"));
                        String notifMessage = "Khách hàng " + savedOrder.getRecipientName()
                                        + " vừa đặt một đơn hàng trị giá "
                                        + formatVN.format(savedOrder.getFinalAmount()) + "đ.";
                        notificationService.createAndSendNotification("ORDER", notifTitle, notifMessage,
                                        savedOrder.getOrderCode());
                } catch (Exception e) {
                        log.error("[Cảnh báo] Lỗi khi bắn thông báo WebSocket", e);
                }

                return savedOrder;
        }
}
