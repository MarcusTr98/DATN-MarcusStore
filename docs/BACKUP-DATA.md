# Cấu hình Trung tâm sao lưu dữ liệu

Module chỉ cho tài khoản có `ROLE_ADMIN`. `ROLE_STAFF` không thấy route và bị
backend trả `403` nếu gọi API trực tiếp.

## 1. Cấu hình thư mục

Tạo một thư mục tuyệt đối, ví dụ:

```text
D:\MarcusStoreBackups
```

Thêm vào `application.properties`:

```properties
backup.storage-path=D:/MarcusStoreBackups
backup.sqlserver-directory=D:/MarcusStoreBackups
```

Hai cấu hình phải trỏ cùng một thư mục. Backend cần quyền đọc/ghi thư mục. Tài
khoản Windows chạy dịch vụ SQL Server cũng cần quyền ghi vì file `.bak` do SQL
Server tạo, không phải Spring Boot.

Với SQL Server cài mặc định trên Windows, service account thường có dạng:

```text
NT SERVICE\MSSQLSERVER
```

SQL Server Express thường là:

```text
NT SERVICE\MSSQL$SQLEXPRESS
```

Chỉ cấp quyền thư mục backup, không cấp quyền cho toàn bộ ổ đĩa.

## 2. Quyền SQL Server

Tài khoản kết nối trong `spring.datasource.username` cần quyền tạo backup.
Trong môi trường dev dùng `sa` thì đã có quyền. Nếu dùng tài khoản giới hạn,
DBA cấp membership `db_backupoperator` trên đúng database.

Module chạy:

```sql
BACKUP DATABASE ... WITH COPY_ONLY, CHECKSUM, INIT;
RESTORE VERIFYONLY ... WITH CHECKSUM;
```

Nếu edition hỗ trợ, hệ thống ưu tiên thêm `COMPRESSION`; nếu không sẽ tự thử lại
không nén.

## 3. Phân biệt Excel và BAK

- Excel: mỗi bảng là một sheet; tự che cột có tên chứa password, OTP, token,
  secret, credential hoặc hash. Dùng để đọc, tra cứu và in.
- BAK: chứa nguyên database, bao gồm dữ liệu xác thực đã băm. Dùng để phục hồi
  bằng SQL Server Management Studio và phải được cất giữ an toàn.

Không có chức năng restore trên giao diện. File backup và metadata được lưu
ngoài Git; admin chủ động tải về ổ cứng và xóa bản trên máy chủ khi không cần.

## 4. Xử lý sự cố

- `Access is denied`: kiểm tra quyền ghi thư mục của SQL Server service account.
- `BACKUP DATABASE permission denied`: cấp quyền `db_backupoperator`.
- `File không vượt qua kiểm tra toàn vẹn`: không sử dụng file; tạo bản mới.
- Job chuyển thành thất bại sau khi restart: tác vụ trước đó bị gián đoạn, tạo
  lại backup mới.
