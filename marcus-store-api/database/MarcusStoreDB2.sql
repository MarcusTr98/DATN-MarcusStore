IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'MarcusStoreDB')
BEGIN
    CREATE DATABASE MarcusStoreDB;
END
GO

USE MarcusStoreDB;
GO

-- AUTH & RBAC
CREATE TABLE Roles (
    role_id     INT IDENTITY(1,1),
    role_name   VARCHAR(50)    NOT NULL UNIQUE,
    description NVARCHAR(255),
    CONSTRAINT PK_Roles PRIMARY KEY (role_id)
);

CREATE TABLE Permissions (
    permission_id   INT IDENTITY(1,1),
    permission_name VARCHAR(100)   NOT NULL UNIQUE,
    description     NVARCHAR(255),
    module_name     VARCHAR(50),
    CONSTRAINT PK_Permissions PRIMARY KEY (permission_id)
);

CREATE TABLE Role_Permissions (
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    CONSTRAINT PK_Role_Permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT FK_RolePerm_Roles FOREIGN KEY (role_id)       REFERENCES Roles(role_id)       ON DELETE CASCADE,
    CONSTRAINT FK_RolePerm_Perms FOREIGN KEY (permission_id) REFERENCES Permissions(permission_id) ON DELETE CASCADE
);

CREATE TABLE Users (
    user_id       INT IDENTITY(1,1),
    role_id       INT            NOT NULL,
    username      VARCHAR(50)    NOT NULL UNIQUE,
    password_hash VARCHAR(255)   NOT NULL,
    email         VARCHAR(100)   NOT NULL UNIQUE,
    phone_number  VARCHAR(15),
    full_name     NVARCHAR(100),
    is_active     BIT            DEFAULT 1,
    created_at    DATETIME2      DEFAULT GETDATE(),
    updated_at    DATETIME2      DEFAULT GETDATE(),
    CONSTRAINT PK_Users       PRIMARY KEY (user_id),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);
ALTER TABLE Users
ADD email_verified BIT DEFAULT 0

ALTER TABLE Users
ADD google_account_id VARCHAR(100) NULL;

ALTER TABLE Users
ALTER COLUMN password_hash VARCHAR(255) NULL;

-- Thêm cột facebook_account_id - cần xóa
ALTER TABLE users ADD facebook_account_id VARCHAR(255) NULL;

CREATE UNIQUE NONCLUSTERED INDEX uq_users_facebook_account_id
ON dbo.Users (facebook_account_id)
WHERE facebook_account_id IS NOT NULL;
GO


-- ============================================================
-- CUSTOMER ADDRESSES
-- ============================================================

CREATE TABLE User_Addresses (
    address_id     INT IDENTITY(1,1) PRIMARY KEY,
    user_id        INT            NOT NULL,
    recipient_name NVARCHAR(100)  NOT NULL,
    phone_number   VARCHAR(10)    NOT NULL,
    province_name  NVARCHAR(100)  NOT NULL,
    district_name  NVARCHAR(100)  NOT NULL,
    ward_name      NVARCHAR(100)  NOT NULL,
    detail_address NVARCHAR(300)  NOT NULL,
    note           NVARCHAR(300),
    is_default     BIT            DEFAULT 0,
    created_at     DATETIME2      DEFAULT GETDATE(),
    updated_at     DATETIME2      DEFAULT GETDATE(),
    CONSTRAINT FK_UserAddr_Users FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    CONSTRAINT CK_PhoneNumber CHECK (
        phone_number NOT LIKE '%[^0-9]%'
        AND LEN(phone_number) = 10
        AND phone_number LIKE '0%'
    )
);
ALTER TABLE User_Addresses 
ADD latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL;

ALTER TABLE User_Addresses 
ADD province_id INT NULL,
    district_id INT NULL,
    ward_code VARCHAR(20) NULL;

-- ============================================================
-- CATEGORIES & PRODUCTS
-- ============================================================

CREATE TABLE Categories (
    category_id   INT IDENTITY(1,1),
    category_name NVARCHAR(100)  NOT NULL,
    parent_id     INT            NULL,
    slug          VARCHAR(255)   NOT NULL UNIQUE,
    status        BIT            DEFAULT 1,
    CONSTRAINT PK_Categories        PRIMARY KEY (category_id),
    CONSTRAINT FK_Categories_Parent FOREIGN KEY (parent_id) REFERENCES Categories(category_id)
);
alter table Categories 
add categori_img varchar(500) null;


CREATE TABLE Products (
    product_id   INT IDENTITY(1,1),
    product_name NVARCHAR(150)  NOT NULL,
    description  NVARCHAR(MAX),
    brand        NVARCHAR(50),
    thumbnail_url VARCHAR(500)  NULL,
    category_id  INT            NOT NULL,
    slug         VARCHAR(255)   NOT NULL UNIQUE,
    status       BIT            DEFAULT 1,
    created_at   DATETIME2      DEFAULT GETDATE(),
    CONSTRAINT PK_Products            PRIMARY KEY (product_id),
    CONSTRAINT FK_Products_Categories FOREIGN KEY (category_id) REFERENCES Categories(category_id)
);

CREATE TABLE Product_Images (
    image_id      INT IDENTITY(1,1),
    product_id    INT          NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    is_primary    BIT          DEFAULT 0,
    display_order INT          DEFAULT 0,
    CONSTRAINT PK_Product_Images         PRIMARY KEY (image_id),
    CONSTRAINT FK_ProductImages_Products  FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE
);
ALTER TABLE Product_Images
ALTER COLUMN display_order INT NULL;


-- ============================================================
-- DYNAMIC ATTRIBUTES & SKU VARIANTS
-- ============================================================

CREATE TABLE Product_Skus (
    sku_id        INT IDENTITY(1,1),
    product_id    INT              NOT NULL,
    sku_code      VARCHAR(50)      NOT NULL UNIQUE,
    sku_image_url VARCHAR(500)     NULL,
    price         DECIMAL(18,2)    NOT NULL CHECK (price >= 0),
    stock_quantity INT             NOT NULL CHECK (stock_quantity >= 0),
    is_active     BIT              DEFAULT 1,
    CONSTRAINT PK_Product_Skus         PRIMARY KEY (sku_id),
    CONSTRAINT FK_ProductSkus_Products  FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE
);

ALTER TABLE Product_Skus 
ADD weight_gram INT NOT NULL DEFAULT 500 CHECK (weight_gram > 0);
ALTER TABLE Product_Skus ADD original_price DECIMAL(18,2) NULL;


CREATE TABLE Attributes (
    attribute_id   INT IDENTITY(1,1),
    attribute_name NVARCHAR(50) NOT NULL UNIQUE,
    CONSTRAINT PK_Attributes PRIMARY KEY (attribute_id)
);

CREATE TABLE Attribute_Values (
    value_id     INT IDENTITY(1,1),
    attribute_id INT          NOT NULL,
    value_string NVARCHAR(100) NOT NULL,
    CONSTRAINT PK_Attribute_Values          PRIMARY KEY (value_id),
    CONSTRAINT FK_AttributeValues_Attributes FOREIGN KEY (attribute_id) REFERENCES Attributes(attribute_id) ON DELETE CASCADE
);

ALTER TABLE Attribute_Values 
ADD value_meta NVARCHAR(50) NULL;

CREATE TABLE Sku_Attribute_Values (
    sku_id   INT NOT NULL,
    value_id INT NOT NULL,
    CONSTRAINT PK_Sku_Attribute_Values PRIMARY KEY (sku_id, value_id),
    CONSTRAINT FK_SkuAttr_Skus         FOREIGN KEY (sku_id)   REFERENCES Product_Skus(sku_id)       ON DELETE CASCADE,
    CONSTRAINT FK_SkuAttr_Values       FOREIGN KEY (value_id) REFERENCES Attribute_Values(value_id) ON DELETE CASCADE
);


-- ============================================================
-- CART & WISHLIST
-- ============================================================

CREATE TABLE Carts (
    cart_id    INT IDENTITY(1,1),
    user_id    INT       NOT NULL UNIQUE,
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Carts       PRIMARY KEY (cart_id),
    CONSTRAINT FK_Carts_Users FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);

CREATE TABLE Cart_Items (
    cart_item_id INT IDENTITY(1,1),
    cart_id      INT NOT NULL,
    sku_id       INT NOT NULL,
    quantity     INT NOT NULL CHECK (quantity > 0),
    CONSTRAINT PK_Cart_Items      PRIMARY KEY (cart_item_id),
    CONSTRAINT UC_Cart_Sku        UNIQUE (cart_id, sku_id),
    CONSTRAINT FK_CartItems_Carts FOREIGN KEY (cart_id) REFERENCES Carts(cart_id)        ON DELETE CASCADE,
    CONSTRAINT FK_CartItems_Skus  FOREIGN KEY (sku_id)  REFERENCES Product_Skus(sku_id)
);
-- Thêm cột lưu giá Flash Sale (NULL = không phải sản phẩm Flash Sale)
ALTER TABLE Cart_Items ADD flash_sale_price DECIMAL(18, 2) NULL;
-- Thêm cột tham chiếu đến FlashSaleSlot (biết sản phẩm thuộc slot nào)
ALTER TABLE Cart_Items ADD flash_sale_slot_id INT NULL;

-- Thêm khóa ngoại
ALTER TABLE Cart_Items ADD CONSTRAINT FK_CartItems_FlashSaleSlot FOREIGN KEY (flash_sale_slot_id) REFERENCES Flash_Sale_Slots(slot_id);


CREATE TABLE Wishlists (
    wishlist_id INT IDENTITY(1,1),
    user_id     INT       NOT NULL,
    product_id  INT       NOT NULL,
    created_at  DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_Wishlists          PRIMARY KEY (wishlist_id),
    CONSTRAINT UC_User_Product       UNIQUE (user_id, product_id),
    CONSTRAINT FK_Wishlists_Users    FOREIGN KEY (user_id)    REFERENCES Users(user_id)       ON DELETE CASCADE,
    CONSTRAINT FK_Wishlists_Products FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE
);


-- ============================================================
-- PROMOTIONS & FLASH SALE
-- ============================================================

CREATE TABLE Vouchers (
    voucher_id          INT IDENTITY(1,1) PRIMARY KEY,
    voucher_code        VARCHAR(50)   NOT NULL UNIQUE,
    discount_value      DECIMAL(18,2) NOT NULL CHECK (discount_value > 0),
    discount_type       VARCHAR(20)   NOT NULL, -- 'PERCENT', 'AMOUNT', 'FREESHIP', 'GIFT'
    max_discount_amount DECIMAL(18,2) NULL,
    min_order_value     DECIMAL(18,2) DEFAULT 0,
    start_date          DATETIME2     NOT NULL,
    end_date            DATETIME2     NOT NULL,
    quantity            INT           NOT NULL CHECK (quantity >= 0),
    is_active           BIT           DEFAULT 1,
    target_type         VARCHAR(20)   DEFAULT 'ALL'
);
ALTER TABLE Vouchers ADD CONSTRAINT CK_Vouchers_Discount_Type 
CHECK (discount_type IN ('PERCENT', 'AMOUNT', 'FREESHIP', 'GIFT'));


select*from Flash_Sale_Slots
select*from Flash_Sale_Items

CREATE TABLE Flash_Sale_Slots (
    slot_id    INT IDENTITY(1,1),
    name       NVARCHAR(100) NOT NULL,
    start_date DATETIME2     NOT NULL,
    end_date   DATETIME2     NOT NULL,
    status     TINYINT       NOT NULL DEFAULT 1,
    created_at DATETIME2     DEFAULT GETDATE(),
    updated_at DATETIME2     DEFAULT GETDATE(),
    CONSTRAINT PK_Flash_Sale_Slots        PRIMARY KEY (slot_id),
    CONSTRAINT CK_FlashSaleSlots_Dates    CHECK (end_date > start_date),
    CONSTRAINT CK_FlashSaleSlots_Status   CHECK (status IN (0, 1, 2, 3, 4))
);
alter table Flash_Sale_Slots
 add banner_image_url nvarchar(500);


CREATE TABLE Flash_Sale_Items (
    slot_id             INT           NOT NULL,
    sku_id              INT           NOT NULL,
    original_price      DECIMAL(18,2) NOT NULL CHECK (original_price >= 0),
    flash_sale_price    DECIMAL(18,2) NOT NULL CHECK (flash_sale_price >= 0),
    flash_sale_quantity INT           NOT NULL CHECK (flash_sale_quantity > 0),
    sold_quantity       INT           NOT NULL DEFAULT 0,
    created_at          DATETIME2     DEFAULT GETDATE(),
    CONSTRAINT PK_Flash_Sale_Items        PRIMARY KEY (slot_id, sku_id),
    CONSTRAINT FK_FlashSaleItems_Slots    FOREIGN KEY (slot_id) REFERENCES Flash_Sale_Slots(slot_id) ON DELETE CASCADE,
    CONSTRAINT FK_FlashSaleItems_Skus     FOREIGN KEY (sku_id)  REFERENCES Product_Skus(sku_id)      ON DELETE CASCADE,
    CONSTRAINT CK_FlashSaleItems_Price    CHECK (flash_sale_price <= original_price),
    CONSTRAINT CK_FlashSaleItems_Qty      CHECK (sold_quantity <= flash_sale_quantity)
);


-- ============================================================
-- ORDERS
-- ============================================================

CREATE TABLE Orders (
    order_id         INT IDENTITY(1,1),
    user_id          INT            NOT NULL,
    voucher_id       INT            NULL,
    order_code       VARCHAR(50)    NOT NULL UNIQUE,
    recipient_name   NVARCHAR(100)  NOT NULL,
    recipient_phone  VARCHAR(15)    NOT NULL,
    shipping_address NVARCHAR(500)  NOT NULL,
    total_amount     DECIMAL(18,2)  NOT NULL CHECK (total_amount >= 0),
    discount_amount  DECIMAL(18,2)  DEFAULT 0 CHECK (discount_amount >= 0),
    final_amount     DECIMAL(18,2)  NOT NULL CHECK (final_amount >= 0),
    payment_method   NVARCHAR(50)   NOT NULL,
    payment_status   NVARCHAR(50)   DEFAULT N'UNPAID',
    transaction_id   VARCHAR(100)   NULL,
    order_status     NVARCHAR(50)   DEFAULT N'PENDING',
    created_at       DATETIME2      DEFAULT GETDATE(),
    updated_at       DATETIME2      DEFAULT GETDATE(),
    CONSTRAINT PK_Orders          PRIMARY KEY (order_id),
    CONSTRAINT FK_Orders_Users    FOREIGN KEY (user_id)    REFERENCES Users(user_id),
    CONSTRAINT FK_Orders_Vouchers FOREIGN KEY (voucher_id) REFERENCES Vouchers(voucher_id) ON DELETE SET NULL
);
ALTER TABLE Orders ADD shipping_fee DECIMAL(18,2) DEFAULT 0 CHECK (shipping_fee >= 0);
ALTER TABLE Orders ADD tracking_code VARCHAR(100) NULL;
ALTER TABLE Orders ADD payment_date DATETIME2;
-- transaction GHN Marcus
ALTER TABLE Orders ADD to_district_id INT NULL;
ALTER TABLE Orders ADD to_ward_code VARCHAR(20) NULL;
ALTER TABLE Orders ADD shipping_subsidy DECIMAL(18,2) DEFAULT 0 CHECK (shipping_subsidy >= 0);
ALTER TABLE Orders ADD customer_shipping_fee DECIMAL(18,2) NULL;
ALTER TABLE Orders ADD delivery_note NVARCHAR(500) NULL;


CREATE TABLE Order_Items (
    order_item_id       INT IDENTITY(1,1),
    order_id            INT           NOT NULL,
    sku_id              INT           NOT NULL,
    quantity            INT           NOT NULL CHECK (quantity > 0),
    price_at_purchase   DECIMAL(18,2) NOT NULL CHECK (price_at_purchase >= 0),
    CONSTRAINT PK_Order_Items        PRIMARY KEY (order_item_id),
    CONSTRAINT FK_OrderItems_Orders  FOREIGN KEY (order_id) REFERENCES Orders(order_id)      ON DELETE CASCADE,
    CONSTRAINT FK_OrderItems_Skus    FOREIGN KEY (sku_id)   REFERENCES Product_Skus(sku_id)
);
ALTER TABLE Order_Items ADD is_flash_sale BIT NULL DEFAULT 0;
ALTER TABLE Order_Items ADD original_price DECIMAL(18, 2) NULL;
ALTER TABLE Order_Items ADD flash_sale_slot_name NVARCHAR(100) NULL;
-- Thêm cột flash_sale_slot_id vào bảng Order_Items
ALTER TABLE Order_Items 
ADD flash_sale_slot_id INT NULL;
-- Thêm foreign key constraint (nếu muốn ràng buộc quan hệ)
ALTER TABLE Order_Items 
ADD CONSTRAINT FK_OrderItems_FlashSaleSlot 
FOREIGN KEY (flash_sale_slot_id) REFERENCES Flash_Sale_Slots(slot_id);


-- ============================================================
-- IMEI MANAGEMENT
-- ============================================================

CREATE TABLE Product_Items (
    item_id       INT IDENTITY(1,1) PRIMARY KEY,
    sku_id        INT          NOT NULL,
    imei_code     VARCHAR(50)  NOT NULL UNIQUE,
    status        INT          DEFAULT 1,  -- 1: In stock | 2: Sold | 3: In warranty | 4: Defective/Returned
    order_item_id INT          NULL,
    created_at    DATETIME2    DEFAULT GETDATE(),
    updated_at    DATETIME2    DEFAULT GETDATE(),
    CONSTRAINT FK_ProductItems_Skus       FOREIGN KEY (sku_id)        REFERENCES Product_Skus(sku_id)    ON DELETE CASCADE,
    CONSTRAINT FK_ProductItems_OrderItems FOREIGN KEY (order_item_id) REFERENCES Order_Items(order_item_id) ON DELETE SET NULL
);
-- ============================================================
-- CMS & UI
-- ============================================================
SELECt * FROM System_Settings
CREATE TABLE System_Settings (
    setting_key   VARCHAR(50)    NOT NULL,
    setting_value NVARCHAR(MAX)  NOT NULL,
    setting_group VARCHAR(50)    NOT NULL,
    description   NVARCHAR(255),
    updated_at    DATETIME2      DEFAULT GETDATE(),
    CONSTRAINT PK_System_Settings PRIMARY KEY (setting_key)
);

CREATE TABLE Post_Categories (
    post_category_id INT IDENTITY(1,1),
    name             NVARCHAR(100) NOT NULL UNIQUE,
    slug             VARCHAR(100)  NOT NULL UNIQUE,
    status           BIT           DEFAULT 1,
    CONSTRAINT PK_Post_Categories PRIMARY KEY (post_category_id)
);

CREATE TABLE Posts (
    post_id          INT IDENTITY(1,1),
    post_category_id INT            NOT NULL,
    author_id        INT            NOT NULL,
    title            NVARCHAR(255)  NOT NULL,
    slug             VARCHAR(255)   NOT NULL UNIQUE,
    thumbnail_url    VARCHAR(500),
    excerpt          NVARCHAR(500),
    content          NVARCHAR(MAX)  NOT NULL,
    is_published     BIT            DEFAULT 0,
    published_at     DATETIME2      NULL,
    created_at       DATETIME2      DEFAULT GETDATE(),
    updated_at       DATETIME2      DEFAULT GETDATE(),
    CONSTRAINT PK_Posts                  PRIMARY KEY (post_id),
    CONSTRAINT FK_Posts_PostCategories   FOREIGN KEY (post_category_id) REFERENCES Post_Categories(post_category_id),
    CONSTRAINT FK_Posts_Users            FOREIGN KEY (author_id)        REFERENCES Users(user_id)
);

CREATE TABLE Banner_Positions (
    position_id   INT IDENTITY(1,1),
    position_code VARCHAR(50)   NOT NULL UNIQUE,
    description   NVARCHAR(255),
    CONSTRAINT PK_Banner_Positions PRIMARY KEY (position_id)
);

CREATE TABLE Banners (
    banner_id     INT IDENTITY(1,1),
    position_id   INT          NOT NULL,
    title         NVARCHAR(150),
    image_url     VARCHAR(500) NOT NULL,
    target_url    VARCHAR(500),
    display_order INT          DEFAULT 0,
    is_active     BIT          DEFAULT 1,
    start_date    DATETIME2    NULL,
    end_date      DATETIME2    NULL,
    CONSTRAINT PK_Banners          PRIMARY KEY (banner_id),
    CONSTRAINT FK_Banners_Positions FOREIGN KEY (position_id) REFERENCES Banner_Positions(position_id) ON DELETE CASCADE
);


-- ============================================================
-- INTERACTION & AUDIT
-- ============================================================

CREATE TABLE Comments_Evaluations (
    review_id    INT IDENTITY(1,1),
    user_id      INT           NOT NULL,
    product_id   INT           NOT NULL,
    rating       INT           CHECK (rating BETWEEN 1 AND 5),
    comment_text NVARCHAR(MAX),
    is_approved  BIT           DEFAULT 0,
    created_at   DATETIME2     DEFAULT GETDATE(),
    CONSTRAINT PK_Comments_Evaluations PRIMARY KEY (review_id),
    CONSTRAINT FK_Reviews_Users        FOREIGN KEY (user_id)    REFERENCES Users(user_id)       ON DELETE CASCADE,
    CONSTRAINT FK_Reviews_Products     FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE
);

CREATE TABLE Audit_Logs (
    log_id      INT IDENTITY(1,1),
    user_id     INT           NULL,
    action_type NVARCHAR(50)  NOT NULL,
    table_name  VARCHAR(50)   NOT NULL,
    description NVARCHAR(MAX),
    ip_address  VARCHAR(45),
    created_at  DATETIME2     DEFAULT GETDATE(),
    CONSTRAINT PK_Audit_Logs       PRIMARY KEY (log_id),
    CONSTRAINT FK_AuditLogs_Users  FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE SET NULL
);
GO

-- Thêm mới 10/6/2026
--Mục đích dùng để lưu mã OTP khi gửi qua email của user khi đăng ký , quên mk ..
CREATE TABLE EmailOtps (
    otp_id INT IDENTITY(1,1),
    email VARCHAR(100) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expired_at DATETIME2 NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),

    CONSTRAINT PK_EmailOtps
        PRIMARY KEY (otp_id)
);
GO
--Là bảng lưu tài khoản chưa kích hoạt (chưa verify OTP)
CREATE TABLE PendingRegistrations (
    pending_id INT IDENTITY(1,1),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(15),
    full_name NVARCHAR(100),
    created_at DATETIME2 DEFAULT GETDATE(),
    expired_at DATETIME2 NOT NULL,

    CONSTRAINT PK_PendingRegistrations
        PRIMARY KEY (pending_id)
);
GO
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('PendingRegistrations')
      AND name = 'is_newsletter_signup'
)
BEGIN
    ALTER TABLE PendingRegistrations
    ADD is_newsletter_signup BIT NOT NULL DEFAULT 0;
END
GO



--Là bảng lưu lịch sử thao tác với đơn
USE MarcusStoreDB;
CREATE TABLE Order_Status_History (
    history_id INT IDENTITY(1,1),
    order_id INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    title NVARCHAR(255)NULL,
    note NVARCHAR(500) NULL,
    created_by INT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),

    CONSTRAINT PK_Order_Status_History PRIMARY KEY (history_id),

    CONSTRAINT FK_OrderStatusHistory_Orders
        FOREIGN KEY (order_id)
        REFERENCES Orders(order_id)
        ON DELETE CASCADE,

    CONSTRAINT FK_OrderStatusHistory_Users
        FOREIGN KEY (created_by)
        REFERENCES Users(user_id)
        ON DELETE SET NULL
);

CREATE TABLE Order_Status_History (
    history_id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT NOT NULL,
    status VARCHAR(50) NOT NULL, -- Chỉ chứa mã tiếng Anh (PENDING, v.v.)
    title NVARCHAR(255) NULL,    -- Ép kiểu N để lưu tiếng Việt
    note NVARCHAR(500) NULL,     -- Ép kiểu N để lưu tiếng Việt
    created_by INT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_OrderStatusHistory_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id) ON DELETE CASCADE
);
CREATE INDEX IX_OrderStatusHistory_Order_CreatedAt
ON Order_Status_History(order_id, created_at);


-- 2: thêm cột is_hidden vào bảng orders
ALTER TABLE Orders
ADD is_hidden BIT NOT NULL
CONSTRAINT DF_Orders_IsHidden DEFAULT 0;

-- 3: update cột is_hidden sau khi thêm cột mới
UPDATE Orders
SET is_hidden = 0;


-- Marcus thêm xử lý khiếu nại
CREATE TABLE Contact_Requests (
    contact_id INT IDENTITY(1,1) PRIMARY KEY,
    customer_name NVARCHAR(100) NOT NULL,
    phone_number VARCHAR(15) NOT NULL,
    email VARCHAR(100),
    message NVARCHAR(1000) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING (Mới), IN_PROGRESS (Đang xử lý), RESOLVED (Đã giải quyết)
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2
);
ALTER TABLE Contact_Requests ADD user_id INT NULL;
ALTER TABLE Contact_Requests ADD CONSTRAINT FK_Contact_User FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE SET NULL;

-- Marcus thêm bảng lưu trữ Chuông thông báo
CREATE TABLE Admin_Notifications (
    id INT IDENTITY(1,1) PRIMARY KEY,
    type VARCHAR(50) NOT NULL, -- 'ORDER' hoặc 'CONTACT'
    title NVARCHAR(255) NOT NULL,
    message NVARCHAR(1000) NOT NULL,
    reference_id VARCHAR(50) NULL, -- Lưu orderCode hoặc contact_id để click vào chuyển trang
    is_read BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE()
);

-- Marcus thêm để tính phí ship riêng
USE MarcusStoreDB;
GO

CREATE TABLE Shipping_Config (
    config_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    threshold_value DECIMAL(18,2) NOT NULL, -- Ngưỡng freeship (VD: 5.000.000)
    min_order_value DECIMAL(18,2) DEFAULT 0, -- Ngưỡng chặn đơn (VD: 200.000)
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);
GO
-- Thêm cột Trợ giá vận chuyển tối đa
ALTER TABLE Shipping_Config ADD max_freeship_amount DECIMAL(18,2) DEFAULT 60000;

-- Thêm cột Cấu hình mức khai giá bảo hiểm tối đa
ALTER TABLE Shipping_Config ADD max_insurance_value DECIMAL(18,2) DEFAULT 5000000;
UPDATE Shipping_Config SET max_freeship_amount = 60000, max_insurance_value = 5000000 WHERE max_freeship_amount IS NULL;

-- Đạt thêm voucher 25/06
-- =============================================
-- 3. Tạo bảng User_Vouchers (Đạt thêm)
CREATE TABLE User_Vouchers (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    voucher_id    INT NOT NULL,
    user_id       INT NOT NULL,
    assigned_at   DATETIME2 DEFAULT GETDATE(),
    is_used       BIT NOT NULL DEFAULT 0,
    used_at       DATETIME2 NULL,
    created_at    DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT UK_User_Vouchers_Voucher_User UNIQUE (voucher_id, user_id),
    CONSTRAINT FK_UserVouchers_Vouchers FOREIGN KEY (voucher_id) REFERENCES Vouchers(voucher_id) ON DELETE CASCADE,
    CONSTRAINT FK_UserVouchers_Users FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);

-- Marcus thêm 29/06
CREATE TABLE Order_Transactions (
    transaction_id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT NOT NULL,
    amount DECIMAL(18,2) NOT NULL, -- Số tiền thực tế chuyển
    type VARCHAR(50) NOT NULL,    -- VNPAY_PAYMENT, COD_COLLECTION, REFUND_VNPAY
    status VARCHAR(20) NOT NULL,  -- SUCCESS, PENDING, FAILED
    note NVARCHAR(500),           -- Lưu ghi chú ví dụ: "GHN xác nhận giao thành công ngày..."
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_OrderTrans_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id) ON DELETE CASCADE
);
ALTER TABLE Order_Transactions ADD is_reconciled BIT DEFAULT 0 NOT NULL;
ALTER TABLE Order_Transactions ADD idempotency_key VARCHAR(150) NULL;
ALTER TABLE Order_Transactions ADD provider_transaction_id VARCHAR(100) NULL;
ALTER TABLE Order_Transactions ADD provider_response_code VARCHAR(20) NULL;
ALTER TABLE Order_Transactions ADD provider_transaction_date VARCHAR(14) NULL;
CREATE UNIQUE INDEX UX_OrderTransactions_IdempotencyKey
    ON Order_Transactions(idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE Refund_Requests (
    refund_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id INT NOT NULL,
    payment_transaction_id INT NOT NULL,
    refund_transaction_id INT NULL,
    request_code VARCHAR(32) NOT NULL UNIQUE,
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    amount DECIMAL(18,2) NOT NULL CHECK (amount > 0),
    shipping_deducted DECIMAL(18,2) NOT NULL DEFAULT 0,
    reason NVARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_by INT NULL,
    approved_by INT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    next_retry_at DATETIME2 NULL,
    provider_response_id VARCHAR(100) NULL,
    provider_refund_transaction_id VARCHAR(100) NULL,
    provider_response_code VARCHAR(20) NULL,
    provider_transaction_status VARCHAR(20) NULL,
    provider_message NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    approved_at DATETIME2 NULL,
    last_attempt_at DATETIME2 NULL,
    processed_at DATETIME2 NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT FK_RefundRequests_Order FOREIGN KEY (order_id) REFERENCES Orders(order_id),
    CONSTRAINT FK_RefundRequests_PaymentTransaction FOREIGN KEY (payment_transaction_id)
        REFERENCES Order_Transactions(transaction_id),
    CONSTRAINT FK_RefundRequests_RefundTransaction FOREIGN KEY (refund_transaction_id)
        REFERENCES Order_Transactions(transaction_id),
    CONSTRAINT FK_RefundRequests_RequestedBy FOREIGN KEY (requested_by) REFERENCES Users(user_id),
    CONSTRAINT FK_RefundRequests_ApprovedBy FOREIGN KEY (approved_by) REFERENCES Users(user_id)
);
CREATE INDEX IX_RefundRequests_StatusRetry ON Refund_Requests(status, next_retry_at);

-- Ngọc thêm 1/7 bảng user_Permission
CREATE TABLE User_Permissions(
    user_id INT NOT NULL,
    permission_id INT NOT NULL,

    CONSTRAINT PK_User_Permissions
        PRIMARY KEY(user_id, permission_id),

    CONSTRAINT FK_UP_User
        FOREIGN KEY(user_id)
        REFERENCES Users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT FK_UP_Permission
        FOREIGN KEY(permission_id)
        REFERENCES Permissions(permission_id)
        ON DELETE CASCADE
);

-- Đức thêm 8/7
CREATE TABLE Spec_Attributes (
    spec_attribute_id INT IDENTITY(1,1),
    category_id INT NOT NULL,
    name NVARCHAR(100) NOT NULL,
    unit NVARCHAR(20) NULL,
    data_type VARCHAR(20) NOT NULL DEFAULT 'text', -- 'number', 'text', 'boolean'
    display_order INT DEFAULT 0,
    CONSTRAINT PK_Spec_Attributes PRIMARY KEY (spec_attribute_id),
    CONSTRAINT FK_SpecAttr_Category FOREIGN KEY (category_id) REFERENCES Categories(category_id) ON DELETE CASCADE,
    CONSTRAINT UQ_SpecAttr_CategoryName UNIQUE (category_id, name)
);

CREATE TABLE Product_Spec_Values (
    id INT IDENTITY(1,1),
    product_id INT NOT NULL,
    spec_attribute_id INT NOT NULL,
    value_text NVARCHAR(255) NOT NULL,
    CONSTRAINT PK_Product_Spec_Values PRIMARY KEY (id),
    CONSTRAINT FK_ProdSpec_Product FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE CASCADE,
    CONSTRAINT FK_ProdSpec_Attribute FOREIGN KEY (spec_attribute_id) REFERENCES Spec_Attributes(spec_attribute_id) ON DELETE CASCADE,
    CONSTRAINT UQ_ProductSpec UNIQUE (product_id, spec_attribute_id)
);



CREATE INDEX IX_OrderTrans_OrderId ON Order_Transactions(order_id);	
CREATE INDEX IX_UserVouchers_UserId ON User_Vouchers(user_id);
CREATE INDEX IX_UserVouchers_VoucherId ON User_Vouchers(voucher_id);
CREATE INDEX IX_VoucherCode ON Vouchers(voucher_code);

CREATE INDEX IX_ProductSkus_Product          ON Product_Skus(product_id);
CREATE INDEX IX_Orders_User                  ON Orders(user_id);
CREATE INDEX IX_CartItems_Cart               ON Cart_Items(cart_id);
CREATE INDEX IX_AuditLogs_CreatedAt          ON Audit_Logs(created_at DESC);
CREATE INDEX IX_Posts_Slug                   ON Posts(slug);
CREATE INDEX IX_Banners_Position             ON Banners(position_id);
CREATE INDEX IX_FlashSaleSlots_Status_Time   ON Flash_Sale_Slots(status, start_date, end_date);
CREATE INDEX IX_FlashSaleItems_Sku           ON Flash_Sale_Items(sku_id, slot_id);
CREATE INDEX IX_UserAddresses_UserId         ON User_Addresses(user_id);
CREATE UNIQUE INDEX UX_User_DefaultAddress   ON User_Addresses(user_id) WHERE is_default = 1;

CREATE NONCLUSTERED INDEX IX_CartItems_FlashSaleSlotId ON Cart_Items (flash_sale_slot_id) WHERE flash_sale_slot_id IS NOT NULL;

GO


