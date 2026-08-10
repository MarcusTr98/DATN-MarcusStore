/* MARCUS P2 - AI, ANALYTICS VÀ HÀNH VI SQL SERVER
   Dừng backend -> backup MarcusStoreDB -> Ctrl+A Execute toàn bộ. */
USE MarcusStoreDB;
GO
SET XACT_ABORT ON;
GO

IF OBJECT_ID(N'dbo.AI_Usage_Events', N'U') IS NULL
    THROW 51200, N'Thiếu AI_Usage_Events. Hãy chạy MarcusUpdateHeThong0908/master SQL trước.', 1;
GO

-- Marcus thêm: nối feedback với đúng câu trả lời; không lưu nội dung hội thoại.
IF COL_LENGTH('dbo.AI_Usage_Events', 'advice_id') IS NULL
    ALTER TABLE dbo.AI_Usage_Events ADD advice_id VARCHAR(36) NULL;
GO

-- Marcus sửa: bỏ mọi check cũ trên event_type, kể cả database thành viên dùng tên tự sinh.
DECLARE @dropEventChecks NVARCHAR(MAX);
SELECT @dropEventChecks = STRING_AGG(
    N'ALTER TABLE dbo.AI_Usage_Events DROP CONSTRAINT ' + QUOTENAME(name), N';')
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'dbo.AI_Usage_Events')
  AND definition LIKE '%event_type%';
IF @dropEventChecks IS NOT NULL EXEC sys.sp_executesql @dropEventChecks;
ALTER TABLE dbo.AI_Usage_Events ADD CONSTRAINT CK_AIUsageEvents_Type CHECK (
    event_type IN ('CHAT_SUCCESS', 'CHAT_RESPONSE', 'CHAT_FAILED', 'PRODUCT_CLICK',
                   'FEEDBACK_HELPFUL', 'FEEDBACK_NOT_HELPFUL')
);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_AIUsageEvents_Advice' AND object_id=OBJECT_ID('dbo.AI_Usage_Events'))
    CREATE INDEX IX_AIUsageEvents_Advice ON dbo.AI_Usage_Events(advice_id, created_at DESC)
    WHERE advice_id IS NOT NULL;
GO

-- Marcus sửa: SQL Server không cho filtered index lọc trên computed column.
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_AIUsageEvents_OneFeedback' AND object_id=OBJECT_ID('dbo.AI_Usage_Events'))
    DROP INDEX UX_AIUsageEvents_OneFeedback ON dbo.AI_Usage_Events;
IF COL_LENGTH('dbo.AI_Usage_Events', 'feedback_advice_key') IS NOT NULL
    ALTER TABLE dbo.AI_Usage_Events DROP COLUMN feedback_advice_key;
GO
;WITH duplicated AS (
    SELECT event_id, ROW_NUMBER() OVER (PARTITION BY advice_id ORDER BY created_at, event_id) AS row_number
    FROM dbo.AI_Usage_Events
    WHERE advice_id IS NOT NULL
      AND event_type IN ('FEEDBACK_HELPFUL','FEEDBACK_NOT_HELPFUL')
)
DELETE FROM duplicated WHERE row_number > 1;
GO
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
    ) THROW 51202, N'Mỗi câu tư vấn chỉ được ghi nhận một feedback.', 1;
END;
GO

-- Marcus thêm: cache AI Analytics tự hết hiệu lực khi dữ liệu tổng hợp thay đổi.
IF OBJECT_ID(N'dbo.AI_Analytics_Reports', N'U') IS NULL
    THROW 51201, N'Thiếu AI_Analytics_Reports. Hãy chạy MarcusUpdateHeThong0908/master SQL trước.', 1;
GO
IF COL_LENGTH('dbo.AI_Analytics_Reports', 'data_fingerprint') IS NULL
    ALTER TABLE dbo.AI_Analytics_Reports ADD data_fingerprint VARCHAR(64) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_AIAnalyticsReports_Fingerprint' AND object_id=OBJECT_ID('dbo.AI_Analytics_Reports'))
    CREATE INDEX IX_AIAnalyticsReports_Fingerprint
        ON dbo.AI_Analytics_Reports(from_date,to_date,model_name,data_fingerprint,generated_at DESC);
GO

-- Marcus thêm: event funnel tối thiểu, UUID ẩn danh; không có cột nội dung/IP/userId.
IF OBJECT_ID(N'dbo.Customer_Behavior_Events', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Customer_Behavior_Events (
        event_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CustomerBehaviorEvents PRIMARY KEY,
        event_type VARCHAR(30) NOT NULL,
        session_id VARCHAR(36) NULL,
        product_id INT NULL,
        order_id INT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_CustomerBehaviorEvents_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_CustomerBehaviorEvents_Type CHECK (
            event_type IN ('PRODUCT_VIEW','CHECKOUT_STARTED','ORDER_CREATED','PAYMENT_SUCCESS','AI_QUESTION','AI_PRODUCT_CLICK')
        )
    );
    CREATE INDEX IX_CustomerBehaviorEvents_CreatedType
        ON dbo.Customer_Behavior_Events(created_at DESC, event_type);
    CREATE INDEX IX_CustomerBehaviorEvents_Session
        ON dbo.Customer_Behavior_Events(session_id, created_at DESC)
        WHERE session_id IS NOT NULL;
    CREATE INDEX IX_CustomerBehaviorEvents_Order
        ON dbo.Customer_Behavior_Events(order_id, created_at DESC)
        WHERE order_id IS NOT NULL;
END;
GO

SELECT
    CASE WHEN COL_LENGTH('dbo.AI_Usage_Events','advice_id') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS ai_feedback_schema,
    CASE WHEN OBJECT_ID('dbo.TR_AIUsageEvents_OneFeedback','TR') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS ai_feedback_lock,
    CASE WHEN COL_LENGTH('dbo.AI_Analytics_Reports','data_fingerprint') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS analytics_fingerprint,
    CASE WHEN OBJECT_ID(N'dbo.Customer_Behavior_Events', N'U') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS behavior_schema,
    CASE WHEN COL_LENGTH('dbo.Customer_Behavior_Events','session_id') IS NOT NULL
          AND COL_LENGTH('dbo.Customer_Behavior_Events','event_type') IS NOT NULL
         THEN N'DAT - Khởi động backend và chạy UAT P2.'
         ELSE N'CHUA DAT - Gửi Result cho Marcus.' END AS overall_result;
GO
