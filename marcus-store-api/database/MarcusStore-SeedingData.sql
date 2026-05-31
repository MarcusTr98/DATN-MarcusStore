USE MarcusStoreDB;
GO

-- Tắt cảnh báo số dòng ảnh hưởng để script chạy nhanh hơn
SET NOCOUNT ON;

-- ==============================================================================
-- 1. TÀI KHOẢN VÀ PHÂN QUYỀN (RBAC)
-- ==============================================================================
PRINT N'Seeding Roles & Permissions...';

SET IDENTITY_INSERT Roles ON;
INSERT INTO Roles (role_id, role_name, description) VALUES
(1, 'ADMIN', N'Quản trị viên cấp cao nhất'),
(2, 'STAFF', N'Nhân viên'),
(3, 'CUSTOMER', N'Khách hàng mua sắm');
SET IDENTITY_INSERT Roles OFF;

SET IDENTITY_INSERT Permissions ON;
INSERT INTO Permissions (permission_id, permission_name, description, module_name) VALUES
-- Module Nhóm Sản Phẩm
(1, 'PRODUCT_VIEW', N'Xem danh sách sản phẩm', 'PRODUCT'),
(2, 'PRODUCT_CREATE', N'Thêm mới sản phẩm', 'PRODUCT'),
(3, 'PRODUCT_UPDATE', N'Cập nhật sản phẩm', 'PRODUCT'),
(4, 'PRODUCT_DELETE', N'Xóa sản phẩm', 'PRODUCT'),
-- Module Kho hàng (IMEI)
(5, 'INVENTORY_VIEW', N'Xem tồn kho và IMEI', 'INVENTORY'),
(6, 'INVENTORY_IMPORT', N'Nhập kho (Thêm IMEI)', 'INVENTORY'),
-- Module Đơn hàng
(7, 'ORDER_VIEW', N'Xem danh sách đơn hàng', 'ORDER'),
(8, 'ORDER_PROCESS', N'Xử lý trạng thái đơn hàng', 'ORDER'),
-- Module Người dùng
(9, 'USER_VIEW', N'Xem danh sách người dùng', 'USER'),
(10, 'USER_MANAGE', N'Quản lý phân quyền và khóa tài khoản', 'USER'),
-- Module Khuyến mãi
(11, 'MARKETING_MANAGE', N'Quản lý Voucher & Flash Sale', 'MARKETING'),
-- Module Cấu hình
(12, 'SETTING_MANAGE', N'Quản lý cấu hình hệ thống và Banner', 'SYSTEM');
SET IDENTITY_INSERT Permissions OFF;

-- Cấp toàn quyền cho ADMIN (Role 1), Quyền cơ bản cho STAFF (Role 2)
INSERT INTO Role_Permissions (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12),
(2, 1), (2, 5), (2, 7), (2, 8); -- Nhân viên chỉ được xem và xử lý đơn hàng

SET IDENTITY_INSERT Users ON;
-- Lưu ý: Mật khẩu mặc định là '123456' đã được mã hóa BCrypt ($2a$10$...)
INSERT INTO Users (user_id, role_id, username, password_hash, email, phone_number, full_name, is_active) VALUES
(1, 1, 'admin', '$2a$10$X/X5D.A.eX8l.Fh7jA9O/O2t0bO.D5q9.a/gM2.288zN7XzM1vWq2', 'admin@marcusstore.com', '0901111111', N'Marcus (Admin Cấp Cao)', 1),
(2, 2, 'staff1', '$2a$10$X/X5D.A.eX8l.Fh7jA9O/O2t0bO.D5q9.a/gM2.288zN7XzM1vWq2', 'staff1@marcusstore.com', '0902222222', N'Trần Nhân Viên', 1),
(3, 3, 'khach1', '$2a$10$X/X5D.A.eX8l.Fh7jA9O/O2t0bO.D5q9.a/gM2.288zN7XzM1vWq2', 'khach1@gmail.com', '0983333333', N'Nguyễn Khách Hàng', 1);
SET IDENTITY_INSERT Users OFF;

-- ==============================================================================
-- 2. ĐỊA CHỈ HÀNH CHÍNH (5 THÀNH PHỐ LỚN THEO CHUẨN MÃ BƯU CHÍNH)
-- ==============================================================================
PRINT N'Seeding Locations...';

INSERT INTO Provinces (province_code, province_name) VALUES
('01', N'Thành phố Hà Nội'),
('31', N'Thành phố Hải Phòng'),
('48', N'Thành phố Đà Nẵng'),
('79', N'Thành phố Hồ Chí Minh'),
('92', N'Thành phố Cần Thơ');

INSERT INTO Districts (district_code, district_name, province_code) VALUES
('001', N'Quận Ba Đình', '01'),
('005', N'Quận Cầu Giấy', '01'),
('303', N'Quận Hồng Bàng', '31'), -- Phường Hồng Bàng, Hải Phòng của Marcus
('305', N'Quận Ngô Quyền', '31'),
('490', N'Quận Hải Châu', '48'),
('760', N'Quận 1', '79'),
('761', N'Quận 12', '79'),
('916', N'Quận Ninh Kiều', '92');

INSERT INTO Wards (ward_code, ward_name, district_code) VALUES
('00001', N'Phường Phúc Xá', '001'),
('00166', N'Phường Mai Dịch', '005'),
('10200', N'Phường Hoàng Văn Thụ', '303'), -- Thuộc Hồng Bàng, Hải Phòng
('10203', N'Phường Sở Dầu', '303'),
('20200', N'Phường Bến Nghé', '760'),
('20203', N'Phường Tân Định', '760'),
('30300', N'Phường Hải Châu I', '490'),
('40400', N'Phường Xuân Khánh', '916');

SET IDENTITY_INSERT User_Addresses ON;
INSERT INTO User_Addresses (address_id, user_id, recipient_name, phone_number, province_code, district_code, ward_code, detail_address, note, is_default) VALUES
(1, 3, N'Nguyễn Khách Hàng', '0983333333', '31', '303', '10200', N'Số 12, Đường Điện Biên Phủ', N'Giao giờ hành chính', 1),
(2, 3, N'Người Nhận Hộ', '0919999999', '79', '760', '20200', N'Tòa nhà Bitexco, Số 2 Hải Triều', NULL, 0);
SET IDENTITY_INSERT User_Addresses OFF;

-- ==============================================================================
-- 3. DANH MỤC VÀ SẢN PHẨM LÕI
-- ==============================================================================
PRINT N'Seeding Categories & Products...';

SET IDENTITY_INSERT Categories ON;
INSERT INTO Categories (category_id, category_name, parent_id, status) VALUES
(1, N'Điện thoại', NULL, 1),
(2, N'Apple (iPhone)', 1, 1),
(3, N'Samsung', 1, 1),
(4, N'Phụ kiện', NULL, 1);
SET IDENTITY_INSERT Categories OFF;

SET IDENTITY_INSERT Products ON;
INSERT INTO Products (product_id, product_name, description, brand, thumbnail_url, category_id) VALUES
(1, N'iPhone 15 Pro Max', N'<p>iPhone 15 Pro Max với thiết kế Titan chuẩn hàng không vũ trụ...</p>', 'Apple', '/images/iphone-15-pro-max-thumb.jpg', 2),
(2, N'Samsung Galaxy S24 Ultra', N'<p>Galaxy AI is here. Kỷ nguyên AI trên điện thoại di động...</p>', 'Samsung', '/images/s24-ultra-thumb.jpg', 3);
SET IDENTITY_INSERT Products OFF;

SET IDENTITY_INSERT Product_Images ON;
INSERT INTO Product_Images (image_id, product_id, image_url, is_primary, display_order) VALUES
(1, 1, '/images/ip15pm-main.jpg', 1, 1),
(2, 1, '/images/ip15pm-side.jpg', 0, 2),
(3, 2, '/images/s24u-main.jpg', 1, 1);
SET IDENTITY_INSERT Product_Images OFF;

-- ==============================================================================
-- 4. THUỘC TÍNH (ATTRIBUTES) VÀ BIẾN THỂ (SKU)
-- ==============================================================================
SET IDENTITY_INSERT Attributes ON;
INSERT INTO Attributes (attribute_id, attribute_name) VALUES
(1, N'Màu sắc'),
(2, N'Dung lượng bộ nhớ');
SET IDENTITY_INSERT Attributes OFF;

SET IDENTITY_INSERT Attribute_Values ON;
INSERT INTO Attribute_Values (value_id, attribute_id, value_string) VALUES
(1, 1, N'Titan Tự Nhiên'),
(2, 1, N'Titan Đen'),
(3, 2, N'256GB'),
(4, 2, N'512GB'),
(5, 1, N'Xám Titan'),
(6, 1, N'Đen Titan');
SET IDENTITY_INSERT Attribute_Values OFF;

SET IDENTITY_INSERT Product_Skus ON;
INSERT INTO Product_Skus (sku_id, product_id, sku_code, sku_image_url, price, stock_quantity) VALUES
(1, 1, 'IP15PM-NAT-256', '/images/ip15pm-nat.jpg', 29990000.00, 15),
(2, 1, 'IP15PM-BLK-256', '/images/ip15pm-blk.jpg', 29490000.00, 10),
(3, 2, 'S24U-GRY-256', '/images/s24u-gry.jpg', 27990000.00, 5);
SET IDENTITY_INSERT Product_Skus OFF;

INSERT INTO Sku_Attribute_Values (sku_id, value_id) VALUES
(1, 1), (1, 3), -- SKU 1: Titan Tự Nhiên + 256GB
(2, 2), (2, 3), -- SKU 2: Titan Đen + 256GB
(3, 5), (3, 3); -- SKU 3: Xám Titan + 256GB

-- ==============================================================================
-- 5. KHO HÀNG (QUẢN LÝ IMEI CHO TỪNG CHIẾC MÁY)
-- ==============================================================================
PRINT N'Seeding Inventory (IMEIs)...';

SET IDENTITY_INSERT Product_Items ON;
INSERT INTO Product_Items (item_id, sku_id, imei_code, status, order_item_id) VALUES
-- 3 chiếc iPhone 15 PM Titan Tự nhiên đang rảnh trong kho (status = 1)
(1, 1, '359260114000001', 1, NULL),
(2, 1, '359260114000002', 1, NULL),
(3, 1, '359260114000003', 1, NULL),
-- 1 chiếc S24 Ultra (status = 1)
(4, 3, '354123556000001', 1, NULL);
SET IDENTITY_INSERT Product_Items OFF;

-- ==============================================================================
-- 6. KHUYẾN MÃI (VOUCHER & FLASH SALE)
-- ==============================================================================
SET IDENTITY_INSERT Vouchers ON;
INSERT INTO Vouchers (voucher_id, voucher_code, discount_value, discount_type, max_discount_amount, min_order_value, start_date, end_date, quantity) VALUES
(1, 'WELCOME2026', 10.00, 'PERCENT', 500000.00, 1000000.00, '2026-01-01', '2026-12-31', 100),
(2, 'GIAM500K', 500000.00, 'AMOUNT', NULL, 15000000.00, '2026-05-01', '2026-06-30', 50);
SET IDENTITY_INSERT Vouchers OFF;

SET IDENTITY_INSERT Flash_Sale_Slots ON;
INSERT INTO Flash_Sale_Slots (slot_id, name, start_date, end_date, status) VALUES
(1, N'Giờ vàng giá sốc', '2026-05-30 08:00:00', '2026-05-30 23:59:59', 1);
SET IDENTITY_INSERT Flash_Sale_Slots OFF;

INSERT INTO Flash_Sale_Items (slot_id, sku_id, original_price, flash_sale_price, flash_sale_quantity, sold_quantity) VALUES
(1, 1, 29990000.00, 28990000.00, 5, 0);

-- ==============================================================================
-- 7. GIỎ HÀNG VÀ YÊU THÍCH
-- ==============================================================================
SET IDENTITY_INSERT Carts ON;
INSERT INTO Carts (cart_id, user_id) VALUES (1, 3);
SET IDENTITY_INSERT Carts OFF;

SET IDENTITY_INSERT Cart_Items ON;
INSERT INTO Cart_Items (cart_item_id, cart_id, sku_id, quantity) VALUES 
(1, 1, 2, 1); -- Khách hàng 3 đang để iPhone 15 Titan Đen trong giỏ
SET IDENTITY_INSERT Cart_Items OFF;

SET IDENTITY_INSERT Wishlists ON;
INSERT INTO Wishlists (wishlist_id, user_id, product_id) VALUES (1, 3, 1);
SET IDENTITY_INSERT Wishlists OFF;

-- ==============================================================================
-- 8. ĐƠN HÀNG (GIẢ LẬP KHÁCH HÀNG ĐÃ ĐẶT HÀNG THÀNH CÔNG 1 ĐƠN)
-- ==============================================================================
PRINT N'Seeding Orders...';

SET IDENTITY_INSERT Orders ON;
INSERT INTO Orders (order_id, user_id, voucher_id, order_code, recipient_name, recipient_phone, shipping_address, total_amount, discount_amount, final_amount, payment_method, payment_status, order_status) VALUES
(1, 3, 2, 'ORD-20260530-001', N'Nguyễn Khách Hàng', '0983333333', N'Số 12, Đường Điện Biên Phủ, Phường Hoàng Văn Thụ, Quận Hồng Bàng, Thành phố Hải Phòng', 27990000.00, 500000.00, 27490000.00, 'VNPay', 'PAID', 'COMPLETED');
SET IDENTITY_INSERT Orders OFF;

SET IDENTITY_INSERT Order_Items ON;
INSERT INTO Order_Items (order_item_id, order_id, sku_id, quantity, price_at_purchase) VALUES
(1, 1, 3, 1, 27990000.00); -- Đã mua 1 chiếc S24 Ultra
SET IDENTITY_INSERT Order_Items OFF;

-- Cập nhật chiếc máy S24 Ultra trong kho thành ĐÃ BÁN (status = 2) và link tới đơn hàng vừa mua
UPDATE Product_Items SET status = 2, order_item_id = 1, updated_at = GETDATE() WHERE item_id = 4;

-- ==============================================================================
-- 9. CMS ĐỘNG, TIN TỨC & LOGS
-- ==============================================================================
INSERT INTO System_Settings (setting_key, setting_value, setting_group, description) VALUES
('STORE_NAME', 'Marcus Store - Đẳng Cấp Công Nghệ', 'GENERAL', N'Tên cửa hàng'),
('HOTLINE', '0901234567', 'CONTACT', N'Số điện thoại hỗ trợ'),
('ADDRESS', N'Đại học FPT Polytechnic, Hải Phòng', 'CONTACT', N'Địa chỉ cửa hàng');

SET IDENTITY_INSERT Post_Categories ON;
INSERT INTO Post_Categories (post_category_id, name, slug) VALUES
(1, N'Tin tức Công Nghệ', 'tin-tuc-cong-nghe'),
(2, N'Thủ thuật - Hướng dẫn', 'thu-thuat');
SET IDENTITY_INSERT Post_Categories OFF;

SET IDENTITY_INSERT Posts ON;
INSERT INTO Posts (post_id, post_category_id, author_id, title, slug, content, is_published) VALUES
(1, 1, 1, N'Apple chính thức ra mắt iPhone 15', 'apple-ra-mat-iphone-15', N'<p>Vào rạng sáng nay, Apple đã chính thức giới thiệu siêu phẩm...</p>', 1);
SET IDENTITY_INSERT Posts OFF;

SET IDENTITY_INSERT Banner_Positions ON;
INSERT INTO Banner_Positions (position_id, position_code, description) VALUES
(1, 'HOME_HERO_SLIDER', N'Slider chính ngoài trang chủ'),
(2, 'CATEGORY_RIGHT', N'Cột bên phải trang danh mục');
SET IDENTITY_INSERT Banner_Positions OFF;

SET IDENTITY_INSERT Banners ON;
INSERT INTO Banners (banner_id, position_id, title, image_url, target_url) VALUES
(1, 1, N'Săn sale Tháng 5', '/images/banner-home-1.jpg', '/flash-sale'),
(2, 1, N'Galaxy S24 Series', '/images/banner-home-2.jpg', '/product/s24-ultra');
SET IDENTITY_INSERT Banners OFF;

SET IDENTITY_INSERT Comments_Evaluations ON;
INSERT INTO Comments_Evaluations (review_id, user_id, product_id, rating, comment_text, is_approved) VALUES
(1, 3, 2, 5, N'Máy dùng cực mượt, AI dịch trực tiếp rất thông minh. Giao hàng tại Hải Phòng siêu nhanh!', 1);
SET IDENTITY_INSERT Comments_Evaluations OFF;

SET IDENTITY_INSERT Audit_Logs ON;
INSERT INTO Audit_Logs (log_id, user_id, action_type, table_name, description, ip_address) VALUES
(1, 1, 'CREATE', 'Products', N'Admin thêm mới sản phẩm iPhone 15 Pro Max', '127.0.0.1');
SET IDENTITY_INSERT Audit_Logs OFF;

PRINT N'Seeding Complete! Database is ready for Marcus Store API.';
GO


USE MarcusStoreDB;
GO
-- 1. Tạm thời VÔ HIỆU HÓA toàn bộ kiểm tra khóa ngoại (Để tránh lỗi Conflict khi xóa)
EXEC sp_MSforeachtable "ALTER TABLE ? NOCHECK CONSTRAINT all";

-- 2. XÓA sạch toàn bộ dữ liệu trong tất cả các bảng
EXEC sp_MSforeachtable "DELETE FROM ?";

-- 3. RESET bộ đếm Identity (ID tự tăng) về 0 cho những bảng có sử dụng Identity
EXEC sp_MSforeachtable "IF OBJECTPROPERTY(OBJECT_ID('?'), 'TableHasIdentity') = 1 DBCC CHECKIDENT ('?', RESEED, 0)";

-- 4. BẬT LẠI toàn bộ kiểm tra khóa ngoại để bảo vệ tính toàn vẹn của cấu trúc
EXEC sp_MSforeachtable "ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all";
GO

PRINT N'Đã dọn sạch toàn bộ dữ liệu và Reset ID về 0 thành công! Sẵn sàng chạy lại file Seeding.';