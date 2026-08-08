USE MarcusStoreDB;
GO

/* Marcus sửa: dữ liệu hủy đã tách khỏi Orders. Chạy file normalize trước để
   giữ đúng một nguồn dữ liệu cấu trúc. */
IF OBJECT_ID(N'dbo.Order_Cancellations', N'U') IS NULL
    THROW 51010, N'Chưa có Order_Cancellations. Hãy chạy ORDER-NORMALIZE-SHIPPING-CANCELLATION.sql trước.', 1;
GO

/* Marcus thêm: bổ sung bản ghi tối thiểu cho đơn hủy cũ chưa có dữ liệu cấu trúc. */
INSERT INTO dbo.Order_Cancellations (order_id, reason_code, actor_type, detail, cancelled_at)
SELECT o.order_id, 'SYSTEM_OTHER', 'SYSTEM', NULL, COALESCE(o.updated_at, o.created_at, GETDATE())
FROM dbo.Orders o
LEFT JOIN dbo.Order_Cancellations cancellation ON cancellation.order_id = o.order_id
WHERE o.order_status = 'CANCELLED'
  AND cancellation.order_id IS NULL;
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
    SUM(CASE WHEN cancellation.reason_code IS NULL THEN 1 ELSE 0 END) AS missing_reason_code,
    SUM(CASE WHEN cancellation.actor_type IS NULL THEN 1 ELSE 0 END) AS missing_actor
FROM dbo.Orders orders
LEFT JOIN dbo.Order_Cancellations cancellation ON cancellation.order_id = orders.order_id
WHERE orders.order_status = 'CANCELLED';
GO
