-- Marcus thêm: chạy script này một lần trước khi khởi động backend sau khi pull nhánh.
-- Script có thể chạy lại an toàn trên SQL Server.
IF COL_LENGTH('dbo.Orders', 'fulfillment_method') IS NULL
BEGIN
    ALTER TABLE dbo.Orders
        ADD fulfillment_method VARCHAR(30) NOT NULL
            CONSTRAINT DF_Orders_FulfillmentMethod DEFAULT 'DELIVERY';
END;

-- kiểm tra 2 phương thức nhận hàng
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
