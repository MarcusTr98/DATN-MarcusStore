# MARCUS — UAT GỘP MUA HÀNG, GIỮ KHO, IMEI, GHN, VNPAY, HỦY VÀ HOÀN

## 1. Gửi thành viên kho: một công thức tồn duy nhất

Marcus lưu ý thành viên kho không đồng bộ `Product_Skus.stock_quantity` bằng tổng
IMEI `IN_STOCK` đơn thuần. Checkout đã giữ hàng ngay khi tạo đơn, trước thời điểm
Admin gán IMEI.

**Công thức bắt buộc:**

`Tồn khả dụng = IMEI đang trong kho - số lượng của đơn đang giữ nhưng chưa gán IMEI`

Các trạng thái đang giữ hàng:

- `PENDING`
- `CONFIRMED`
- `PROCESSING`
- `READY_FOR_PICKUP`
- `PACKED`
- `SHIPPING`

Khi gán IMEI, một IMEI rời `IN_STOCK` nhưng đồng thời một đơn vị “chưa gán” cũng
giảm. Vì vậy tồn khả dụng không được giảm lần hai.

Truy vấn kiểm tra SKU lệch:

```sql
;WITH PhysicalStock AS (
    SELECT sku_id, COUNT(*) AS physical_qty
    FROM Product_Items
    WHERE status = 1 -- IN_STOCK
    GROUP BY sku_id
), ReservedWithoutImei AS (
    SELECT oi.sku_id,
           SUM(oi.quantity - ISNULL(assigned.assigned_qty, 0)) AS reserved_qty
    FROM Order_Items oi
    JOIN Orders o ON o.order_id = oi.order_id
    OUTER APPLY (
        SELECT COUNT(*) AS assigned_qty
        FROM Product_Items pi
        WHERE pi.order_item_id = oi.order_item_id
    ) assigned
    WHERE o.order_status IN (
        'PENDING','CONFIRMED','PROCESSING',
        'READY_FOR_PICKUP','PACKED','SHIPPING'
    )
    GROUP BY oi.sku_id
)
SELECT sku.sku_code,
       sku.stock_quantity AS stored_available,
       ISNULL(ps.physical_qty, 0) AS imei_in_stock,
       ISNULL(r.reserved_qty, 0) AS reserved_without_imei,
       CASE
           WHEN ISNULL(ps.physical_qty, 0) - ISNULL(r.reserved_qty, 0) > 0
           THEN ISNULL(ps.physical_qty, 0) - ISNULL(r.reserved_qty, 0)
           ELSE 0
       END AS expected_available
FROM Product_Skus sku
JOIN Products p ON p.product_id = sku.product_id AND p.status_imei = 1
LEFT JOIN PhysicalStock ps ON ps.sku_id = sku.sku_id
LEFT JOIN ReservedWithoutImei r ON r.sku_id = sku.sku_id
WHERE sku.stock_quantity <>
      CASE
          WHEN ISNULL(ps.physical_qty, 0) - ISNULL(r.reserved_qty, 0) > 0
          THEN ISNULL(ps.physical_qty, 0) - ISNULL(r.reserved_qty, 0)
          ELSE 0
      END;
```

Kết quả đúng là **0 dòng**. Thành viên kho chỉ sửa công thức đồng bộ của module
kho; không cộng/trừ tồn thêm trong luồng Checkout của Marcus.

## 2. Chuẩn bị UAT

1. Backup database `.bak`.
2. Chọn một SKU điện thoại quản lý IMEI, đang hoạt động và có ít nhất 3 IMEI
   `IN_STOCK`.
3. Ghi lại `stock_quantity`, số IMEI trong kho, voucher và Flash Sale trước test.
4. Đăng nhập hai cửa sổ: một tài khoản khách và một tài khoản Admin.
5. Để test COD nhanh, chỉ trên máy demo có thể đặt:

```properties
checkout.cod.pending-timeout-hours=1
checkout.cod.expiry-scan-delay-ms=60000
```

Không đặt timeout bằng `0` trên database đang có đơn thật.

## 3. UAT-01 — COD: mua, giữ kho, gán IMEI và đẩy GHN

### Thao tác giao diện

1. Khách thêm SKU đã chọn vào giỏ, vào Checkout, chọn **Giao tận nơi + COD**.
2. Nhấn đặt hàng đúng một lần và ghi lại mã đơn.
3. Admin mở chi tiết đơn và xác nhận đơn.
4. Admin chuyển sang chuẩn bị hàng, chọn đúng số IMEI của SKU.
5. Admin đóng gói và tạo vận đơn GHN.

### Kết quả phải thấy

- Tạo đúng một đơn, một nhóm `Order_Items`.
- Ngay sau Checkout, tồn khả dụng giảm đúng số lượng dù IMEI chưa gán.
- Sau khi gán IMEI, tồn khả dụng không giảm lần thứ hai.
- IMEI chuyển khỏi `IN_STOCK`, liên kết đúng `order_item_id`.
- Mã vận đơn và trạng thái tích hợp GHN hiển thị trong chi tiết đơn.
- Chuông khách/Admin đi đúng đơn.

### SQL đối chiếu

```sql
DECLARE @order_code VARCHAR(50) = 'THAY_MA_DON';

SELECT o.order_code, o.order_status, o.payment_method, o.payment_status,
       sd.fulfillment_method, sd.tracking_code, sd.ghn_integration_status
FROM Orders o
LEFT JOIN Order_Shipping_Details sd ON sd.order_id = o.order_id
WHERE o.order_code = @order_code;

SELECT oi.order_item_id, sku.sku_code, oi.quantity,
       COUNT(pi.item_id) AS assigned_imeis
FROM Orders o
JOIN Order_Items oi ON oi.order_id = o.order_id
JOIN Product_Skus sku ON sku.sku_id = oi.sku_id
LEFT JOIN Product_Items pi ON pi.order_item_id = oi.order_item_id
WHERE o.order_code = @order_code
GROUP BY oi.order_item_id, sku.sku_code, oi.quantity;
```

`assigned_imeis` phải bằng `quantity` với sản phẩm quản lý IMEI.

## 4. UAT-02 — Chống spam COD và tự giải phóng đơn treo

### Chống spam bằng giao diện

1. Cùng một khách tạo 3 đơn COD vẫn còn `PENDING`.
2. Thêm lại hàng và tạo đơn COD thứ 4.
3. Hệ thống phải từ chối với thông báo khách đang có quá nhiều đơn chờ xác nhận.
4. Hủy một đơn cũ rồi thử lại; đơn mới được phép tạo nếu còn hàng.

### Tự hủy COD chờ quá lâu

1. Tạo một đơn COD mới và không cho Admin xác nhận.
2. Trên database test, chỉ chỉnh thời gian của đúng đơn này:

```sql
UPDATE Orders
SET created_at = DATEADD(HOUR, -2, SYSDATETIME())
WHERE order_code = 'THAY_MA_DON_COD_TEST'
  AND payment_method = 'COD'
  AND order_status = 'PENDING';
```

3. Chờ scheduler chạy.
4. Tải lại chi tiết đơn khách và danh sách Admin.

Kết quả: đơn thành `CANCELLED`, lý do
`SYSTEM_COD_CONFIRMATION_EXPIRED`; kho, voucher và Flash Sale được hoàn đúng một
lần; khách nhận thông báo chuông. Nếu Admin xác nhận trước scheduler thì đơn
không bị hủy.

## 5. UAT-03 — VNPAY thành công, GHN và webhook

1. Khách Checkout giao tận nơi bằng VNPAY.
2. Thanh toán thành công trên sandbox và quay về website.
3. Kiểm tra đơn là `PAID`, giao dịch `VNPAY_PAYMENT/SUCCESS` và chưa bị scheduler
   hủy.
4. Admin xác nhận, gán IMEI, đóng gói và tạo vận đơn GHN như UAT-01.
5. Nếu dashboard dev không đẩy callback, dùng Postman gửi payload webhook GHN vào
   URL ngrok của `/api/ghn/webhook`, kèm đúng `X-Verification-Token`.
6. Kiểm tra trạng thái đơn đổi theo webhook và chuông trỏ đúng đơn.

Ngrok chỉ cần khi giả lập **bên thứ ba gọi vào máy local**. Không cần ngrok khi
test thao tác giao diện, gọi API nội bộ hoặc đọc database.

## 6. UAT-04 — Hủy và hoàn tài nguyên/refund

### COD chưa xuất hàng

1. Khách/Admin hủy đơn COD còn được phép hủy.
2. Xác nhận tồn SKU, Flash Sale và voucher về đúng số trước Checkout.
3. Gọi lại thao tác/API hủy: hệ thống không hoàn lần hai.

### VNPAY đã thanh toán

1. Hủy đơn VNPAY `PAID` ở trạng thái còn cho phép.
2. Kiểm tra chỉ có một `Refund_Requests` và một giao dịch `REFUND` dự kiến.
3. Dùng Postman đăng nhập Admin, lấy Bearer token và gọi endpoint xác nhận sandbox
   theo kịch bản refund hiện có.
4. Kiểm tra refund thành `SUCCESS`, payment status thành `REFUNDED`, giao dịch âm
   xuất hiện đúng một lần trong đối soát.

## 7. Điều kiện UAT đạt

- Không SKU nào âm tồn hoặc lệch công thức mục 1.
- Không IMEI `SOLD` mà thiếu `order_item_id`.
- Không dòng đơn điện thoại đã xuất mà thiếu IMEI.
- Một Checkout request chỉ tạo một đơn.
- Một lần hủy chỉ hoàn kho/voucher/Flash Sale một lần.
- VNPAY `PAID` có đúng giao dịch thu thành công; refund không vượt tiền đã thu.
- Giao dịch chưa `SUCCESS` không được tích đối soát.
- Deep link chuông mở đúng đơn/refund cần xử lý.
