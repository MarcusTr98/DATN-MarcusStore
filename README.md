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

Kiểm tra phiên bản:

```bash
java -version
node --version
npm --version
```

## Khởi tạo cơ sở dữ liệu

1. Tạo hoặc chọn database SQL Server dành cho Marcus Store.
2. Chạy script nền `marcus-store-api/database/MarcusStoreDB2.sql`.
3. Với database đã tồn tại từ phiên bản cũ, chạy các script nâng cấp cần thiết theo thứ tự ngày trong tên file, ví dụ:
   - `20260817_order_assignments.sql`
   - `20260822_ai_analytics_upgrade.sql`
4. Script `MarcusDongBoIdentity1408.sql` chỉ dùng khi cần đồng bộ identity theo dữ liệu của nhóm; không chạy lại tùy tiện trên database đang có dữ liệu thật.

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

Không gửi secret qua Git, commit, README hoặc ảnh chụp màn hình. Thành viên mới nhận bộ cấu hình dùng chung qua kênh riêng của nhóm.

## Chạy backend

Windows:

```bash
cd marcus-store-api
mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
cd marcus-store-api
./mvnw spring-boot:run
```

Backend mặc định chạy tại `http://localhost:8080`.

## Chạy frontend

```bash
cd marcus-store-frontend
npm install
npm run dev
```

Frontend mặc định chạy tại `http://localhost:5173`.

REST API mặc định là `http://localhost:8080/api`. Khi deploy môi trường khác, cấu hình `VITE_API_URL` và `VITE_WS_URL` trong file môi trường local của frontend.

## Kiểm tra trước khi trình diễn hoặc tạo Pull Request

Backend:

```bash
cd marcus-store-api
mvnw.cmd test
```

Frontend:

```bash
cd marcus-store-frontend
npm run lint
npm run build
```

Chỉ chốt phiên bản trình diễn khi backend test, frontend lint và production build đều thành công.

## Tài khoản trình diễn

Không lưu mật khẩu thật trong repository. Trước buổi bảo vệ, nhóm chuẩn bị riêng tối thiểu:

- Một tài khoản `ROLE_ADMIN`.
- Một tài khoản `ROLE_STAFF` có bộ quyền giới hạn.
- Một tài khoản khách hàng đã xác thực email.
- Dữ liệu mẫu gồm sản phẩm còn hàng, voucher, flash sale và đơn hàng ở nhiều trạng thái.

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

## Lưu ý khi mang đi bảo vệ

- Không đổi key, database hoặc URL môi trường ngay trước buổi thi nếu chưa chạy lại toàn bộ luồng demo.
- Chuẩn bị dữ liệu demo và phương án dự phòng khi GHN, VNPAY, Google hoặc Gemini sandbox không phản hồi.
- Chạy trước các luồng đăng nhập, checkout COD, checkout VNPAY, quản trị đơn, voucher, flash sale, bảo hành và phân quyền.
- Sao lưu database và giữ một bản build frontend đã kiểm chứng.
