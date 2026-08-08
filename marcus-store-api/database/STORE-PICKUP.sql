USE MarcusStoreDB;
GO

/* Marcus sửa: fulfillment_method đã chuyển sang Order_Shipping_Details.
   File được giữ để thành viên dùng tài liệu cũ nhận đúng hướng dẫn, không vô
   tình thêm cột trở lại Orders. */
IF OBJECT_ID(N'dbo.Order_Shipping_Details', N'U') IS NULL
    THROW 51020, N'Hãy chạy ORDER-NORMALIZE-SHIPPING-CANCELLATION.sql để cấu hình nhận tại cửa hàng.', 1;

SELECT
    fulfillment_method,
    COUNT(*) AS total_orders
FROM dbo.Order_Shipping_Details
GROUP BY fulfillment_method;
GO
