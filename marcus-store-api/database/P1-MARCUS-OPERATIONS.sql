/*
  MARCUS - Thêm 09/08
  Database đang có dữ liệu: dừng backend, backup MarcusStoreDB, Ctrl+A và Execute
  toàn bộ file đúng một lần. File có thể chạy lại an toàn nếu lần đầu bị gián đoạn.
*/
USE MarcusStoreDB;
GO
SET XACT_ABORT ON;
GO

-- Marcus thêm metadata/idempotency cho chuông Admin và khách.
IF COL_LENGTH('dbo.Admin_Notifications', 'event_key') IS NULL ALTER TABLE dbo.Admin_Notifications ADD event_key VARCHAR(180) NULL;
IF COL_LENGTH('dbo.Admin_Notifications', 'category') IS NULL ALTER TABLE dbo.Admin_Notifications ADD category VARCHAR(20) NOT NULL CONSTRAINT DF_AdminNotif_Category DEFAULT 'INFO' WITH VALUES;
IF COL_LENGTH('dbo.Admin_Notifications', 'icon') IS NULL ALTER TABLE dbo.Admin_Notifications ADD icon VARCHAR(80) NULL;
IF COL_LENGTH('dbo.Admin_Notifications', 'deep_link') IS NULL ALTER TABLE dbo.Admin_Notifications ADD deep_link VARCHAR(300) NULL;
IF COL_LENGTH('dbo.Admin_Notifications', 'expires_at') IS NULL ALTER TABLE dbo.Admin_Notifications ADD expires_at DATETIME2 NULL;
GO
-- Marcus sửa: SQL Server cần biên dịch batch mới sau khi ALTER thì mới nhận cột event_key.
UPDATE dbo.Admin_Notifications SET event_key = CONCAT('LEGACY_ADMIN:', id) WHERE event_key IS NULL;
UPDATE dbo.Admin_Notifications SET expires_at = DATEADD(DAY, 90, COALESCE(created_at, GETDATE())) WHERE expires_at IS NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_AdminNotifications_EventKey' AND object_id=OBJECT_ID('dbo.Admin_Notifications'))
    CREATE UNIQUE INDEX UX_AdminNotifications_EventKey ON dbo.Admin_Notifications(event_key) WHERE event_key IS NOT NULL;
GO

IF COL_LENGTH('dbo.User_Notifications', 'event_key') IS NULL ALTER TABLE dbo.User_Notifications ADD event_key VARCHAR(180) NULL;
IF COL_LENGTH('dbo.User_Notifications', 'category') IS NULL ALTER TABLE dbo.User_Notifications ADD category VARCHAR(20) NOT NULL CONSTRAINT DF_UserNotif_Category DEFAULT 'INFO' WITH VALUES;
IF COL_LENGTH('dbo.User_Notifications', 'icon') IS NULL ALTER TABLE dbo.User_Notifications ADD icon VARCHAR(80) NULL;
IF COL_LENGTH('dbo.User_Notifications', 'deep_link') IS NULL ALTER TABLE dbo.User_Notifications ADD deep_link VARCHAR(300) NULL;
IF COL_LENGTH('dbo.User_Notifications', 'expires_at') IS NULL ALTER TABLE dbo.User_Notifications ADD expires_at DATETIME2 NULL;
GO
-- Marcus sửa: tách batch tương tự cho chuông khách hàng.
UPDATE dbo.User_Notifications SET event_key = CONCAT('LEGACY_USER:', id) WHERE event_key IS NULL;
UPDATE dbo.User_Notifications SET expires_at = DATEADD(DAY, 120, COALESCE(created_at, GETDATE())) WHERE expires_at IS NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_UserNotifications_EventKey' AND object_id=OBJECT_ID('dbo.User_Notifications'))
    CREATE UNIQUE INDEX UX_UserNotifications_EventKey ON dbo.User_Notifications(event_key) WHERE event_key IS NOT NULL;
GO

-- Marcus thêm audit đối soát.
IF COL_LENGTH('dbo.Order_Transactions', 'reconciled_by') IS NULL ALTER TABLE dbo.Order_Transactions ADD reconciled_by VARCHAR(100) NULL;
IF COL_LENGTH('dbo.Order_Transactions', 'reconciled_at') IS NULL ALTER TABLE dbo.Order_Transactions ADD reconciled_at DATETIME2 NULL;
GO

-- Marcus thêm người cập nhật System Settings.
IF COL_LENGTH('dbo.System_Settings', 'updated_by') IS NULL ALTER TABLE dbo.System_Settings ADD updated_by VARCHAR(100) NULL;
GO

-- Marcus chuẩn hóa vòng đời liên hệ.
IF COL_LENGTH('dbo.Contact_Requests', 'handled_by') IS NULL ALTER TABLE dbo.Contact_Requests ADD handled_by VARCHAR(100) NULL;
IF COL_LENGTH('dbo.Contact_Requests', 'processing_started_at') IS NULL ALTER TABLE dbo.Contact_Requests ADD processing_started_at DATETIME2 NULL;
IF COL_LENGTH('dbo.Contact_Requests', 'resolved_at') IS NULL ALTER TABLE dbo.Contact_Requests ADD resolved_at DATETIME2 NULL;
UPDATE dbo.Contact_Requests SET status='NEW' WHERE status='PENDING' OR status IS NULL;
GO

-- Marcus thêm metadata Live Chat; KHÔNG có cột nội dung chat hoặc thông tin khách.
IF OBJECT_ID('dbo.Chat_Session_Metrics', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Chat_Session_Metrics (
        session_id VARCHAR(36) NOT NULL CONSTRAINT PK_ChatSessionMetrics PRIMARY KEY,
        customer_hash VARCHAR(64) NOT NULL,
        started_at DATETIME2 NOT NULL,
        claimed_at DATETIME2 NULL,
        first_response_at DATETIME2 NULL,
        ended_at DATETIME2 NULL,
        status VARCHAR(20) NOT NULL,
        answered BIT NOT NULL CONSTRAINT DF_ChatSessionMetrics_Answered DEFAULT 0,
        closed_by VARCHAR(20) NULL
    );
    CREATE INDEX IX_ChatSessionMetrics_StartedAt ON dbo.Chat_Session_Metrics(started_at DESC);
END;
GO

SELECT
    CASE WHEN COL_LENGTH('dbo.Admin_Notifications','event_key') IS NOT NULL THEN 'OK' ELSE 'MISSING' END notification_schema,
    CASE WHEN COL_LENGTH('dbo.Order_Transactions','reconciled_at') IS NOT NULL THEN 'OK' ELSE 'MISSING' END finance_schema,
    CASE WHEN COL_LENGTH('dbo.System_Settings','updated_by') IS NOT NULL THEN 'OK' ELSE 'MISSING' END settings_schema,
    CASE WHEN COL_LENGTH('dbo.Contact_Requests','handled_by') IS NOT NULL THEN 'OK' ELSE 'MISSING' END contact_schema,
    CASE WHEN OBJECT_ID('dbo.Chat_Session_Metrics','U') IS NOT NULL THEN 'OK' ELSE 'MISSING' END chat_schema;
GO
