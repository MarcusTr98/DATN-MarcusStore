/* MARCUS CHẠY RIÊNG - SỬA P2 SAU LỖI MSG 10609
   Database hiện tại đã có các bảng P2. Tắt backend -> Ctrl+A -> Execute toàn bộ. */
USE MarcusStoreDB;
GO
SET XACT_ABORT ON;
GO

IF OBJECT_ID('dbo.AI_Usage_Events','U') IS NULL
    THROW 51400, N'Thiếu AI_Usage_Events. Hãy chạy MarcusUpdateHeThong0908.sql.', 1;
IF OBJECT_ID('dbo.AI_Analytics_Reports','U') IS NULL
    THROW 51401, N'Thiếu AI_Analytics_Reports. Hãy chạy MarcusUpdateHeThong0908.sql.', 1;
GO

-- Marcus sửa: dọn phần computed column của lần chạy lỗi trước.
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_AIUsageEvents_OneFeedback' AND object_id=OBJECT_ID('dbo.AI_Usage_Events'))
    DROP INDEX UX_AIUsageEvents_OneFeedback ON dbo.AI_Usage_Events;
IF COL_LENGTH('dbo.AI_Usage_Events','feedback_advice_key') IS NOT NULL
    ALTER TABLE dbo.AI_Usage_Events DROP COLUMN feedback_advice_key;
GO

-- Marcus sửa: giữ feedback đầu tiên nếu dữ liệu cũ đã bị ghi lặp.
;WITH duplicated AS (
    SELECT event_id, ROW_NUMBER() OVER (PARTITION BY advice_id ORDER BY created_at,event_id) AS row_number
    FROM dbo.AI_Usage_Events
    WHERE advice_id IS NOT NULL
      AND event_type IN ('FEEDBACK_HELPFUL','FEEDBACK_NOT_HELPFUL')
)
DELETE FROM duplicated WHERE row_number > 1;
GO

-- Marcus thêm: trigger chặn cả double click và hai loại feedback cho cùng advice.
CREATE OR ALTER TRIGGER dbo.TR_AIUsageEvents_OneFeedback
ON dbo.AI_Usage_Events AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT usage_event.advice_id
        FROM dbo.AI_Usage_Events usage_event
        WHERE usage_event.advice_id IS NOT NULL
          AND usage_event.event_type IN ('FEEDBACK_HELPFUL','FEEDBACK_NOT_HELPFUL')
          AND usage_event.advice_id IN (SELECT advice_id FROM inserted WHERE advice_id IS NOT NULL)
        GROUP BY usage_event.advice_id HAVING COUNT_BIG(*) > 1
    ) THROW 51402, N'Mỗi câu tư vấn chỉ được ghi nhận một feedback.', 1;
END;
GO

-- Marcus thêm: fingerprint làm cache Analytics hết hiệu lực khi dữ liệu đổi.
IF COL_LENGTH('dbo.AI_Analytics_Reports','data_fingerprint') IS NULL
    ALTER TABLE dbo.AI_Analytics_Reports ADD data_fingerprint VARCHAR(64) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_AIAnalyticsReports_Fingerprint' AND object_id=OBJECT_ID('dbo.AI_Analytics_Reports'))
    CREATE INDEX IX_AIAnalyticsReports_Fingerprint
        ON dbo.AI_Analytics_Reports(from_date,to_date,model_name,data_fingerprint,generated_at DESC);
GO

SELECT
    CASE WHEN OBJECT_ID('dbo.TR_AIUsageEvents_OneFeedback','TR') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS feedback_lock,
    CASE WHEN COL_LENGTH('dbo.AI_Analytics_Reports','data_fingerprint') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS analytics_fingerprint,
    CASE WHEN OBJECT_ID('dbo.Customer_Behavior_Events','U') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS behavior_events,
    CASE WHEN OBJECT_ID('dbo.TR_AIUsageEvents_OneFeedback','TR') IS NOT NULL
           AND COL_LENGTH('dbo.AI_Analytics_Reports','data_fingerprint') IS NOT NULL
         THEN N'DAT - Khởi động backend.' ELSE N'CHUA DAT - Gửi Result cho Marcus.' END AS overall_result;
GO
