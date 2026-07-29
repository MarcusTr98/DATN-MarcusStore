USE MarcusStoreDB;
GO

/* Marcus thêm: lưu lại bản tin AI để xem lại không tốn quota Gemini. */
IF OBJECT_ID('dbo.AI_Analytics_Reports', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AI_Analytics_Reports (
        report_id BIGINT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_AI_Analytics_Reports PRIMARY KEY,
        from_date DATE NOT NULL,
        to_date DATE NOT NULL,
        report_json NVARCHAR(MAX) NOT NULL,
        model_name VARCHAR(100) NOT NULL,
        generated_at DATETIME2 NOT NULL
            CONSTRAINT DF_AIAnalyticsReports_GeneratedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_AIAnalyticsReports_Period CHECK (from_date <= to_date),
        CONSTRAINT CK_AIAnalyticsReports_Json CHECK (ISJSON(report_json) = 1)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_AIAnalyticsReports_PeriodGenerated'
      AND object_id = OBJECT_ID('dbo.AI_Analytics_Reports')
)
BEGIN
    CREATE INDEX IX_AIAnalyticsReports_PeriodGenerated
        ON dbo.AI_Analytics_Reports(from_date, to_date, generated_at DESC);
END;
GO

PRINT N'Marcus: bảng lưu báo cáo AI Analytics đã sẵn sàng.';
GO
