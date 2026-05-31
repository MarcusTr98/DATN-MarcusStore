IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'MarcusStoreDB')
BEGIN
    CREATE DATABASE MarcusStoreDB;
END
GO

USE MarcusStoreDB;
GO

-- ==========================================
-- XÓA BẢNG (THỨ TỰ NGƯỢC ĐỂ KHÔNG LỖI KHÓA NGOẠI)
-- ==========================================
-- Nhóm CMS, Logs & Tương tác
DROP TABLE IF EXISTS Wishlists;
DROP TABLE IF EXISTS Audit_Logs;
DROP TABLE IF EXISTS Comments_Evaluations;
DROP TABLE IF EXISTS System_Settings;
DROP TABLE IF EXISTS Banners;
DROP TABLE IF EXISTS Banner_Positions;
DROP TABLE IF EXISTS Posts;
DROP TABLE IF EXISTS Post_Categories;

-- Nhóm Quản lý IMEI & Đơn hàng, Giỏ hàng
DROP TABLE IF EXISTS Product_Items; -- Xóa IMEI trước Orders và Skus
DROP TABLE IF EXISTS Order_Items;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Vouchers;
DROP TABLE IF EXISTS Flash_Sale_Items;
DROP TABLE IF EXISTS Flash_Sale_Slots;
DROP TABLE IF EXISTS Cart_Items;
DROP TABLE IF EXISTS Carts;

-- Nhóm Sản phẩm & Biến thể
DROP TABLE IF EXISTS Sku_Attribute_Values;
DROP TABLE IF EXISTS Attribute_Values;
DROP TABLE IF EXISTS Attributes;
DROP TABLE IF EXISTS Product_Skus;
DROP TABLE IF EXISTS Product_Images;
DROP TABLE IF EXISTS Products;
DROP TABLE IF EXISTS Categories;

-- Nhóm Địa chỉ khách hàng
DROP TABLE IF EXISTS User_Addresses;
DROP TABLE IF EXISTS Wards;
DROP TABLE IF EXISTS Districts;
DROP TABLE IF EXISTS Provinces;

-- Nhóm Phân quyền
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS Role_Permissions;
DROP TABLE IF EXISTS Permissions;
DROP TABLE IF EXISTS Roles;
GO


-- TÀI KHOẢN & PHÂN QUYỀN
CREATE TABLE Roles (
    role_id INT IDENTITY(1,1),
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(255),
    CONSTRAINT PK_Roles PRIMARY KEY (role_id)
);

CREATE TABLE Permissions (
    permission_id INT IDENTITY(1,1),
    permission_name VARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(255),
    module_name VARCHAR(50), 
    CONSTRAINT PK_Permissions PRIMARY KEY (permission_id)
);

CREATE TABLE Role_Permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    CONSTRAINT PK_Role_Permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT FK_RolePerm_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id) ON DELETE CASCADE,
    CONSTRAINT FK_RolePerm_Perms FOREIGN KEY (permission_id) REFERENCES Permissions(permission_id) ON DELETE CASCADE
);

CREATE TABLE Users (
    user_id INT IDENTITY(1,1),
    role_id INT NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(15),
    full_name NVARCHAR(100),
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Users PRIMARY KEY (user_id),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);

-- MODULE ĐỊA CHỈ HÀNH CHÍNH & KHÁCH HÀNG
CREATE TABLE Provinces ( 
    province_code VARCHAR(20) PRIMARY KEY, 
    province_name NVARCHAR(100) NOT NULL 
);

CREATE TABLE Districts (
    district_code VARCHAR(20) PRIMARY KEY, 
    district_name NVARCHAR(100) NOT NULL, 
    province_code VARCHAR(20) NOT NULL, 
    CONSTRAINT FK_Districts_Provinces FOREIGN KEY (province_code) REFERENCES Provinces(province_code) ON DELETE CASCADE
);

CREATE TABLE Wards (
    ward_code VARCHAR(20) PRIMARY KEY, 
    ward_name NVARCHAR(100) NOT NULL, 
    district_code VARCHAR(20) NOT NULL, 
    CONSTRAINT FK_Wards_Districts FOREIGN KEY (district_code) REFERENCES Districts(district_code) ON DELETE CASCADE
);

CREATE TABLE User_Addresses (
    address_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    recipient_name NVARCHAR(100) NOT NULL, 
    phone_number VARCHAR(10) NOT NULL, 
    province_code VARCHAR(20) NOT NULL,  
    district_code VARCHAR(20) NOT NULL, 
    ward_code VARCHAR(20) NOT NULL, 
    detail_address NVARCHAR(300) NOT NULL, 
    note NVARCHAR(300), 
    is_default BIT DEFAULT 0, 
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),

    CONSTRAINT FK_UserAddr_Users FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_UserAddr_Province FOREIGN KEY (province_code) REFERENCES Provinces(province_code),
    CONSTRAINT FK_UserAddr_District FOREIGN KEY (district_code) REFERENCES Districts(district_code),
    CONSTRAINT FK_UserAddr_Ward FOREIGN KEY (ward_code) REFERENCES Wards(ward_code),
    CONSTRAINT CK_PhoneNumber CHECK (phone_number NOT LIKE '%[^0-9]%' AND LEN(phone_number) = 10 AND phone_number LIKE '0%')
);


-- QUẢN TRỊ DANH MỤC & SẢN PHẨM LÕI

CREATE TABLE Categories (
    category_id INT IDENTITY(1,1),
    category_name NVARCHAR(100) NOT NULL,
    parent_id INT NULL, 
    status BIT DEFAULT 1,
    CONSTRAINT PK_Categories PRIMARY KEY (category_id),
    CONSTRAINT FK_Categories_Parent FOREIGN KEY (parent_id) REFERENCES Categories(category_id)
);

CREATE TABLE Products (
    product_id INT IDENTITY(1,1),
    product_name NVARCHAR(150) NOT NULL,
    description NVARCHAR(MAX),
    brand NVARCHAR(50),
    thumbnail_url VARCHAR(500) NULL,
    category_id INT NOT NULL,
    status BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Products PRIMARY KEY (product_id),
    CONSTRAINT FK_Products_Categories FOREIGN KEY (category_id) REFERENCES Categories(category_id) 
);

CREATE TABLE Product_Images (
    image_id INT IDENTITY(1,1),
    product_id INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BIT DEFAULT 0, 
    display_order INT DEFAULT 0,
    CONSTRAINT PK_Product_Images PRIMARY KEY (image_id),
    CONSTRAINT FK_ProductImages_Products FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE
);


-- THUỘC TÍNH ĐỘNG & BIẾN THỂ (SKU)

CREATE TABLE Product_Skus (
    sku_id INT IDENTITY(1,1),
    product_id INT NOT NULL,
    sku_code VARCHAR(50) NOT NULL UNIQUE, 
    sku_image_url VARCHAR(500) NULL,
    price DECIMAL(18,2) NOT NULL CHECK (price >= 0),
    stock_quantity INT NOT NULL CHECK (stock_quantity >= 0),
    is_active BIT DEFAULT 1,
    CONSTRAINT PK_Product_Skus PRIMARY KEY (sku_id),
    CONSTRAINT FK_ProductSkus_Products FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE
);

CREATE TABLE Attributes (
    attribute_id INT IDENTITY(1,1),
    attribute_name NVARCHAR(50) NOT NULL UNIQUE, 
    CONSTRAINT PK_Attributes PRIMARY KEY (attribute_id)
);

CREATE TABLE Attribute_Values (
    value_id INT IDENTITY(1,1),
    attribute_id INT NOT NULL,
    value_string NVARCHAR(100) NOT NULL, 
    CONSTRAINT PK_Attribute_Values PRIMARY KEY (value_id),
    CONSTRAINT FK_AttributeValues_Attributes FOREIGN KEY (attribute_id) REFERENCES Attributes(attribute_id) ON DELETE CASCADE
);

CREATE TABLE Sku_Attribute_Values (
    sku_id INT NOT NULL,
    value_id INT NOT NULL,
    CONSTRAINT PK_Sku_Attribute_Values PRIMARY KEY (sku_id, value_id),
    CONSTRAINT FK_SkuAttr_Skus FOREIGN KEY (sku_id) REFERENCES Product_Skus(sku_id) ON DELETE CASCADE,
    CONSTRAINT FK_SkuAttr_Values FOREIGN KEY (value_id) REFERENCES Attribute_Values(value_id) ON DELETE CASCADE
);


-- GIỎ HÀNG & SẢN PHẨM YÊU THÍCH

CREATE TABLE Carts (
    cart_id INT IDENTITY(1,1),
    user_id INT NOT NULL UNIQUE, 
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Carts PRIMARY KEY (cart_id),
    CONSTRAINT FK_Carts_Users FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);

CREATE TABLE Cart_Items (
    cart_item_id INT IDENTITY(1,1),
    cart_id INT NOT NULL,
    sku_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    CONSTRAINT PK_Cart_Items PRIMARY KEY (cart_item_id),
    CONSTRAINT UC_Cart_Sku UNIQUE (cart_id, sku_id), 
    CONSTRAINT FK_CartItems_Carts FOREIGN KEY (cart_id) REFERENCES Carts(cart_id) ON DELETE CASCADE,
    CONSTRAINT FK_CartItems_Skus FOREIGN KEY (sku_id) REFERENCES Product_Skus(sku_id) 
);

CREATE TABLE Wishlists (
    wishlist_id INT IDENTITY(1,1),
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Wishlists PRIMARY KEY (wishlist_id),
    CONSTRAINT UC_User_Product UNIQUE (user_id, product_id),
    CONSTRAINT FK_Wishlists_Users FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_Wishlists_Products FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE
);


-- KHUYẾN MÃI & FLASH SALE (UPDATE MỚI VOUCHER)
CREATE TABLE Vouchers (
    voucher_id INT IDENTITY(1,1),
    voucher_code VARCHAR(50) NOT NULL UNIQUE,
    discount_value DECIMAL(18,2) NOT NULL CHECK (discount_value > 0),
    discount_type VARCHAR(10) NOT NULL CHECK (discount_type IN ('PERCENT', 'AMOUNT')),
    max_discount_amount DECIMAL(18,2) NULL, -- UPDATE CỦA ĐẠT
    min_order_value DECIMAL(18,2) DEFAULT 0,
    start_date DATETIME2 NOT NULL,
    end_date DATETIME2 NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    is_active BIT DEFAULT 1,
    CONSTRAINT PK_Vouchers PRIMARY KEY (voucher_id),
    CONSTRAINT CK_Vouchers_MaxDiscountAmount CHECK (
        (discount_type = 'PERCENT' AND max_discount_amount IS NOT NULL AND max_discount_amount > 0)
        OR
        (discount_type = 'AMOUNT' AND max_discount_amount IS NULL)
    )
);

CREATE TABLE Flash_Sale_Slots (
    slot_id INT IDENTITY(1,1), 
    name NVARCHAR(100) NOT NULL,        
    start_date DATETIME2 NOT NULL,   
    end_date DATETIME2 NOT NULL,     
    status TINYINT NOT NULL DEFAULT 1, 
    created_at DATETIME2 DEFAULT GETDATE(), 
    updated_at DATETIME2 DEFAULT GETDATE(),      
    CONSTRAINT PK_Flash_Sale_Slots PRIMARY KEY (slot_id),
    CONSTRAINT CK_FlashSaleSlots_Dates CHECK (end_date > start_date),
    CONSTRAINT CK_FlashSaleSlots_Status CHECK (status IN (0, 1, 2, 3, 4))
);

CREATE TABLE Flash_Sale_Items (
    slot_id INT NOT NULL,                
    sku_id INT NOT NULL,                
    original_price DECIMAL(18,2) NOT NULL CHECK (original_price >= 0),   
    flash_sale_price DECIMAL(18,2) NOT NULL CHECK (flash_sale_price >= 0), 
    flash_sale_quantity INT NOT NULL CHECK (flash_sale_quantity > 0),        
    sold_quantity INT NOT NULL DEFAULT 0, 
    created_at DATETIME2 DEFAULT GETDATE(), 
    CONSTRAINT PK_Flash_Sale_Items PRIMARY KEY (slot_id, sku_id),
    CONSTRAINT FK_FlashSaleItems_Slots FOREIGN KEY (slot_id) REFERENCES Flash_Sale_Slots(slot_id) ON DELETE CASCADE,
    CONSTRAINT FK_FlashSaleItems_Skus FOREIGN KEY (sku_id) REFERENCES Product_Skus(sku_id) ON DELETE CASCADE,
    CONSTRAINT CK_FlashSaleItems_Price CHECK (flash_sale_price <= original_price),
    CONSTRAINT CK_FlashSaleItems_Qty CHECK (sold_quantity <= flash_sale_quantity)
);

-- QUẢN LÝ ĐƠN HÀNG
CREATE TABLE Orders (
    order_id INT IDENTITY(1,1),
    user_id INT NOT NULL,
    voucher_id INT NULL,
    order_code VARCHAR(50) NOT NULL UNIQUE,
    recipient_name NVARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(15) NOT NULL,
    shipping_address NVARCHAR(500) NOT NULL, -- Dùng để lưu vết địa chỉ dạng text tại thời điểm đặt hàng
    total_amount DECIMAL(18,2) NOT NULL CHECK (total_amount >= 0),
    discount_amount DECIMAL(18,2) DEFAULT 0 CHECK (discount_amount >= 0),
    final_amount DECIMAL(18,2) NOT NULL CHECK (final_amount >= 0),
    payment_method NVARCHAR(50) NOT NULL, 
    payment_status NVARCHAR(50) DEFAULT N'UNPAID', 
    transaction_id VARCHAR(100) NULL,
    order_status NVARCHAR(50) DEFAULT N'PENDING', 
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Orders PRIMARY KEY (order_id),
    CONSTRAINT FK_Orders_Users FOREIGN KEY (user_id) REFERENCES Users(user_id),
    CONSTRAINT FK_Orders_Vouchers FOREIGN KEY (voucher_id) REFERENCES Vouchers(voucher_id) ON DELETE SET NULL
);

CREATE TABLE Order_Items (
    order_item_id INT IDENTITY(1,1),
    order_id INT NOT NULL,
    sku_id INT NOT NULL, 
    quantity INT NOT NULL CHECK (quantity > 0),
    price_at_purchase DECIMAL(18,2) NOT NULL CHECK (price_at_purchase >= 0), 
    CONSTRAINT PK_Order_Items PRIMARY KEY (order_item_id),
    CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id) ON DELETE CASCADE,
    CONSTRAINT FK_OrderItems_Skus FOREIGN KEY (sku_id) REFERENCES Product_Skus(sku_id)
);

-- MODULE QUẢN LÝ IMEI
CREATE TABLE Product_Items (
    item_id INT IDENTITY(1,1) PRIMARY KEY,
    sku_id INT NOT NULL, 
    imei_code VARCHAR(50) NOT NULL UNIQUE,
    status INT DEFAULT 1, -- 1: Trong kho, 2: Đã bán, 3: Đang bảo hành, 4: Lỗi/Trả hàng
    order_item_id INT NULL, -- ĐÃ SỬA: Liên kết vào dòng Chi tiết đơn hàng
    created_at DATETIME2 DEFAULT GETDATE(), -- Ngày nhập kho
    updated_at DATETIME2 DEFAULT GETDATE(), -- Ngày xuất kho (dùng để tính thời hạn bảo hành)
    
    CONSTRAINT FK_ProductItems_Skus FOREIGN KEY (sku_id) REFERENCES Product_Skus(sku_id) ON DELETE CASCADE,
    CONSTRAINT FK_ProductItems_OrderItems FOREIGN KEY (order_item_id) REFERENCES Order_Items(order_item_id) ON DELETE SET NULL
);


-- CMS ĐỘNG & GIAO DIỆN

CREATE TABLE System_Settings (
    setting_key VARCHAR(50) NOT NULL, 
    setting_value NVARCHAR(MAX) NOT NULL, 
    setting_group VARCHAR(50) NOT NULL, 
    description NVARCHAR(255),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_System_Settings PRIMARY KEY (setting_key)
);

CREATE TABLE Post_Categories (
    post_category_id INT IDENTITY(1,1),
    name NVARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE, 
    status BIT DEFAULT 1,
    CONSTRAINT PK_Post_Categories PRIMARY KEY (post_category_id)
);

CREATE TABLE Posts (
    post_id INT IDENTITY(1,1),
    post_category_id INT NOT NULL,
    author_id INT NOT NULL, 
    title NVARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    thumbnail_url VARCHAR(500),
    excerpt NVARCHAR(500), 
    content NVARCHAR(MAX) NOT NULL, 
    is_published BIT DEFAULT 0, 
    published_at DATETIME2 NULL, 
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Posts PRIMARY KEY (post_id),
    CONSTRAINT FK_Posts_PostCategories FOREIGN KEY (post_category_id) REFERENCES Post_Categories(post_category_id),
    CONSTRAINT FK_Posts_Users FOREIGN KEY (author_id) REFERENCES Users(user_id)
);

CREATE TABLE Banner_Positions (
    position_id INT IDENTITY(1,1),
    position_code VARCHAR(50) NOT NULL UNIQUE, 
    description NVARCHAR(255),
    CONSTRAINT PK_Banner_Positions PRIMARY KEY (position_id)
);

CREATE TABLE Banners (
    banner_id INT IDENTITY(1,1),
    position_id INT NOT NULL,
    title NVARCHAR(150),
    image_url VARCHAR(500) NOT NULL,
    target_url VARCHAR(500), 
    display_order INT DEFAULT 0, 
    is_active BIT DEFAULT 1,
    start_date DATETIME2 NULL, 
    end_date DATETIME2 NULL,
    CONSTRAINT PK_Banners PRIMARY KEY (banner_id),
    CONSTRAINT FK_Banners_Positions FOREIGN KEY (position_id) REFERENCES Banner_Positions(position_id) ON DELETE CASCADE
);


-- TƯƠNG TÁC & AUDIT LOGS

CREATE TABLE Comments_Evaluations (
    review_id INT IDENTITY(1,1),
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    comment_text NVARCHAR(MAX),
    is_approved BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Comments_Evaluations PRIMARY KEY (review_id),
    CONSTRAINT FK_Reviews_Users FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_Reviews_Products FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE
);

CREATE TABLE Audit_Logs (
    log_id INT IDENTITY(1,1),
    user_id INT NULL, 
    action_type NVARCHAR(50) NOT NULL, 
    table_name VARCHAR(50) NOT NULL, 
    description NVARCHAR(MAX), 
    ip_address VARCHAR(45),
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Audit_Logs PRIMARY KEY (log_id),
    CONSTRAINT FK_AuditLogs_Users FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE SET NULL
);
GO

-- TẠO INDEXES
CREATE INDEX IX_ProductSkus_Product ON Product_Skus(product_id);
CREATE INDEX IX_Orders_User ON Orders(user_id);
CREATE INDEX IX_CartItems_Cart ON Cart_Items(cart_id);
CREATE INDEX IX_AuditLogs_CreatedAt ON Audit_Logs(created_at DESC);
CREATE INDEX IX_Posts_Slug ON Posts(slug);
CREATE INDEX IX_Banners_Position ON Banners(position_id);
CREATE INDEX IX_FlashSaleSlots_Status_Time ON Flash_Sale_Slots(status, start_date, end_date);
CREATE INDEX IX_FlashSaleItems_Sku ON Flash_Sale_Items(sku_id, slot_id);
CREATE INDEX IX_UserAddresses_UserId ON User_Addresses(user_id); 
CREATE UNIQUE INDEX UX_User_DefaultAddress ON User_Addresses(user_id) WHERE is_default = 1;
GO