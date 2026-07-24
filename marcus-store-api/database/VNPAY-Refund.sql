-- Schema cho luồng hoàn tiền VNPAY. Script có thể chạy lại nhiều lần.

IF COL_LENGTH('dbo.Orders', 'customer_shipping_fee') IS NULL
BEGIN
    ALTER TABLE dbo.Orders ADD customer_shipping_fee DECIMAL(18,2) NULL;
END;
GO

IF COL_LENGTH('dbo.Order_Transactions', 'provider_transaction_date') IS NULL
BEGIN
    ALTER TABLE dbo.Order_Transactions ADD provider_transaction_date VARCHAR(14) NULL;
END;
GO

IF OBJECT_ID('dbo.Refund_Requests', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Refund_Requests (
        refund_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        order_id INT NOT NULL,
        payment_transaction_id INT NOT NULL,
        refund_transaction_id INT NULL,
        request_code VARCHAR(32) NOT NULL,
        idempotency_key VARCHAR(150) NOT NULL,
        amount DECIMAL(18,2) NOT NULL,
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
        CONSTRAINT FK_RefundRequests_Order FOREIGN KEY (order_id) REFERENCES dbo.Orders(order_id),
        CONSTRAINT FK_RefundRequests_PaymentTransaction FOREIGN KEY (payment_transaction_id)
            REFERENCES dbo.Order_Transactions(transaction_id),
        CONSTRAINT FK_RefundRequests_RefundTransaction FOREIGN KEY (refund_transaction_id)
            REFERENCES dbo.Order_Transactions(transaction_id),
        CONSTRAINT FK_RefundRequests_RequestedBy FOREIGN KEY (requested_by) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_RefundRequests_ApprovedBy FOREIGN KEY (approved_by) REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_RefundRequests_Amount CHECK (amount > 0),
        CONSTRAINT CK_RefundRequests_Retry CHECK (retry_count >= 0 AND max_retries > 0),
        CONSTRAINT UX_RefundRequests_RequestCode UNIQUE (request_code),
        CONSTRAINT UX_RefundRequests_IdempotencyKey UNIQUE (idempotency_key)
    );

    CREATE INDEX IX_RefundRequests_StatusRetry
        ON dbo.Refund_Requests(status, next_retry_at);
END;
GO

-- Marcus thêm dữ liệu điều phối QueryDR và audit xác nhận Sandbox sau khi
-- Refund_Requests chắc chắn đã tồn tại.
IF COL_LENGTH('dbo.Refund_Requests', 'reconciliation_attempts') IS NULL
    ALTER TABLE dbo.Refund_Requests ADD reconciliation_attempts INT NOT NULL
        CONSTRAINT DF_RefundRequests_ReconciliationAttempts DEFAULT 0;
GO
IF COL_LENGTH('dbo.Refund_Requests', 'last_reconciled_at') IS NULL
    ALTER TABLE dbo.Refund_Requests ADD last_reconciled_at DATETIME2 NULL;
GO
IF COL_LENGTH('dbo.Refund_Requests', 'next_reconciliation_at') IS NULL
    ALTER TABLE dbo.Refund_Requests ADD next_reconciliation_at DATETIME2 NULL;
GO
IF COL_LENGTH('dbo.Refund_Requests', 'last_reconciliation_message') IS NULL
    ALTER TABLE dbo.Refund_Requests ADD last_reconciliation_message NVARCHAR(500) NULL;
GO
IF COL_LENGTH('dbo.Refund_Requests', 'manually_confirmed_by') IS NULL
BEGIN
    ALTER TABLE dbo.Refund_Requests ADD manually_confirmed_by INT NULL;
    ALTER TABLE dbo.Refund_Requests ADD CONSTRAINT FK_RefundRequests_ManuallyConfirmedBy
        FOREIGN KEY (manually_confirmed_by) REFERENCES dbo.Users(user_id);
END;
GO
IF COL_LENGTH('dbo.Refund_Requests', 'manually_confirmed_at') IS NULL
    ALTER TABLE dbo.Refund_Requests ADD manually_confirmed_at DATETIME2 NULL;
GO
IF COL_LENGTH('dbo.Refund_Requests', 'manual_confirmation_note') IS NULL
    ALTER TABLE dbo.Refund_Requests ADD manual_confirmation_note NVARCHAR(500) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_RefundRequests_Reconciliation'
    AND object_id = OBJECT_ID('dbo.Refund_Requests'))
    CREATE INDEX IX_RefundRequests_Reconciliation
        ON dbo.Refund_Requests(status, next_reconciliation_at);
GO
