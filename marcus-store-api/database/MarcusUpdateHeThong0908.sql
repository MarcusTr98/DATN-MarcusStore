/*
  CÁCH CHẠY:
  1. Dừng backend và backup MarcusStoreDB.
  2. Mở đúng file này trong SSMS.
  3. Ctrl+A -> Execute TOÀN BỘ đúng một lần.
  4. File có thể chạy lại an toàn nếu lần đầu bị gián đoạn.
  5. Chỉ chạy backend mới khi Result cuối có overall_result = DAT.
*/
USE MarcusStoreDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

-- ============================================================
-- 0. KIỂM TRA CÁC BẢNG NỀN CẦN THIẾT
-- ============================================================
IF OBJECT_ID(N'dbo.Orders', N'U') IS NULL
    THROW 51100, N'Không tìm thấy dbo.Orders trong MarcusStoreDB.', 1;
IF OBJECT_ID(N'dbo.Admin_Notifications', N'U') IS NULL
    THROW 51101, N'Thiếu dbo.Admin_Notifications. Hãy chạy SQL nền của dev trước.', 1;
IF OBJECT_ID(N'dbo.User_Notifications', N'U') IS NULL
    THROW 51102, N'Thiếu dbo.User_Notifications. Hãy chạy SQL nền của dev trước.', 1;
IF OBJECT_ID(N'dbo.Order_Transactions', N'U') IS NULL
    THROW 51103, N'Thiếu dbo.Order_Transactions. Hãy chạy SQL nền của dev trước.', 1;
IF OBJECT_ID(N'dbo.System_Settings', N'U') IS NULL
    THROW 51104, N'Thiếu dbo.System_Settings. Hãy chạy SQL nền của dev trước.', 1;
IF OBJECT_ID(N'dbo.Contact_Requests', N'U') IS NULL
    THROW 51105, N'Thiếu dbo.Contact_Requests. Hãy chạy SQL nền của dev trước.', 1;
IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
    THROW 51106, N'Thiếu dbo.Users. Hãy chạy SQL nền của dev trước.', 1;
GO

-- ============================================================
-- P0.1 CHECKOUT IDEMPOTENCY
-- ============================================================
-- Marcus thêm: database dev cũ chưa có cột thì bổ sung trực tiếp vào Orders.
IF COL_LENGTH('dbo.Orders', 'checkout_request_id') IS NULL
    ALTER TABLE dbo.Orders ADD checkout_request_id VARCHAR(64) NULL;
IF COL_LENGTH('dbo.Orders', 'payment_date') IS NULL
    ALTER TABLE dbo.Orders ADD payment_date DATETIME2 NULL;
IF COL_LENGTH('dbo.Orders', 'is_hidden') IS NULL
    ALTER TABLE dbo.Orders ADD is_hidden BIT NOT NULL
        CONSTRAINT DF_Orders_IsHidden_MarcusUpdate DEFAULT 0 WITH VALUES;
GO

-- Marcus thêm: chỉ tạo unique index khi dữ liệu hiện tại không bị trùng.
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_Orders_CheckoutRequestId'
      AND object_id = OBJECT_ID(N'dbo.Orders')
)
AND NOT EXISTS (
    SELECT checkout_request_id
    FROM dbo.Orders
    WHERE checkout_request_id IS NOT NULL
    GROUP BY checkout_request_id
    HAVING COUNT(*) > 1
)
    CREATE UNIQUE INDEX UX_Orders_CheckoutRequestId
        ON dbo.Orders(checkout_request_id)
        WHERE checkout_request_id IS NOT NULL;
GO

-- ============================================================
-- P0.2 TÁCH THÔNG TIN GIAO NHẬN/GHN KHỎI ORDERS
-- ============================================================
IF OBJECT_ID(N'dbo.Order_Shipping_Details', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Order_Shipping_Details (
        order_id                 INT NOT NULL,
        fulfillment_method       VARCHAR(30) NOT NULL
            CONSTRAINT DF_OrderShipping_Fulfillment DEFAULT 'DELIVERY',
        shipping_fee             DECIMAL(18,2) NULL,
        shipping_subsidy         DECIMAL(18,2) NOT NULL
            CONSTRAINT DF_OrderShipping_Subsidy DEFAULT 0,
        customer_shipping_fee    DECIMAL(18,2) NULL,
        tracking_code            VARCHAR(100) NULL,
        to_district_id           INT NULL,
        to_ward_code             VARCHAR(20) NULL,
        delivery_note            NVARCHAR(500) NULL,
        ghn_integration_status   VARCHAR(30) NOT NULL
            CONSTRAINT DF_OrderShipping_GhnStatus DEFAULT 'NOT_REQUIRED',
        ghn_retry_count          INT NOT NULL
            CONSTRAINT DF_OrderShipping_GhnRetry DEFAULT 0,
        ghn_last_error           NVARCHAR(500) NULL,
        ghn_last_attempt_at      DATETIME2 NULL,
        CONSTRAINT PK_Order_Shipping_Details PRIMARY KEY (order_id),
        CONSTRAINT FK_OrderShipping_Orders FOREIGN KEY (order_id)
            REFERENCES dbo.Orders(order_id) ON DELETE CASCADE,
        CONSTRAINT CK_OrderShipping_Fulfillment
            CHECK (fulfillment_method IN ('DELIVERY', 'STORE_PICKUP')),
        CONSTRAINT CK_OrderShipping_Fees CHECK (
            (shipping_fee IS NULL OR shipping_fee >= 0)
            AND shipping_subsidy >= 0
            AND (customer_shipping_fee IS NULL OR customer_shipping_fee >= 0)
        ),
        CONSTRAINT CK_OrderShipping_GhnRetry CHECK (ghn_retry_count >= 0)
    );
END;
GO

/* Marcus thêm: dùng SQL động để câu lệnh không tham chiếu cột không tồn tại.
   Database dev cơ bản vẫn copy được phí ship, tracking, địa chỉ GHN và ghi chú.
   Các cột ghn_* chưa từng có sẽ nhận NOT_REQUIRED/0/NULL. */
DECLARE @fulfillment NVARCHAR(500) = CASE
    WHEN COL_LENGTH('dbo.Orders', 'fulfillment_method') IS NOT NULL
        THEN N'CASE WHEN UPPER(LTRIM(RTRIM(COALESCE(o.fulfillment_method, ''DELIVERY'')))) = ''STORE_PICKUP'' THEN ''STORE_PICKUP'' ELSE ''DELIVERY'' END'
    ELSE N'''DELIVERY'''
END;
DECLARE @shippingFee NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'shipping_fee') IS NOT NULL THEN N'o.shipping_fee' ELSE N'NULL' END;
DECLARE @shippingSubsidy NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'shipping_subsidy') IS NOT NULL THEN N'COALESCE(o.shipping_subsidy, 0)' ELSE N'0' END;
DECLARE @customerShippingFee NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'customer_shipping_fee') IS NOT NULL THEN N'o.customer_shipping_fee' ELSE N'NULL' END;
DECLARE @trackingCode NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'tracking_code') IS NOT NULL THEN N'o.tracking_code' ELSE N'NULL' END;
DECLARE @districtId NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'to_district_id') IS NOT NULL THEN N'o.to_district_id' ELSE N'NULL' END;
DECLARE @wardCode NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'to_ward_code') IS NOT NULL THEN N'o.to_ward_code' ELSE N'NULL' END;
DECLARE @deliveryNote NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'delivery_note') IS NOT NULL THEN N'o.delivery_note' ELSE N'NULL' END;
DECLARE @ghnStatus NVARCHAR(400) = CASE
    WHEN COL_LENGTH('dbo.Orders', 'ghn_integration_status') IS NOT NULL
        THEN N'COALESCE(NULLIF(UPPER(LTRIM(RTRIM(o.ghn_integration_status))), ''''), ''NOT_REQUIRED'')'
    WHEN COL_LENGTH('dbo.Orders', 'tracking_code') IS NOT NULL
        THEN N'CASE WHEN NULLIF(LTRIM(RTRIM(o.tracking_code)), '''') IS NOT NULL THEN ''CREATED'' ELSE ''NOT_REQUIRED'' END'
    ELSE N'''NOT_REQUIRED'''
END;
DECLARE @ghnRetry NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'ghn_retry_count') IS NOT NULL THEN N'COALESCE(o.ghn_retry_count, 0)' ELSE N'0' END;
DECLARE @ghnError NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'ghn_last_error') IS NOT NULL THEN N'o.ghn_last_error' ELSE N'NULL' END;
DECLARE @ghnAttempt NVARCHAR(100) = CASE WHEN COL_LENGTH('dbo.Orders', 'ghn_last_attempt_at') IS NOT NULL THEN N'o.ghn_last_attempt_at' ELSE N'NULL' END;

DECLARE @copyShippingSql NVARCHAR(MAX) = N'
INSERT INTO dbo.Order_Shipping_Details (
    order_id, fulfillment_method, shipping_fee, shipping_subsidy,
    customer_shipping_fee, tracking_code, to_district_id, to_ward_code,
    delivery_note, ghn_integration_status, ghn_retry_count,
    ghn_last_error, ghn_last_attempt_at
)
SELECT o.order_id, ' + @fulfillment + N', ' + @shippingFee + N', '
    + @shippingSubsidy + N', ' + @customerShippingFee + N', ' + @trackingCode
    + N', ' + @districtId + N', ' + @wardCode + N', ' + @deliveryNote
    + N', ' + @ghnStatus + N', ' + @ghnRetry + N', ' + @ghnError
    + N', ' + @ghnAttempt + N'
FROM dbo.Orders o
LEFT JOIN dbo.Order_Shipping_Details shipping ON shipping.order_id = o.order_id
WHERE shipping.order_id IS NULL;';
EXEC sys.sp_executesql @copyShippingSql;
GO

-- Chỉ thêm unique tracking index khi mã vận đơn hiện tại không bị trùng.
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_OrderShipping_TrackingCode'
      AND object_id = OBJECT_ID(N'dbo.Order_Shipping_Details')
)
AND NOT EXISTS (
    SELECT tracking_code
    FROM dbo.Order_Shipping_Details
    WHERE tracking_code IS NOT NULL
    GROUP BY tracking_code
    HAVING COUNT(*) > 1
)
    CREATE UNIQUE INDEX UX_OrderShipping_TrackingCode
        ON dbo.Order_Shipping_Details(tracking_code)
        WHERE tracking_code IS NOT NULL;
GO

-- ============================================================
-- P0.3 TÁCH THÔNG TIN HỦY ĐƠN KHỎI ORDERS
-- ============================================================
IF OBJECT_ID(N'dbo.Order_Cancellations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Order_Cancellations (
        order_id       INT NOT NULL,
        reason_code    VARCHAR(50) NOT NULL,
        actor_type     VARCHAR(20) NOT NULL,
        detail         NVARCHAR(500) NULL,
        cancelled_at   DATETIME2 NOT NULL
            CONSTRAINT DF_OrderCancellations_CancelledAt DEFAULT GETDATE(),
        CONSTRAINT PK_Order_Cancellations PRIMARY KEY (order_id),
        CONSTRAINT FK_OrderCancellations_Orders FOREIGN KEY (order_id)
            REFERENCES dbo.Orders(order_id) ON DELETE CASCADE,
        CONSTRAINT CK_OrderCancellations_Actor
            CHECK (actor_type IN ('CUSTOMER', 'ADMIN', 'SYSTEM', 'GHN'))
    );
END;
GO

/* Marcus thêm: nếu database thành viên không có cancellation_* thì đơn CANCELLED
   cũ vẫn có bản ghi tối thiểu SYSTEM_OTHER/SYSTEM. */
DECLARE @reasonCode NVARCHAR(300) = CASE
    WHEN COL_LENGTH('dbo.Orders', 'cancellation_reason_code') IS NOT NULL
        THEN N'COALESCE(NULLIF(o.cancellation_reason_code, ''''), ''SYSTEM_OTHER'')'
    ELSE N'''SYSTEM_OTHER'''
END;
DECLARE @actorType NVARCHAR(500) = CASE
    WHEN COL_LENGTH('dbo.Orders', 'cancellation_actor') IS NOT NULL
        THEN N'CASE UPPER(COALESCE(o.cancellation_actor, ''SYSTEM'')) WHEN ''CUSTOMER'' THEN ''CUSTOMER'' WHEN ''ADMIN'' THEN ''ADMIN'' WHEN ''GHN'' THEN ''GHN'' ELSE ''SYSTEM'' END'
    ELSE N'''SYSTEM'''
END;
DECLARE @cancelledAt NVARCHAR(300) = CASE
    WHEN COL_LENGTH('dbo.Orders', 'cancelled_at') IS NOT NULL
        THEN N'COALESCE(o.cancelled_at, o.updated_at, o.created_at, GETDATE())'
    ELSE N'COALESCE(o.updated_at, o.created_at, GETDATE())'
END;
DECLARE @copyCancellationSql NVARCHAR(MAX) = N'
INSERT INTO dbo.Order_Cancellations (order_id, reason_code, actor_type, detail, cancelled_at)
SELECT o.order_id, ' + @reasonCode + N', ' + @actorType + N', NULL, ' + @cancelledAt + N'
FROM dbo.Orders o
LEFT JOIN dbo.Order_Cancellations cancellation ON cancellation.order_id = o.order_id
WHERE UPPER(o.order_status) = ''CANCELLED'' AND cancellation.order_id IS NULL;';
EXEC sys.sp_executesql @copyCancellationSql;
GO

-- Marcus thêm: index chống trùng Attribute Value nếu dữ liệu đủ sạch.
IF OBJECT_ID(N'dbo.Attribute_Values', N'U') IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_AttributeValues_Attribute_Value'
      AND object_id = OBJECT_ID(N'dbo.Attribute_Values')
)
AND NOT EXISTS (
    SELECT 1 FROM dbo.Attribute_Values
    GROUP BY attribute_id, value_string
    HAVING COUNT(*) > 1
)
    CREATE UNIQUE INDEX UX_AttributeValues_Attribute_Value
        ON dbo.Attribute_Values(attribute_id, value_string);
GO

-- ============================================================
-- P0.4 GIAO DỊCH VÀ REFUND VNPAY
-- ============================================================
-- Marcus thêm: đồng bộ đủ cột OrderTransaction mà code hiện tại đang ánh xạ.
IF COL_LENGTH('dbo.Order_Transactions', 'is_reconciled') IS NULL
    ALTER TABLE dbo.Order_Transactions ADD is_reconciled BIT NOT NULL
        CONSTRAINT DF_OrderTransactions_Reconciled_MarcusUpdate DEFAULT 0 WITH VALUES;
IF COL_LENGTH('dbo.Order_Transactions', 'idempotency_key') IS NULL
    ALTER TABLE dbo.Order_Transactions ADD idempotency_key VARCHAR(150) NULL;
IF COL_LENGTH('dbo.Order_Transactions', 'provider_transaction_id') IS NULL
    ALTER TABLE dbo.Order_Transactions ADD provider_transaction_id VARCHAR(100) NULL;
IF COL_LENGTH('dbo.Order_Transactions', 'provider_response_code') IS NULL
    ALTER TABLE dbo.Order_Transactions ADD provider_response_code VARCHAR(20) NULL;
IF COL_LENGTH('dbo.Order_Transactions', 'provider_transaction_date') IS NULL
    ALTER TABLE dbo.Order_Transactions ADD provider_transaction_date VARCHAR(14) NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_OrderTransactions_IdempotencyKey'
      AND object_id = OBJECT_ID(N'dbo.Order_Transactions')
)
AND NOT EXISTS (
    SELECT idempotency_key
    FROM dbo.Order_Transactions
    WHERE idempotency_key IS NOT NULL
    GROUP BY idempotency_key
    HAVING COUNT(*) > 1
)
    CREATE UNIQUE INDEX UX_OrderTransactions_IdempotencyKey
        ON dbo.Order_Transactions(idempotency_key)
        WHERE idempotency_key IS NOT NULL;
GO

-- Marcus thêm: database Dev chưa có Refund_Requests thì tạo đúng schema Entity.
IF OBJECT_ID(N'dbo.Refund_Requests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Refund_Requests (
        refund_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_RefundRequests PRIMARY KEY,
        order_id INT NOT NULL,
        payment_transaction_id INT NOT NULL,
        refund_transaction_id INT NULL,
        request_code VARCHAR(32) NOT NULL,
        idempotency_key VARCHAR(150) NOT NULL,
        amount DECIMAL(18,2) NOT NULL,
        shipping_deducted DECIMAL(18,2) NOT NULL CONSTRAINT DF_RefundRequests_ShippingDeducted DEFAULT 0,
        reason NVARCHAR(500) NOT NULL,
        status VARCHAR(30) NOT NULL,
        requested_by INT NULL,
        approved_by INT NULL,
        retry_count INT NOT NULL CONSTRAINT DF_RefundRequests_RetryCount DEFAULT 0,
        max_retries INT NOT NULL CONSTRAINT DF_RefundRequests_MaxRetries DEFAULT 3,
        next_retry_at DATETIME2 NULL,
        provider_response_id VARCHAR(100) NULL,
        provider_refund_transaction_id VARCHAR(100) NULL,
        provider_response_code VARCHAR(20) NULL,
        provider_transaction_status VARCHAR(20) NULL,
        provider_message NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_RefundRequests_CreatedAt DEFAULT GETDATE(),
        approved_at DATETIME2 NULL,
        last_attempt_at DATETIME2 NULL,
        reconciliation_attempts INT NOT NULL CONSTRAINT DF_RefundRequests_ReconciliationAttempts DEFAULT 0,
        last_reconciled_at DATETIME2 NULL,
        next_reconciliation_at DATETIME2 NULL,
        last_reconciliation_message NVARCHAR(500) NULL,
        manually_confirmed_by INT NULL,
        manually_confirmed_at DATETIME2 NULL,
        manual_confirmation_note NVARCHAR(500) NULL,
        processed_at DATETIME2 NULL,
        row_version BIGINT NOT NULL CONSTRAINT DF_RefundRequests_RowVersion DEFAULT 0,
        CONSTRAINT UX_RefundRequests_RequestCode UNIQUE (request_code),
        CONSTRAINT UX_RefundRequests_IdempotencyKey UNIQUE (idempotency_key),
        CONSTRAINT CK_RefundRequests_Amount CHECK (amount > 0),
        CONSTRAINT FK_RefundRequests_Order FOREIGN KEY (order_id) REFERENCES dbo.Orders(order_id),
        CONSTRAINT FK_RefundRequests_PaymentTransaction FOREIGN KEY (payment_transaction_id) REFERENCES dbo.Order_Transactions(transaction_id),
        CONSTRAINT FK_RefundRequests_RefundTransaction FOREIGN KEY (refund_transaction_id) REFERENCES dbo.Order_Transactions(transaction_id),
        CONSTRAINT FK_RefundRequests_RequestedBy FOREIGN KEY (requested_by) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_RefundRequests_ApprovedBy FOREIGN KEY (approved_by) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_RefundRequests_ManuallyConfirmedBy FOREIGN KEY (manually_confirmed_by) REFERENCES dbo.Users(user_id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_RefundRequests_StatusRetry' AND object_id = OBJECT_ID(N'dbo.Refund_Requests'))
    CREATE INDEX IX_RefundRequests_StatusRetry ON dbo.Refund_Requests(status, next_retry_at);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_RefundRequests_Reconciliation' AND object_id = OBJECT_ID(N'dbo.Refund_Requests'))
    CREATE INDEX IX_RefundRequests_Reconciliation ON dbo.Refund_Requests(status, next_reconciliation_at);
GO

-- ============================================================
-- P1.1 NOTIFICATION: IDEMPOTENCY, CATEGORY, ICON, DEEP LINK, RETENTION
-- ============================================================
IF COL_LENGTH('dbo.Admin_Notifications', 'event_key') IS NULL ALTER TABLE dbo.Admin_Notifications ADD event_key VARCHAR(180) NULL;
IF COL_LENGTH('dbo.Admin_Notifications', 'category') IS NULL ALTER TABLE dbo.Admin_Notifications ADD category VARCHAR(20) NOT NULL CONSTRAINT DF_AdminNotif_Category DEFAULT 'INFO' WITH VALUES;
IF COL_LENGTH('dbo.Admin_Notifications', 'icon') IS NULL ALTER TABLE dbo.Admin_Notifications ADD icon VARCHAR(80) NULL;
IF COL_LENGTH('dbo.Admin_Notifications', 'deep_link') IS NULL ALTER TABLE dbo.Admin_Notifications ADD deep_link VARCHAR(300) NULL;
IF COL_LENGTH('dbo.Admin_Notifications', 'expires_at') IS NULL ALTER TABLE dbo.Admin_Notifications ADD expires_at DATETIME2 NULL;
GO
UPDATE dbo.Admin_Notifications SET event_key = CONCAT('LEGACY_ADMIN:', id) WHERE event_key IS NULL;
UPDATE dbo.Admin_Notifications SET expires_at = DATEADD(DAY, 90, COALESCE(created_at, GETDATE())) WHERE expires_at IS NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_AdminNotifications_EventKey' AND object_id = OBJECT_ID(N'dbo.Admin_Notifications'))
    CREATE UNIQUE INDEX UX_AdminNotifications_EventKey ON dbo.Admin_Notifications(event_key) WHERE event_key IS NOT NULL;
GO

IF COL_LENGTH('dbo.User_Notifications', 'event_key') IS NULL ALTER TABLE dbo.User_Notifications ADD event_key VARCHAR(180) NULL;
IF COL_LENGTH('dbo.User_Notifications', 'category') IS NULL ALTER TABLE dbo.User_Notifications ADD category VARCHAR(20) NOT NULL CONSTRAINT DF_UserNotif_Category DEFAULT 'INFO' WITH VALUES;
IF COL_LENGTH('dbo.User_Notifications', 'icon') IS NULL ALTER TABLE dbo.User_Notifications ADD icon VARCHAR(80) NULL;
IF COL_LENGTH('dbo.User_Notifications', 'deep_link') IS NULL ALTER TABLE dbo.User_Notifications ADD deep_link VARCHAR(300) NULL;
IF COL_LENGTH('dbo.User_Notifications', 'expires_at') IS NULL ALTER TABLE dbo.User_Notifications ADD expires_at DATETIME2 NULL;
GO
UPDATE dbo.User_Notifications SET event_key = CONCAT('LEGACY_USER:', id) WHERE event_key IS NULL;
UPDATE dbo.User_Notifications SET expires_at = DATEADD(DAY, 120, COALESCE(created_at, GETDATE())) WHERE expires_at IS NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_UserNotifications_EventKey' AND object_id = OBJECT_ID(N'dbo.User_Notifications'))
    CREATE UNIQUE INDEX UX_UserNotifications_EventKey ON dbo.User_Notifications(event_key) WHERE event_key IS NOT NULL;
GO

-- ============================================================
-- P1.2 ĐỐI SOÁT, SETTINGS, LIÊN HỆ
-- ============================================================
IF COL_LENGTH('dbo.Order_Transactions', 'reconciled_by') IS NULL ALTER TABLE dbo.Order_Transactions ADD reconciled_by VARCHAR(100) NULL;
IF COL_LENGTH('dbo.Order_Transactions', 'reconciled_at') IS NULL ALTER TABLE dbo.Order_Transactions ADD reconciled_at DATETIME2 NULL;
GO

IF COL_LENGTH('dbo.System_Settings', 'updated_by') IS NULL ALTER TABLE dbo.System_Settings ADD updated_by VARCHAR(100) NULL;
GO

IF COL_LENGTH('dbo.Contact_Requests', 'handled_by') IS NULL ALTER TABLE dbo.Contact_Requests ADD handled_by VARCHAR(100) NULL;
IF COL_LENGTH('dbo.Contact_Requests', 'processing_started_at') IS NULL ALTER TABLE dbo.Contact_Requests ADD processing_started_at DATETIME2 NULL;
IF COL_LENGTH('dbo.Contact_Requests', 'resolved_at') IS NULL ALTER TABLE dbo.Contact_Requests ADD resolved_at DATETIME2 NULL;
GO
UPDATE dbo.Contact_Requests SET status = 'NEW' WHERE status = 'PENDING' OR status IS NULL;
GO

-- ============================================================
-- P1.3 LIVE CHAT METADATA - KHÔNG LƯU NỘI DUNG
-- ============================================================
IF OBJECT_ID(N'dbo.Chat_Session_Metrics', N'U') IS NULL
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
    CREATE INDEX IX_ChatSessionMetrics_StartedAt
        ON dbo.Chat_Session_Metrics(started_at DESC);
END;
GO

-- ============================================================
-- P2 AI + ANALYTICS - Marcus thêm cho thành viên chỉ chạy một file
-- ============================================================
IF OBJECT_ID('dbo.AI_Usage_Events','U') IS NULL
BEGIN
    CREATE TABLE dbo.AI_Usage_Events (
        event_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        session_id VARCHAR(36) NOT NULL,
        advice_id VARCHAR(36) NULL,
        event_type VARCHAR(30) NOT NULL,
        product_id INT NULL,
        response_time_ms INT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_AIUsageEvents_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_AIUsageEvents_Type CHECK (event_type IN ('CHAT_SUCCESS','CHAT_RESPONSE','CHAT_FAILED','PRODUCT_CLICK','FEEDBACK_HELPFUL','FEEDBACK_NOT_HELPFUL')),
        CONSTRAINT CK_AIUsageEvents_ResponseTime CHECK (response_time_ms IS NULL OR response_time_ms BETWEEN 0 AND 120000)
    );
END;
IF COL_LENGTH('dbo.AI_Usage_Events','advice_id') IS NULL ALTER TABLE dbo.AI_Usage_Events ADD advice_id VARCHAR(36) NULL;
IF COL_LENGTH('dbo.AI_Usage_Events','response_time_ms') IS NULL ALTER TABLE dbo.AI_Usage_Events ADD response_time_ms INT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_AIUsageEvents_Advice' AND object_id=OBJECT_ID('dbo.AI_Usage_Events'))
    CREATE INDEX IX_AIUsageEvents_Advice ON dbo.AI_Usage_Events(advice_id,created_at DESC) WHERE advice_id IS NOT NULL;
GO
DECLARE @dropAiEventChecks NVARCHAR(MAX);
SELECT @dropAiEventChecks=STRING_AGG(N'ALTER TABLE dbo.AI_Usage_Events DROP CONSTRAINT '+QUOTENAME(name),N';')
FROM sys.check_constraints
WHERE parent_object_id=OBJECT_ID('dbo.AI_Usage_Events') AND definition LIKE '%event_type%';
IF @dropAiEventChecks IS NOT NULL EXEC sys.sp_executesql @dropAiEventChecks;
ALTER TABLE dbo.AI_Usage_Events ADD CONSTRAINT CK_AIUsageEvents_Type CHECK(
    event_type IN ('CHAT_SUCCESS','CHAT_RESPONSE','CHAT_FAILED','PRODUCT_CLICK','FEEDBACK_HELPFUL','FEEDBACK_NOT_HELPFUL')
);
GO
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_AIUsageEvents_OneFeedback' AND object_id=OBJECT_ID('dbo.AI_Usage_Events'))
    DROP INDEX UX_AIUsageEvents_OneFeedback ON dbo.AI_Usage_Events;
IF COL_LENGTH('dbo.AI_Usage_Events','feedback_advice_key') IS NOT NULL
    ALTER TABLE dbo.AI_Usage_Events DROP COLUMN feedback_advice_key;
GO
;WITH duplicated AS (
    SELECT event_id,ROW_NUMBER() OVER(PARTITION BY advice_id ORDER BY created_at,event_id) AS row_number
    FROM dbo.AI_Usage_Events WHERE advice_id IS NOT NULL AND event_type IN ('FEEDBACK_HELPFUL','FEEDBACK_NOT_HELPFUL')
)
DELETE FROM duplicated WHERE row_number>1;
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
    ) THROW 51402,N'Mỗi câu tư vấn chỉ được ghi nhận một feedback.',1;
END;
GO

IF OBJECT_ID('dbo.AI_Analytics_Reports','U') IS NULL
BEGIN
    CREATE TABLE dbo.AI_Analytics_Reports (
        report_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        from_date DATE NOT NULL,
        to_date DATE NOT NULL,
        report_json NVARCHAR(MAX) NOT NULL,
        model_name VARCHAR(100) NOT NULL,
        data_fingerprint VARCHAR(64) NULL,
        generated_at DATETIME2 NOT NULL CONSTRAINT DF_AIAnalyticsReports_GeneratedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_AIAnalyticsReports_Period CHECK(from_date<=to_date),
        CONSTRAINT CK_AIAnalyticsReports_Json CHECK(ISJSON(report_json)=1)
    );
END;
IF COL_LENGTH('dbo.AI_Analytics_Reports','data_fingerprint') IS NULL
    ALTER TABLE dbo.AI_Analytics_Reports ADD data_fingerprint VARCHAR(64) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_AIAnalyticsReports_Fingerprint' AND object_id=OBJECT_ID('dbo.AI_Analytics_Reports'))
    CREATE INDEX IX_AIAnalyticsReports_Fingerprint ON dbo.AI_Analytics_Reports(from_date,to_date,model_name,data_fingerprint,generated_at DESC);
GO

IF OBJECT_ID('dbo.Customer_Behavior_Events','U') IS NULL
BEGIN
    CREATE TABLE dbo.Customer_Behavior_Events (
        event_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        event_type VARCHAR(30) NOT NULL,
        session_id VARCHAR(36) NULL,
        product_id INT NULL,
        order_id INT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_CustomerBehaviorEvents_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_CustomerBehaviorEvents_Type CHECK(event_type IN ('PRODUCT_VIEW','CHECKOUT_STARTED','ORDER_CREATED','PAYMENT_SUCCESS','AI_QUESTION','AI_PRODUCT_CLICK'))
    );
END;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_CustomerBehaviorEvents_CreatedType' AND object_id=OBJECT_ID('dbo.Customer_Behavior_Events'))
    CREATE INDEX IX_CustomerBehaviorEvents_CreatedType ON dbo.Customer_Behavior_Events(created_at DESC,event_type);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_CustomerBehaviorEvents_Session' AND object_id=OBJECT_ID('dbo.Customer_Behavior_Events'))
    CREATE INDEX IX_CustomerBehaviorEvents_Session ON dbo.Customer_Behavior_Events(session_id,created_at DESC) WHERE session_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_CustomerBehaviorEvents_Order' AND object_id=OBJECT_ID('dbo.Customer_Behavior_Events'))
    CREATE INDEX IX_CustomerBehaviorEvents_Order ON dbo.Customer_Behavior_Events(order_id,created_at DESC) WHERE order_id IS NOT NULL;
GO

-- ============================================================
-- KẾT QUẢ CUỐI: THÀNH VIÊN CHỈ CẦN ĐỌC DÒNG NÀY
-- ============================================================
DECLARE @missingShipping INT = (
    SELECT COUNT(*)
    FROM dbo.Orders orders
    LEFT JOIN dbo.Order_Shipping_Details shipping ON shipping.order_id = orders.order_id
    WHERE shipping.order_id IS NULL
);
DECLARE @missingCancellation INT = (
    SELECT COUNT(*)
    FROM dbo.Orders orders
    LEFT JOIN dbo.Order_Cancellations cancellation ON cancellation.order_id = orders.order_id
    WHERE UPPER(orders.order_status) = 'CANCELLED'
      AND cancellation.order_id IS NULL
);
DECLARE @checkoutIndexOk BIT = CASE WHEN EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_Orders_CheckoutRequestId'
      AND object_id = OBJECT_ID(N'dbo.Orders')
) THEN 1 ELSE 0 END;
DECLARE @transactionIndexOk BIT = CASE WHEN EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_OrderTransactions_IdempotencyKey'
      AND object_id = OBJECT_ID(N'dbo.Order_Transactions')
) THEN 1 ELSE 0 END;
-- Marcus thêm: không chỉ kiểm tra tên bảng; phải đủ cột Entity mới được báo ĐẠT.
DECLARE @shippingColumnsOk BIT = CASE WHEN
    COL_LENGTH('dbo.Order_Shipping_Details', 'fulfillment_method') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'shipping_fee') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'shipping_subsidy') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'customer_shipping_fee') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'tracking_code') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'to_district_id') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'to_ward_code') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'delivery_note') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'ghn_integration_status') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'ghn_retry_count') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'ghn_last_error') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Shipping_Details', 'ghn_last_attempt_at') IS NOT NULL
    THEN 1 ELSE 0 END;
DECLARE @cancellationColumnsOk BIT = CASE WHEN
    COL_LENGTH('dbo.Order_Cancellations', 'reason_code') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Cancellations', 'actor_type') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Cancellations', 'detail') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Cancellations', 'cancelled_at') IS NOT NULL
    THEN 1 ELSE 0 END;
DECLARE @transactionColumnsOk BIT = CASE WHEN
    COL_LENGTH('dbo.Order_Transactions', 'is_reconciled') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Transactions', 'reconciled_by') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Transactions', 'reconciled_at') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Transactions', 'idempotency_key') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Transactions', 'provider_transaction_id') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Transactions', 'provider_response_code') IS NOT NULL AND
    COL_LENGTH('dbo.Order_Transactions', 'provider_transaction_date') IS NOT NULL
    THEN 1 ELSE 0 END;
DECLARE @refundColumnsOk BIT = CASE WHEN
    COL_LENGTH('dbo.Refund_Requests', 'refund_id') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'payment_transaction_id') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'refund_transaction_id') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'request_code') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'idempotency_key') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'next_retry_at') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'reconciliation_attempts') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'next_reconciliation_at') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'manually_confirmed_by') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'manual_confirmation_note') IS NOT NULL AND
    COL_LENGTH('dbo.Refund_Requests', 'row_version') IS NOT NULL
    THEN 1 ELSE 0 END;

SELECT
    CASE WHEN @shippingColumnsOk = 1 THEN 'OK' ELSE 'MISSING_COLUMN' END AS p0_shipping_schema,
    CASE WHEN @cancellationColumnsOk = 1 THEN 'OK' ELSE 'MISSING_COLUMN' END AS p0_cancellation_schema,
    @missingShipping AS missing_shipping_rows,
    @missingCancellation AS missing_cancellation_rows,
    CASE WHEN @checkoutIndexOk = 1 THEN 'OK' ELSE 'CHECK_DUPLICATE' END AS checkout_idempotency,
    CASE WHEN @transactionIndexOk = 1 THEN 'OK' ELSE 'CHECK_DUPLICATE' END AS transaction_idempotency,
    CASE WHEN @transactionColumnsOk = 1 THEN 'OK' ELSE 'MISSING_COLUMN' END AS p0_transaction_schema,
    CASE WHEN @refundColumnsOk = 1 THEN 'OK' ELSE 'MISSING_COLUMN' END AS p0_refund_schema,
    CASE WHEN COL_LENGTH('dbo.Admin_Notifications', 'event_key') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS p1_notification,
    CASE WHEN COL_LENGTH('dbo.Order_Transactions', 'reconciled_at') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS p1_finance,
    CASE WHEN COL_LENGTH('dbo.System_Settings', 'updated_by') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS p1_settings,
    CASE WHEN COL_LENGTH('dbo.Contact_Requests', 'handled_by') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS p1_contact,
    CASE WHEN OBJECT_ID(N'dbo.Chat_Session_Metrics', N'U') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS p1_chat,
    CASE WHEN OBJECT_ID('dbo.TR_AIUsageEvents_OneFeedback','TR') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS p2_feedback_lock,
    CASE WHEN COL_LENGTH('dbo.AI_Analytics_Reports','data_fingerprint') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS p2_analytics_fingerprint,
    CASE WHEN OBJECT_ID('dbo.Customer_Behavior_Events','U') IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS p2_behavior,
    CASE
        WHEN @shippingColumnsOk = 1
         AND @cancellationColumnsOk = 1
         AND @missingShipping = 0
         AND @missingCancellation = 0
         AND @checkoutIndexOk = 1
         AND @transactionIndexOk = 1
         AND @transactionColumnsOk = 1
         AND @refundColumnsOk = 1
         AND COL_LENGTH('dbo.Admin_Notifications', 'event_key') IS NOT NULL
         AND COL_LENGTH('dbo.Order_Transactions', 'reconciled_at') IS NOT NULL
         AND COL_LENGTH('dbo.System_Settings', 'updated_by') IS NOT NULL
         AND COL_LENGTH('dbo.Contact_Requests', 'handled_by') IS NOT NULL
         AND OBJECT_ID(N'dbo.Chat_Session_Metrics', N'U') IS NOT NULL
         AND OBJECT_ID('dbo.TR_AIUsageEvents_OneFeedback','TR') IS NOT NULL
         AND COL_LENGTH('dbo.AI_Analytics_Reports','data_fingerprint') IS NOT NULL
         AND OBJECT_ID('dbo.Customer_Behavior_Events','U') IS NOT NULL
        THEN N'DAT - Khởi động backend mới và chạy UAT.'
        ELSE N'CHUA DAT - Gửi nguyên Result này cho Marcus, không tự sửa SQL.'
    END AS overall_result;
GO
