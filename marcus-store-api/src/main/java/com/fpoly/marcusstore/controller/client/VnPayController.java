package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.config.VnPayConfig;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.service.OrderCancellationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VnPayController {

    private final VnPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderCancellationService orderCancellationService;

    @Transactional
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> receiveIPN(HttpServletRequest request) {

        // ── Bước 1: Thu thập tất cả tham số VNPAY gửi về
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String name = params.nextElement();
            String value = request.getParameter(name);
            if (value != null && !value.isEmpty()) {
                fields.put(name, value);
            }
        }

        // Tách chữ ký ra khỏi map trước khi băm theo spec VNPAY
        String receivedHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // ── Bước 2: Tái tạo chuỗi hash và xác thực HmacSHA512
        List<String> sortedKeys = new ArrayList<>(fields.keySet());
        Collections.sort(sortedKeys);

        StringBuilder rawData = new StringBuilder();
        for (Iterator<String> it = sortedKeys.iterator(); it.hasNext();) {
            String key = it.next();
            // Dùng URLEncoder.encode và chuẩn US_ASCII
            rawData.append(key)
                    .append('=')
                    .append(java.net.URLEncoder.encode(fields.get(key), StandardCharsets.US_ASCII));
            if (it.hasNext()) {
                rawData.append('&');
            }
        }

        String computedHash = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), rawData.toString());

        if (!computedHash.equalsIgnoreCase(receivedHash)) {
            log.warn("[VNPAY IPN] Chữ ký không hợp lệ. Received={}", receivedHash);
            return ok("97", "Invalid Checksum");
        }

        // ── Bước 3: Lấy các tham số nghiệp vụ
        String orderCode = fields.get("vnp_TxnRef");
        String responseCode = fields.get("vnp_ResponseCode");
        String transactionId = fields.get("vnp_TransactionNo"); // mã GD phía VNPAY

        // Đã bổ sung check null ở đây để chống lỗi NullPointerException sập Server
        String amountStr = fields.get("vnp_Amount");
        if (amountStr == null) {
            log.warn("[VNPAY IPN] VNPAY không gửi số tiền về.");
            return ok("01", "Invalid Amount");
        }
        // VNPAY truyền số tiền đã nhân 100 (VD: 500000 → 5000000)
        long vnpAmount = Long.parseLong(amountStr) / 100;

        // ── Bước 4: Tìm đơn hàng trong DB
        Optional<Order> orderOpt = orderRepository.findByOrderCodeForUpdate(orderCode);
        if (orderOpt.isEmpty()) {
            log.warn("[VNPAY IPN] Không tìm thấy đơn hàng: {}", orderCode);
            return ok("01", "Order not found");
        }

        Order order = orderOpt.get();

        // ── Bước 5: Kiểm tra số tiền khớp
        if (order.getFinalAmount().longValue() != vnpAmount) {
            log.warn("[VNPAY IPN] Số tiền không khớp. DB={}, VNPAY={}",
                    order.getFinalAmount().longValue(), vnpAmount);
            return ok("04", "Invalid Amount");
        }

        // ── Bước 6: Idempotency — chỉ xử lý khi đơn chưa được xác nhận
        String currentPaymentStatus = order.getPaymentStatus();
        if (!"UNPAID".equals(currentPaymentStatus) && !"PENDING".equals(currentPaymentStatus)) {
            log.info("[VNPAY IPN] Đơn {} đã được xác nhận trước đó (status={}). Bỏ qua.",
                    orderCode, currentPaymentStatus);
            return ok("02", "Order already confirmed");
        }

        // ── Bước 7: Cập nhật trạng thái theo kết quả giao dịch
        if ("00".equals(responseCode)) {
            order.setPaymentStatus("PAID");

            // Chỉ set PAID, KHÔNG SET PROCESSING.
            // orderStatus là "PENDING" để Admin vào bấm "Xác nhận đơn"
            order.setOrderStatus("PENDING");

            order.setTransactionId(transactionId);
            order.setPaymentDate(LocalDateTime.now());

            log.info("[VNPAY IPN] Thanh toán thành công. Đơn hàng {} chuyển sang PENDING để Admin xác nhận.",
                    orderCode);
        } else {
            // Thanh toán thất bại: hủy đơn và hoàn tồn kho/voucher đúng một lần.
            order.setPaymentStatus("FAILED");
            orderCancellationService.cancelAndRestore(
                    order,
                    "Giao dịch VNPAY thất bại. ResponseCode: " + responseCode);
            log.info("[VNPAY IPN] Thanh toán thất bại/hủy. Order={}", orderCode);
        }

        orderRepository.save(order);
        // Lưu lịch sử trạng thái để Admin biết đơn đã thanh toán
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(order.getOrderStatus());
        if ("00".equals(responseCode)) {
            history.setTitle("Đã thanh toán VNPAY");
            history.setNote("Giao dịch VNPAY thành công: " + transactionId);
        } else {
            history.setTitle("Thanh toán VNPAY thất bại");
            history.setNote("ResponseCode: " + responseCode + ", TransactionNo: " + transactionId);
        }
        orderStatusHistoryRepository.save(history);

        return ok("00", "Confirm Success");
    }

    // tạo response đúng format VNPAY yêu cầu
    private ResponseEntity<Map<String, String>> ok(String rspCode, String message) {
        return ResponseEntity.ok(Map.of("RspCode", rspCode, "Message", message));
    }
}
