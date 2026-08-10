USE MarcusStoreDB;
GO
SET XACT_ABORT ON;
GO

-- Marcus thêm: migration nhỏ cho Gói C. Không xóa báo cáo cũ; bản cũ có
-- fingerprint NULL sẽ chỉ còn dùng cho mục "báo cáo gần nhất".
IF COL_LENGTH('dbo.AI_Analytics_Reports', 'data_fingerprint') IS NULL
BEGIN
    ALTER TABLE dbo.AI_Analytics_Reports ADD data_fingerprint VARCHAR(64) NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.AI_Analytics_Reports')
      AND name = 'IX_AIAnalyticsReports_Fingerprint'
)
BEGIN
    CREATE INDEX IX_AIAnalyticsReports_Fingerprint
        ON dbo.AI_Analytics_Reports(
            from_date, to_date, model_name, data_fingerprint, generated_at DESC
        );
END;
GO

SELECT
    COL_LENGTH('dbo.AI_Analytics_Reports', 'data_fingerprint') AS fingerprint_column_bytes,
    COUNT_BIG(*) AS stored_reports,
    SUM(CASE WHEN data_fingerprint IS NULL THEN 1 ELSE 0 END) AS legacy_reports
FROM dbo.AI_Analytics_Reports;
GO
