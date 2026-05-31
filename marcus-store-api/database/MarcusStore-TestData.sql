USE MarcusStoreDB;
GO

SELECT*FROM users;

-- Xem tất cả roles và số quyền được cấp
SELECT r.role_name, COUNT(rp.permission_id) AS so_quyen
FROM Roles r
LEFT JOIN Role_Permissions rp ON r.role_id = rp.role_id
GROUP BY r.role_name;

-- Xem chi tiết quyền của từng role
SELECT r.role_name, p.module_name, p.permission_name, p.description
FROM Role_Permissions rp
JOIN Roles r ON r.role_id = rp.role_id
JOIN Permissions p ON p.permission_id = rp.permission_id
ORDER BY r.role_name, p.module_name;

-- Xem danh sách users kèm tên role
SELECT u.user_id, u.username, u.full_name, u.email, r.role_name, u.is_active
FROM Users u
JOIN Roles r ON r.role_id = u.role_id;

-- Xem chuỗi địa chỉ đầy đủ của user (Tỉnh > Quận > Phường)
SELECT 
    ua.recipient_name,
    ua.detail_address,
    w.ward_name, d.district_name, p.province_name,
    ua.is_default
FROM User_Addresses ua
JOIN Provinces p ON p.province_code = ua.province_code
JOIN Districts d ON d.district_code = ua.district_code
JOIN Wards w ON w.ward_code = ua.ward_code;

-- Xem sản phẩm kèm danh mục cha - con
SELECT 
    p.product_name, p.brand,
    child.category_name AS danh_muc_con,
    parent.category_name AS danh_muc_cha
FROM Products p
JOIN Categories child ON child.category_id = p.category_id
LEFT JOIN Categories parent ON parent.category_id = child.parent_id;

-- Xem đầy đủ thông tin SKU kèm thuộc tính (màu sắc, dung lượng)
SELECT 
    p.product_name,
    ps.sku_code,
    ps.price,
    ps.stock_quantity,
    STRING_AGG(av.value_string, ' | ') AS thuoc_tinh
FROM Product_Skus ps
JOIN Products p ON p.product_id = ps.product_id
JOIN Sku_Attribute_Values sav ON sav.sku_id = ps.sku_id
JOIN Attribute_Values av ON av.value_id = sav.value_id
GROUP BY p.product_name, ps.sku_code, ps.price, ps.stock_quantity
ORDER BY p.product_name;

-- Xem tất cả IMEI kèm trạng thái
SELECT 
    pi.imei_code,
    p.product_name,
    ps.sku_code,
    CASE pi.status 
        WHEN 1 THEN N'Trong kho'
        WHEN 2 THEN N'Đã bán'
        WHEN 3 THEN N'Đang bảo hành'
        WHEN 4 THEN N'Lỗi/Trả hàng'
    END AS trang_thai,
    pi.order_item_id
FROM Product_Items pi
JOIN Product_Skus ps ON ps.sku_id = pi.sku_id
JOIN Products p ON p.product_id = ps.product_id;

-- Xem giỏ hàng của khách
SELECT 
    u.full_name AS khach_hang,
    p.product_name,
    ps.sku_code,
    STRING_AGG(av.value_string, ' | ') AS bien_the,
    ci.quantity,
    ci.quantity * ps.price AS thanh_tien
FROM Cart_Items ci
JOIN Carts c ON c.cart_id = ci.cart_id
JOIN Users u ON u.user_id = c.user_id
JOIN Product_Skus ps ON ps.sku_id = ci.sku_id
JOIN Products p ON p.product_id = ps.product_id
JOIN Sku_Attribute_Values sav ON sav.sku_id = ps.sku_id
JOIN Attribute_Values av ON av.value_id = sav.value_id
GROUP BY u.full_name, p.product_name, ps.sku_code, ci.quantity, ps.price;

-- Xem danh sách yêu thích
SELECT u.full_name, p.product_name, p.brand
FROM Wishlists wl
JOIN Users u ON u.user_id = wl.user_id
JOIN Products p ON p.product_id = wl.product_id;

-- Xem tổng quan đơn hàng
SELECT 
    o.order_code, u.full_name AS khach_hang,
    o.total_amount, o.discount_amount, o.final_amount,
    v.voucher_code, o.payment_method,
    o.payment_status, o.order_status,
    o.created_at
FROM Orders o
JOIN Users u ON u.user_id = o.user_id
LEFT JOIN Vouchers v ON v.voucher_id = o.voucher_id;

-- Xem chi tiết từng dòng trong đơn hàng + IMEI máy đã xuất
SELECT 
    o.order_code,
    p.product_name,
    ps.sku_code,
    oi.quantity,
    oi.price_at_purchase,
    pi.imei_code AS imei_da_ban
FROM Order_Items oi
JOIN Orders o ON o.order_id = oi.order_id
JOIN Product_Skus ps ON ps.sku_id = oi.sku_id
JOIN Products p ON p.product_id = ps.product_id
LEFT JOIN Product_Items pi ON pi.order_item_id = oi.order_item_id;


-- Xem voucher còn hiệu lực hôm nay
SELECT voucher_code, discount_type, discount_value, 
       max_discount_amount, min_order_value, quantity, end_date
FROM Vouchers
WHERE is_active = 1 AND GETDATE() BETWEEN start_date AND end_date;

-- Xem flash sale đang chạy kèm % giảm giá
SELECT 
    fs.name AS ten_slot,
    p.product_name,
    ps.sku_code,
    fi.original_price,
    fi.flash_sale_price,
    CAST((fi.original_price - fi.flash_sale_price) * 100.0 / fi.original_price AS INT) AS phan_tram_giam,
    fi.flash_sale_quantity - fi.sold_quantity AS con_lai
FROM Flash_Sale_Items fi
JOIN Flash_Sale_Slots fs ON fs.slot_id = fi.slot_id
JOIN Product_Skus ps ON ps.sku_id = fi.sku_id
JOIN Products p ON p.product_id = ps.product_id
WHERE fs.status = 1;

-- Xem đánh giá sản phẩm đã được duyệt
SELECT u.full_name, p.product_name, ce.rating, ce.comment_text, ce.created_at
FROM Comments_Evaluations ce
JOIN Users u ON u.user_id = ce.user_id
JOIN Products p ON p.product_id = ce.product_id
WHERE ce.is_approved = 1;

-- Xem lịch sử thao tác hệ thống
SELECT u.username, al.action_type, al.table_name, al.description, al.ip_address, al.created_at
FROM Audit_Logs al
LEFT JOIN Users u ON u.user_id = al.user_id
ORDER BY al.created_at DESC;