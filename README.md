# Marcus Store — DATN 2026 HP03

Marcus Store là hệ thống thương mại điện tử bán thiết bị công nghệ, gồm website khách hàng và trang quản trị. Dự án được xây dựng theo mô hình frontend–backend tách biệt, phục vụ đồ án tốt nghiệp năm 2026.

## Công nghệ sử dụng

### Backend

- Java 21
- Spring Boot 3.2.5
- Spring Security, JWT và OAuth2 Google
- Spring Data JPA / Hibernate
- SQL Server
- Maven Wrapper

### Frontend

- Node.js `^20.19.0` hoặc `>=22.12.0`
- Vue 3, Composition API và Vite
- Vue Router và Pinia
- Bootstrap 5, Ant Design Vue
- Chart.js và ApexCharts

## Chức năng chính

- Đăng ký, đăng nhập, OTP, quên mật khẩu và đăng nhập Google.
- Danh mục, sản phẩm, SKU, thuộc tính, thông số và quản lý IMEI.
- Giỏ hàng, yêu thích, so sánh sản phẩm và checkout.
- Thanh toán COD/VNPAY, theo dõi đơn, hủy đơn và hoàn tiền.
- Voucher, flash sale, tồn kho và bảo hành.
- CMS bài viết, banner, đánh giá và yêu cầu liên hệ.
- Quản trị nhân viên, vai trò, quyền hạn và nhật ký hoạt động.
- Dashboard, báo cáo tài chính, phân tích hành vi và trợ lý AI.
- Thông báo, chat realtime và sao lưu dữ liệu.

## Cấu trúc repository

```text
marcus-store/
├── marcus-store-api/          Spring Boot REST API
│   ├── database/              Script khởi tạo và nâng cấp SQL Server
│   └── src/                   Source code và backend tests
├── marcus-store-frontend/     Vue/Vite client và admin
└── README.md
```

## Chuẩn bị môi trường

Cần cài đặt:

- JDK 21
- Node.js đúng phiên bản ghi trong `marcus-store-frontend/package.json`
- SQL Server
- Git

## Khởi tạo cơ sở dữ liệu

1. Tạo hoặc chọn database SQL Server dành cho Marcus Store.
2. Chạy script nền `marcus-store-api/database/MarcusStoreDB2.sql`.
   Nên sao lưu database trước khi chạy script nâng cấp.

## Cấu hình backend

File cấu hình thật là:

```text
marcus-store-api/src/main/resources/application.properties
```

File này đã được Git ignore để không đưa mật khẩu và API key lên repository. Máy thành viên đang chạy dự án có thể tiếp tục dùng file hiện tại, không cần nhập lại key.

Khi setup máy mới, sao chép:

```text
application.properties.example → application.properties
```

Sau đó điền giá trị thật qua biến môi trường hoặc trực tiếp trong file local. Các secret cần thiết gồm:

- `DB_PASSWORD`
- `JWT_SECRET`
- `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- `GHN_API_TOKEN`, `GHN_SHOP_ID`, `GHN_WEBHOOK_TOKEN`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `GEMINI_API_KEY`

## Quy trình Git của nhóm

Không push trực tiếp lên `main` hoặc `dev`. Mỗi thay đổi tạo nhánh riêng từ `dev`:

```bash
git checkout dev
git pull origin dev
git checkout -b feature/ten-thanh-vien/ten-tinh-nang
```

Sau khi hoàn thành và kiểm tra:

```bash
git add .
git commit -m "Mô tả rõ thay đổi"
git push origin feature/ten-thanh-vien/ten-tinh-nang
```

Tạo Pull Request vào `dev` và chờ leader review trước khi merge.
