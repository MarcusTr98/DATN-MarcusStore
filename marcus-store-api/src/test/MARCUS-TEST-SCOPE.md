# Phạm vi testcase tự động của Marcus

> Marcus thêm tài liệu này để xác định đúng trách nhiệm. Bộ test hiện tại chủ yếu
> bảo vệ các module Marcus sở hữu; không đại diện cho test coverage của toàn bộ
> sản phẩm và không nhận thay trách nhiệm test của module thành viên.

## 1. Checkout, đơn hàng và tích hợp bên thứ ba

- `CheckoutRequestValidationTest`: validation dữ liệu Checkout trước khi vào nghiệp vụ.
- `CheckoutServiceIdempotencyTest`: request Checkout gửi lặp chỉ trả về một đơn,
  không chạy lại phần kho/Voucher/Flash Sale của thành viên.
- `OrderServicePaymentGuardTest`: chặn xử lý đơn VNPAY chưa thanh toán và chặn
  hủy nội bộ khi đã có vận đơn GHN.
- `OrderShippingServiceTest`: GHN tạo vận đơn lỗi phải lưu `FAILED`; retry thành
  công phải có tracking và không tạo lặp.
- `GhnPollingServiceTest`: một tracking lỗi không làm dừng polling các tracking khác.
- `GhnWebhookControllerTest`: xác thực token và payload callback GHN.
- `VnPayControllerTest`: callback/IPN VNPAY và các chốt bảo vệ trạng thái thanh toán.
- `VnPayPaymentExpiryServiceTest`: đơn VNPAY bỏ dở được xử lý hết hạn an toàn.
- `OrderCancellationServiceTest`: hủy đơn và hoàn tài nguyên có idempotency.
- `OrderAutoCompletionServiceTest`: tự hoàn thành đơn đủ điều kiện.

## 2. Refund và đối soát tài chính

- `RefundServiceTest`: tạo yêu cầu refund và notification liên quan.
- `VnPayRefundClientTest`: chữ ký/checksum và request refund VNPAY.
- `FinancialServiceTest`: tiền vào, tiền ra, refund và khoảng ngày đối soát.
- `AdminRefundControllerSecurityTest`: quyền truy cập API quản trị refund.

## 3. AI, Analytics và hành vi sử dụng

- `AiAdvisorServiceTest`: AI tư vấn chỉ dùng dữ liệu catalog công khai.
- `AiUsageEventServiceTest`: ghi nhận lượt sử dụng/tư vấn theo tiêu chí hệ thống.
- `AnalyticsServiceTest`: KPI, kỳ so sánh, xu hướng và chỉ số chất lượng bảo hành tổng hợp.
- `AiAnalyticsServiceTest`: cấu hình, cache và payload gửi AI phân tích.
- `AnalyticsRepositoryIntegrationTest`: kiểm tra truy vấn native của Analytics
  khi có database test phù hợp.

## 4. Module nền và module Marcus khác

- `RequestRateLimitFilterTest`: giới hạn spam đăng nhập/API công khai thuộc phạm vi Marcus.
- `ProductConfigServiceTest`: chặn batch sinh trùng tổ hợp biến thể trước khi ghi SKU.
- `AdminNotificationServiceTest`: lưu lịch sử, badge và realtime notification.
- `ChatSessionServiceTest`: vòng đời phiên Live Chat.
- `AdminBackupControllerSecurityTest`: quyền tải bản sao lưu dữ liệu.
- `MarcusModuleValidationTest`: validation các DTO thuộc module Marcus.
- `GlobalExceptionHandlerTest`: chuẩn hóa lỗi nền trả về frontend.
- `MarcusStoreApiApplicationTests`: smoke test khởi động Spring context; phụ thuộc
  cấu hình database/môi trường chạy test.

## Ngoài phạm vi

Bộ test trên không khẳng định đã bao phủ toàn bộ nghiệp vụ Product, Category,
Cart, Voucher, Flash Sale, Kho/IMEI, Bảo hành, Nhân sự/phân quyền hoặc Dashboard
của thành viên. Marcus chỉ kiểm tra điểm giao tiếp của các module đó khi chúng đi
qua Checkout, hủy đơn, GHN/VNPAY, notification, đối soát hoặc Analytics do Marcus
phụ trách.
