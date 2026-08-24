# Testcase tổng hợp AI và Analytics

## Chuẩn bị

1. Pull nhánh `dev` mới nhất.
2. Với database đã tồn tại, chạy duy nhất `marcus-store-api/database/20260822_ai_analytics_upgrade.sql`.
3. Khởi động backend và frontend; đăng nhập một tài khoản Admin để kiểm tra Analytics.

## Test tự động

Tại thư mục `marcus-store-api`:

```powershell
.\mvnw.cmd "-Dtest=AiAdvisorServiceTest,AdvisorProductScorerTest,ProductComparisonBuilderTest,AiUsageEventServiceTest,BehaviorEventServiceTest,AnalyticsActionServiceTest" test
```

Tại thư mục `marcus-store-frontend`:

```powershell
npm run build
```

Kỳ vọng: backend không có failure/error; frontend build thành công.

## AI tư vấn khách hàng

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| AI-01 | Hỏi “Tư vấn điện thoại tầm 20 triệu” | AI hỏi lại mục đích sử dụng, chưa chọn máy ngẫu nhiên |
| AI-02 | Trả lời “Chơi game, pin tốt” | Giữ ngân sách, thêm ưu tiên hiệu năng và pin; trả tối đa 3 máy còn hàng |
| AI-03 | Hỏi “Máy camera tốt dưới 15 triệu” | Điểm/lý do chỉ khẳng định camera nếu catalog có thông số camera |
| AI-04 | Hỏi giá một model và dung lượng cụ thể | Giá lấy đúng SKU còn hàng; không dùng giá tự sinh từ AI |
| AI-05 | Tắt cấu hình Gemini rồi hỏi tư vấn | Vẫn trả fallback từ catalog, có thẻ sản phẩm |
| AI-06 | Gửi số điện thoại, OTP hoặc yêu cầu xem database | Backend từ chối trước khi truy vấn catalog/Gemini |
| AI-07 | Sau khi click một máy, hỏi “Máy này phù hợp học tập không?” | Dùng đúng focused product, không xếp hạng lại máy khác |

## So sánh sản phẩm

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| CMP-01 | “So sánh [model A] và [model B]” | Hiển thị hai thẻ và ma trận giá, tồn kho, thông số |
| CMP-02 | So sánh ba model | Hiển thị tối đa đúng ba cột sản phẩm |
| CMP-03 | Một máy thiếu thông số pin | Ô tương ứng ghi “Chưa có dữ liệu”, không bịa thông số |
| CMP-04 | Yêu cầu bốn model | Backend chỉ lấy tối đa ba sản phẩm vào comparison contract |
| CMP-05 | Gemini trả JSON lỗi hoặc hết quota | Fallback vẫn giữ thẻ và ma trận so sánh từ backend |

## Attribution AI đến thanh toán

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| ATT-01 | Hỏi AI rồi click thẻ sản phẩm | Có `AI_QUESTION`, response và `AI_PRODUCT_CLICK` cùng session |
| ATT-02 | Thêm sản phẩm vào giỏ thành công | Có `CART_ADDED`; request thêm giỏ thất bại không ghi event |
| ATT-03 | Từ giỏ vào checkout và tạo đơn | Có `CHECKOUT_STARTED` rồi `ORDER_CREATED` đúng thứ tự |
| ATT-04 | Thanh toán VNPAY thành công | Có `PAYMENT_SUCCESS` nối về session của đơn |
| ATT-05 | Mở Admin Analytics | Funnel hiển thị hỏi → tư vấn → click → giỏ → checkout → đơn → thanh toán |
| ATT-06 | Click/checkout không theo đúng thứ tự | Không được tính là phiên chuyển đổi đầy đủ |

## AI Analytics và bằng chứng

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| ANA-01 | Chọn khoảng ngày rồi tạo báo cáo AI | Báo cáo có evidence, interpretation, confidence, action, verification |
| ANA-02 | Mở lại cùng kỳ khi dữ liệu không đổi | Đọc cache `AI_Analytics_Reports`, không gọi Gemini lại |
| ANA-03 | Click “Xem dữ liệu bằng chứng” | Cuộn tới KPI, doanh thu, sản phẩm hoặc chất lượng phù hợp signal |
| ANA-04 | Dữ liệu forecast ít/yếu | Hiển thị confidence thấp hoặc cảnh báo; không khẳng định chắc chắn |
| ANA-05 | Gemini Analytics lỗi | Trả fallback thuật toán hoặc báo cáo cache, trang KPI vẫn hoạt động |

## Workflow hành động

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| ACT-01 | Bấm “Tiếp nhận” một đề xuất | Tạo `Analytics_Actions`, owner là Admin hiện tại, status `ACCEPTED` |
| ACT-02 | Tiếp nhận lại cùng hành động đang mở | Backend từ chối bản ghi trùng |
| ACT-03 | Đổi sang “Đang thực hiện” | Status thành `IN_PROGRESS`, cập nhật `updated_at` |
| ACT-04 | Đổi sang “Hoàn tất” | Status thành `DONE` và vẫn còn trong lịch sử theo dõi |
| ACT-05 | Tải lại trang | Danh sách hành động và trạng thái vẫn được đọc từ database |

## Kiểm tra database sau migration

```sql
SELECT OBJECT_ID('dbo.AI_Product_Clicks', 'U') AS redundant_click_table;
SELECT OBJECT_ID('dbo.Analytics_Actions', 'U') AS analytics_actions_table;
SELECT TOP 20 * FROM dbo.Customer_Behavior_Events ORDER BY created_at DESC;
```

Kỳ vọng: `redundant_click_table` là `NULL`; `analytics_actions_table` khác `NULL`; click AI cũ đã được bảo toàn trong `Customer_Behavior_Events`.
