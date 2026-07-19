-- Bảo vệ idempotency cho transaction mới mà không làm hỏng dữ liệu lịch sử.
-- Script có thể chạy lại nhiều lần trên SQL Server.

IF COL_LENGTH('dbo.Order_Transactions', 'idempotency_key') IS NULL
BEGIN
    ALTER TABLE dbo.Order_Transactions
        ADD idempotency_key VARCHAR(150) NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_OrderTransactions_IdempotencyKey'
      AND object_id = OBJECT_ID('dbo.Order_Transactions')
)
BEGIN
    CREATE UNIQUE INDEX UX_OrderTransactions_IdempotencyKey
        ON dbo.Order_Transactions(idempotency_key)
        WHERE idempotency_key IS NOT NULL;
END;
GO
