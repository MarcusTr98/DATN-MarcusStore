# 🛒 MARCUS STORE - DATN2026 - HP03

## 🛠 Tech Stack

**1. Backend (API Server):**

- Java 21 / Spring Boot 3.2.5
- Spring Security & JWT (JSON Web Token)
- Spring Data JPA / Hibernate
- Cơ sở dữ liệu: SQL Server

**2. Frontend (Client & Admin):**

- VueJS 3 (Composition API) / Vite
- Vue Router & Pinia (State Management)
- Bootstrap 5 & FontAwesome

## 🚀 Cấu trúc dự án

Dự án áp dụng mô hình kiến trúc phân lớp chuẩn Enterprise:

- `marcus-store-api/`: Chứa toàn bộ mã nguồn Spring Boot Backend.
- `marcus-store-frontend/`: Chứa mã nguồn VueJS chia làm 2 phân hệ hiển thị rõ ràng (Client Layout & Admin Layout).

## ⚙️ Hướng dẫn cài đặt môi trường

### Chạy Backend

1. Yêu cầu cài đặt **JDK 21** và **SQL Server**.
2. Thực thi script SQL trong thư mục `/database/MarcusStoreDB.sql` để khởi tạo cấu trúc CSDL.
3. Cấu hình lại chuỗi kết nối Database trong file `application.properties`.
4. Mở terminal, chạy lệnh: `mvn spring-boot:run` (Server khởi chạy tại cổng `8080`).

### Chạy Frontend

1. Yêu cầu cài đặt **Node.js** (Phiên bản 18+).
2. Mở terminal tại thư mục frontend, chạy lệnh cài đặt thư viện: `npm install`
3. Khởi động môi trường dev: `npm run dev` (Web khởi chạy tại cổng `5173`).

---

## 🛑 QUY TRÌNH LÀM VIỆC NHÓM TRÊN GIT (BẮT BUỘC ĐỌC KỸ)

**LUẬT:** TUYỆT ĐỐI KHÔNG ai được đẩy code trực tiếp lên nhánh `main` và nhánh `dev`. Mọi thay đổi đều phải thông qua Pull Request (PR) và **chỉ có Leader (Marcus) mới có quyền duyệt code.**

### QUY TẮC ĐẶT TÊN NHÁNH (BRANCH)

Tên nhánh bắt buộc phải có tên người code để dễ truy cứu trách nhiệm.
Cú pháp: `feature/<tên-của-bạn>/<tên-tính-năng-ngắn-gọn>`

- ❌ _Sai:_ `feature/login`, `update-code`, `ngoc-lam`
- ✅ _Đúng:_ `feature/ngoc/login-api`, `feature/duc/product-list-ui`, `feature/dat/flash-sale-db`

### CÁC BƯỚC CODE VÀ ĐẨY CODE LÊN GITHUB (LÀM ĐÚNG THỨ TỰ NÀY)

**Bước 1: Cập nhật code mới nhất từ nhánh chung (`dev`) về máy**
Trước khi code tính năng mới, luôn phải lấy code mới nhất về để tránh lỗi xung đột.

```bash
git checkout dev
git pull origin dev
```

**Bước 2: Tạo nhánh riêng để bắt đầu code**
Từ nhánh `dev`, tạo một nhánh mới theo đúng cú pháp tên ở trên.

```bash
git checkout -b feature/ten-cua-ban/ten-tinh-nang
```

_(Bây giờ bạn tiến hành code bình thường trên IDE như VS Code/IntelliJ...)_

**Bước 3: Đẩy code của bạn lên GitHub sau khi làm xong**
Chạy lần lượt 3 lệnh sau (Chú ý: message commit phải ghi rõ bằng tiếng Việt bạn vừa làm cái gì).

```bash
git add .
git commit -m "Thêm chức năng đăng nhập cho Frontend"
git push origin feature/ten-cua-ban/ten-tinh-nang
```

**Bước 4: Lên GitHub tạo Yêu cầu gộp code (Pull Request)**

1. Lên trang GitHub của dự án.
2. Bạn sẽ thấy một nút xanh lá cây hiện lên ghi là **"Compare & pull request"**. Bấm vào đó.
3. Chỗ **base:** chọn nhánh `dev`. Chỗ **compare:** chọn nhánh `feature/...` của bạn.
4. Bấm **Create pull request** và chờ Marcus vào review code. Nếu code OK, Marcus sẽ bấm nút Merge. Code lỗi, Marcus sẽ reject và yêu cầu sửa lại.
