USE MarcusStoreDB;
GO

/* Marcus thêm: dữ liệu hủy có cấu trúc. Script an toàn khi chạy lại. */
IF COL_LENGTH('dbo.Orders', 'cancellation_reason_code') IS NULL
    ALTER TABLE dbo.Orders ADD cancellation_reason_code VARCHAR(50) NULL;
GO
IF COL_LENGTH('dbo.Orders', 'cancellation_actor') IS NULL
    ALTER TABLE dbo.Orders ADD cancellation_actor VARCHAR(20) NULL;
GO
IF COL_LENGTH('dbo.Orders', 'cancelled_at') IS NULL
    ALTER TABLE dbo.Orders ADD cancelled_at DATETIME2 NULL;
GO

/* Marcus thêm: điền dữ liệu cũ ở mức an toàn, không sửa ghi chú/lịch sử đơn. */
UPDATE dbo.Orders
SET cancellation_reason_code = 'SYSTEM_OTHER',
    cancellation_actor = 'SYSTEM',
    cancelled_at = COALESCE(updated_at, created_at)
WHERE order_status = 'CANCELLED'
  AND cancellation_reason_code IS NULL;
GO

/* Marcus thêm: chống trùng giá trị trong cùng một thuộc tính sau khi đã kiểm tra dữ liệu. */
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_AttributeValues_Attribute_Value'
      AND object_id = OBJECT_ID('dbo.Attribute_Values')
)
AND NOT EXISTS (
    SELECT 1
    FROM dbo.Attribute_Values
    GROUP BY attribute_id, value_string
    HAVING COUNT(*) > 1
)
    CREATE UNIQUE INDEX UX_AttributeValues_Attribute_Value
        ON dbo.Attribute_Values(attribute_id, value_string);
GO

SELECT
    COUNT(*) AS cancelled_orders,
    SUM(CASE WHEN cancellation_reason_code IS NULL THEN 1 ELSE 0 END) AS missing_reason_code,
    SUM(CASE WHEN cancellation_actor IS NULL THEN 1 ELSE 0 END) AS missing_actor
FROM dbo.Orders
WHERE order_status = 'CANCELLED';
GO
