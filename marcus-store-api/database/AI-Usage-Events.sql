USE MarcusStoreDB;
GO

/* Marcus thêm: đo hiệu quả Marcus AI mà không lưu nội dung chat, IP hay thông tin cá nhân. */
IF OBJECT_ID('dbo.AI_Usage_Events', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AI_Usage_Events (
        event_id BIGINT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_AI_Usage_Events PRIMARY KEY,
        session_id VARCHAR(36) NOT NULL,
        event_type VARCHAR(30) NOT NULL,
        product_id INT NULL,
        response_time_ms INT NULL,
        created_at DATETIME2 NOT NULL
            CONSTRAINT DF_AIUsageEvents_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_AIUsageEvents_Type CHECK (
            event_type IN ('CHAT_SUCCESS', 'CHAT_FAILED', 'PRODUCT_CLICK')
        ),
        CONSTRAINT CK_AIUsageEvents_ResponseTime CHECK (
            response_time_ms IS NULL OR response_time_ms BETWEEN 0 AND 120000
        ),
        CONSTRAINT FK_AIUsageEvents_Product
            FOREIGN KEY (product_id) REFERENCES dbo.Products(product_id)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_AIUsageEvents_CreatedType'
      AND object_id = OBJECT_ID('dbo.AI_Usage_Events')
)
BEGIN
    CREATE INDEX IX_AIUsageEvents_CreatedType
        ON dbo.AI_Usage_Events(created_at DESC, event_type);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_AIUsageEvents_Session'
      AND object_id = OBJECT_ID('dbo.AI_Usage_Events')
)
BEGIN
    CREATE INDEX IX_AIUsageEvents_Session
        ON dbo.AI_Usage_Events(session_id, created_at DESC);
END;
GO

PRINT N'Marcus: bảng thống kê sử dụng AI an toàn đã sẵn sàng.';
GO
