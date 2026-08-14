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
import com.fpoly.marcusstore.repository.cms.SystemSettingRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleSlotRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.service.analytics.BehaviorEventService;
import com.fpoly.marcusstore.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutService {

        // Marcus sửa: dùng constructor injection để dependency bắt buộc, bất biến và dễ
        // kiểm thử hơn; không thay đổi nghiệp vụ Cart/Voucher/Flash Sale của thành
        // viên.
        private final CartItemRepository cartItemRepository;
        private final CartRepository cartRepository;
        private final ProductSkuRepository productSkuRepository;
        private final OrderRepository orderRepository;
        private final OrderStatusHistoryRepository orderStatusHistoryRepository;
        private final UserRepository userRepository;
        private final VoucherRepository voucherRepository;
        private final VoucherService voucherService;
        private final GhnService ghnService;
        private final AdminNotificationService notificationService;
        private final UserNotificationService userNotificationService;
        private final ShippingService shippingService;
        private final OrderTransactionService orderTransactionService;
        private final FlashSaleItemRepository flashSaleItemRepository;
        private final FlashSaleSlotRepository flashSaleSlotRepository;
        private final SystemSettingRepository systemSettingRepository;
        private final BehaviorEventService behaviorEventService;

        // Marcus thêm: chặn một tài khoản giữ kho bằng quá nhiều đơn COD chưa được
        // Admin xác nhận. Idempotent retry được xử lý trước nên không bị tính nhầm.
        @Value("${checkout.cod.max-pending-orders:3}")
        private long maxPendingCodOrders;

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
                Integer currentUserId = SecurityUtils.getCurrentUserId();
                String checkoutRequestId = req.getCheckoutRequestId().trim();

                // Marcus thêm: đường tắt cho retry đã hoàn tất. Không đọc Cart và
                // tuyệt đối không đụng lại kho/voucher/Flash Sale.
                Order existingOrder = findExistingCheckout(checkoutRequestId, currentUserId);
                if (existingOrder != null) {
                        return existingOrder;
                }

                // Marcus thêm: khóa giỏ theo user rồi kiểm tra idempotency lần hai.
                // Request đồng thời sẽ chờ request đầu commit và nhận lại cùng đơn.
                cartRepository.findByUserIdForCheckout(currentUserId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "Giỏ hàng rỗng."));
                existingOrder = findExistingCheckout(checkoutRequestId, currentUserId);
                if (existingOrder != null) {
                        return existingOrder;
                }

                // Marcus thêm: client cũ không gửi fulfillmentMethod vẫn được xem là giao tận
                // nơi.
                String fulfillmentMethod = req.getFulfillmentMethod() == null
                                ? "DELIVERY"
                                : req.getFulfillmentMethod().trim().toUpperCase();
                if (!Set.of("DELIVERY", "STORE_PICKUP").contains(fulfillmentMethod)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Phương thức nhận hàng không hợp lệ.");
                }
                boolean isStorePickup = "STORE_PICKUP".equals(fulfillmentMethod);

                // Marcus sửa: không ghi tên, số điện thoại và địa chỉ khách vào log hệ
                // thống. Khi cần truy vết chỉ dùng userId/orderCode ở các bước phía sau.

                if (!isStorePickup
                                && (req.getShippingAddress() == null || req.getShippingAddress().isBlank()
                                                || req.getToDistrictId() == null || req.getToWardCode() == null
                                                || req.getToWardCode().isBlank())) {
                        throw new RuntimeException(
                                        "Lỗi hệ thống: Dữ liệu Quận/Huyện hoặc Phường/Xã bị trống từ Frontend gửi lên!");
                }

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
                order.setCheckoutRequestId(checkoutRequestId);
                order.setUser(user);
                order.setRecipientName(req.getRecipientName());
                order.setRecipientPhone(req.getRecipientPhone());
                order.setFulfillmentMethod(fulfillmentMethod);
                // Marcus sửa: địa chỉ nhận tại quầy do server lấy từ cấu hình, không tin dữ
                // liệu client.
                String storeAddress = systemSettingRepository.findById("ADDRESS")
                                .map(setting -> setting.getSettingValue())
                                .filter(value -> !value.isBlank())
                                .orElse("Marcus Store");
                order.setShippingAddress(isStorePickup ? storeAddress : req.getShippingAddress());
                order.setToDistrictId(isStorePickup ? null : req.getToDistrictId());
                order.setToWardCode(isStorePickup ? null : req.getToWardCode());
                // Marcus sửa: chuẩn hóa và chỉ chấp nhận phương thức do server hỗ trợ.
                // Không dùng trực tiếp chuỗi trạng thái/phương thức từ frontend.
                String paymentMethod = req.getPaymentMethod().trim().toUpperCase();
                if (!Set.of("COD", "VNPAY").contains(paymentMethod)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Phương thức thanh toán không hợp lệ.|INVALID_PAYMENT_METHOD");
                }
                validateCodPendingLimit(paymentMethod, currentUserId);
                order.setPaymentMethod(paymentMethod);
                order.setPaymentStatus("COD".equals(paymentMethod) ? "UNPAID" : "PENDING");
                order.setOrderStatus("PENDING");
                order.setGhnIntegrationStatus(isStorePickup ? "NOT_REQUIRED" : "PENDING");
                order.setGhnRetryCount(0);
                order.setDeliveryNote(req.getNote());

                BigDecimal totalAmount = BigDecimal.ZERO;
                int totalWeightGram = 0;

                for (CartItem cartItem : cartItems) {
                        ProductSku sku = skuMap.get(cartItem.getSku().getSkuId());
                        validateSkuSellable(sku, cartItem.getSku().getSkuCode());

                        Integer persistedQuantity = cartItem.getQuantity();
                        if (persistedQuantity == null || persistedQuantity <= 0 || persistedQuantity > 100) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Số lượng sản phẩm trong giỏ không hợp lệ.|INVALID_QUANTITY");
                        }
                        int buyQuantity = persistedQuantity;
                        BigDecimal trustedFlashSalePrice = null;

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

                                trustedFlashSalePrice = fsi.getFlashSalePrice();
                                if (trustedFlashSalePrice == null
                                                || trustedFlashSalePrice.compareTo(BigDecimal.ZERO) <= 0
                                                || sku.getPrice() == null
                                                || trustedFlashSalePrice.compareTo(sku.getPrice()) >= 0) {
                                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                                        "Giá Flash Sale không hợp lệ. Vui lòng tải lại giỏ hàng.|FLASH_SALE_INVALID_PRICE");
                                }

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

                        if (sku.getStockQuantity() == null || sku.getPrice() == null
                                        || sku.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT,
                                                "Thông tin SKU không hợp lệ. Vui lòng chọn sản phẩm khác.|SKU_INVALID");
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

                        if (trustedFlashSalePrice != null) {
                                // Marcus sửa: Checkout dùng giá từ FlashSaleItem vừa khóa, không
                                // dùng bản giá lưu trong CartItem hay giá frontend.
                                priceAtPurchase = trustedFlashSalePrice;
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

                // Marcus thêm: nhận tại cửa hàng không gọi GHN và không phát sinh phí/trợ giá
                // ship.
                BigDecimal ghnStandardFee = BigDecimal.ZERO;
                BigDecimal discountedShippingFee = BigDecimal.ZERO;
                BigDecimal shopShippingSubsidy = BigDecimal.ZERO;
                if (!isStorePickup) {
                        Integer rawShippingFee = ghnService.calculateShippingFee(
                                        req.getToDistrictId(), req.getToWardCode(), totalWeightGram,
                                        totalAmount.intValue());
                        ghnStandardFee = BigDecimal.valueOf(rawShippingFee);
                        var shippingCalc = shippingService.calculateFinalShipping(totalAmount, ghnStandardFee);
                        discountedShippingFee = shippingCalc.getDiscountedShippingFee();
                        shopShippingSubsidy = ghnStandardFee.subtract(discountedShippingFee);
                }

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

                // Tính phí ship khách thực trả (đã trừ freeshipAmount nếu có)
                BigDecimal customerShippingFee = discountedShippingFee
                                .subtract(freeshipAmount)
                                .max(BigDecimal.ZERO);
                order.setCustomerShippingFee(customerShippingFee);

                // Tính Final Amount bằng toán học chặt chẽ
                BigDecimal finalAmount = totalAmount
                                .add(customerShippingFee)
                                .subtract(discountAmount);

                order.setFinalAmount(finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount);

                Order savedOrder = orderRepository.save(order);
                // Marcus thêm: chỉ lưu mốc funnel và order_id, không lưu thông tin khách.
                try {
                        behaviorEventService.recordOrderCreated(savedOrder.getOrderId(), req.getBehaviorSessionId());
                } catch (RuntimeException ignored) {
                }
                // Marcus thêm: khách nhận xác nhận ngay khi backend tạo đơn thành
                // công; VNPAY vẫn có notification thanh toán riêng sau IPN.
                userNotificationService.createOrderStatusNotification(
                                savedOrder,
                                "PENDING",
                                "Đơn " + savedOrder.getOrderCode()
                                                + " đã được tạo thành công và đang chờ Marcus Store xác nhận.");

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

                // Marcus sửa lỗi chuông: VNPAY chưa thu tiền không được báo là đơn mới cho
                // admin.
                if (!"VNPAY".equalsIgnoreCase(savedOrder.getPaymentMethod())) {
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
                }

                return savedOrder;
        }

        private Order findExistingCheckout(String checkoutRequestId, Integer userId) {
                return orderRepository
                                .findByCheckoutRequestIdAndUserUserId(checkoutRequestId, userId)
                                .orElse(null);
        }

        // Marcus thêm: tách validation để thống nhất SKU/Product cha và kiểm thử
        // được độc lập, không chạm nghiệp vụ Product của thành viên.
        void validateSkuSellable(ProductSku sku, String requestedSkuCode) {
                if (sku == null || !Boolean.TRUE.equals(sku.getIsActive())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Sản phẩm " + requestedSkuCode
                                                        + " không còn khả dụng.|SKU_INACTIVE");
                }
                if (sku.getProduct() == null || !Boolean.TRUE.equals(sku.getProduct().getStatus())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Sản phẩm " + sku.getSkuCode()
                                                        + " đã ngừng kinh doanh. Vui lòng xóa khỏi giỏ hàng.|PRODUCT_INACTIVE");
                }
        }

        // Marcus thêm: tách giới hạn COD để kiểm thử quy tắc giữ kho độc lập.
        void validateCodPendingLimit(String paymentMethod, Integer userId) {
                if (!"COD".equalsIgnoreCase(paymentMethod)) {
                        return;
                }
                long pendingCod = orderRepository.countPendingCodOrders(userId);
                if (pendingCod >= maxPendingCodOrders) {
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                        "Bạn đang có quá nhiều đơn COD chờ xác nhận. "
                                                        + "Vui lòng chờ Admin xử lý hoặc hủy đơn cũ trước khi đặt tiếp.|COD_PENDING_LIMIT");
                }
        }
}
