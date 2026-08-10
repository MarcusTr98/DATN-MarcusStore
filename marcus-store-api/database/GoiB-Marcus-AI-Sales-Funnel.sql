USE MarcusStoreDB;
GO
SET XACT_ABORT ON;
GO

/*
    Marcus thêm Gói B:
    - Không tạo bảng mới.
    - Không lưu nội dung chat, userId, IP hoặc dữ liệu thanh toán.
    - Chỉ thêm khóa chống hai feedback cho cùng một adviceId.
    - Journey AI -> Checkout -> Order -> Payment dùng session_id đã có.
*/

IF OBJECT_ID(N'dbo.AI_Usage_Events', N'U') IS NULL
    THROW 51300, N'Thiếu bảng AI_Usage_Events. Hãy chạy master SQL trước.', 1;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.AI_Usage_Events') AND name='UX_AIUsageEvents_OneFeedback')
    DROP INDEX UX_AIUsageEvents_OneFeedback ON dbo.AI_Usage_Events;
IF COL_LENGTH('dbo.AI_Usage_Events','feedback_advice_key') IS NOT NULL
    ALTER TABLE dbo.AI_Usage_Events DROP COLUMN feedback_advice_key;
GO

-- Marcus sửa dữ liệu cũ: nếu trước đây cùng advice có nhiều feedback thì giữ
-- bản ghi đầu tiên trước khi tạo unique index.
;WITH DuplicateFeedback AS (
    SELECT event_id,
           ROW_NUMBER() OVER (PARTITION BY advice_id ORDER BY created_at, event_id) AS row_number
    FROM dbo.AI_Usage_Events
    WHERE event_type IN ('FEEDBACK_HELPFUL', 'FEEDBACK_NOT_HELPFUL')
      AND advice_id IS NOT NULL
)
DELETE FROM DuplicateFeedback WHERE row_number > 1;
GO

CREATE OR ALTER TRIGGER dbo.TR_AIUsageEvents_OneFeedback
ON dbo.AI_Usage_Events AFTER INSERT,UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT usage_event.advice_id FROM dbo.AI_Usage_Events usage_event
        WHERE usage_event.advice_id IS NOT NULL
          AND usage_event.event_type IN ('FEEDBACK_HELPFUL','FEEDBACK_NOT_HELPFUL')
          AND usage_event.advice_id IN (SELECT advice_id FROM inserted WHERE advice_id IS NOT NULL)
        GROUP BY usage_event.advice_id HAVING COUNT_BIG(*)>1
    ) THROW 51301,N'Mỗi câu tư vấn chỉ được ghi nhận một feedback.',1;
END;
GO

SELECT
    CASE WHEN OBJECT_ID('dbo.TR_AIUsageEvents_OneFeedback','TR') IS NOT NULL
         THEN N'OK' ELSE N'MISSING' END AS feedback_guard;
GO
