-- Marcus thêm: chạy một lần trên database hiện tại trước khi khởi động backend.
IF COL_LENGTH('dbo.Orders', 'fulfillment_method') IS NULL
BEGIN
    ALTER TABLE dbo.Orders
        ADD fulfillment_method VARCHAR(30) NOT NULL
            CONSTRAINT DF_Orders_FulfillmentMethod DEFAULT 'DELIVERY';
END;

-- Marcus thêm: chỉ chấp nhận hai phương thức nhận hàng của MVP.
IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_Orders_FulfillmentMethod'
)
BEGIN
    ALTER TABLE dbo.Orders
        ADD CONSTRAINT CK_Orders_FulfillmentMethod
            CHECK (fulfillment_method IN ('DELIVERY', 'STORE_PICKUP'));
END;
