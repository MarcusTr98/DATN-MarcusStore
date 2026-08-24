SET XACT_ABORT ON;
BEGIN TRANSACTION;

-- 1. Chuẩn hóa event store để có bước thêm giỏ trong attribution.
IF OBJECT_ID('dbo.Customer_Behavior_Events', 'U') IS NOT NULL
BEGIN
    IF EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID('dbo.Customer_Behavior_Events')
          AND name = 'CK_CustomerBehaviorEvents_Type'
    )
        ALTER TABLE dbo.Customer_Behavior_Events DROP CONSTRAINT CK_CustomerBehaviorEvents_Type;

    ALTER TABLE dbo.Customer_Behavior_Events WITH CHECK ADD CONSTRAINT CK_CustomerBehaviorEvents_Type CHECK (
        event_type IN ('PRODUCT_VIEW','CART_ADDED','CHECKOUT_STARTED','ORDER_CREATED','PAYMENT_SUCCESS','AI_QUESTION','AI_PRODUCT_CLICK')
    );
END;

-- 2. Hợp nhất click AI cũ vào event store chính rồi loại bảng trùng.
IF OBJECT_ID('dbo.AI_Product_Clicks', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.Customer_Behavior_Events', 'U') IS NOT NULL
BEGIN
    INSERT INTO dbo.Customer_Behavior_Events(event_type, session_id, product_id, created_at)
    SELECT 'AI_PRODUCT_CLICK', old_click.session_id, old_click.product_id, old_click.clicked_at
    FROM dbo.AI_Product_Clicks old_click
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Customer_Behavior_Events behavior
        WHERE behavior.event_type = 'AI_PRODUCT_CLICK'
          AND behavior.session_id = old_click.session_id
          AND behavior.product_id = old_click.product_id
          AND ABS(DATEDIFF(SECOND, behavior.created_at, old_click.clicked_at)) <= 5
    );

    DROP TABLE dbo.AI_Product_Clicks;
END;

-- 3. Workflow tiếp nhận và theo dõi hành động Analytics.
IF OBJECT_ID('dbo.Analytics_Actions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Analytics_Actions (
        action_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        title NVARCHAR(180) NOT NULL,
        reason NVARCHAR(300) NOT NULL,
        priority VARCHAR(20) NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_AnalyticsActions_Status DEFAULT 'ACCEPTED',
        owner_username VARCHAR(100) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_AnalyticsActions_Created DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_AnalyticsActions_Updated DEFAULT SYSDATETIME(),
        CONSTRAINT CK_AnalyticsActions_Priority CHECK (priority IN ('HIGH','MEDIUM','LOW')),
        CONSTRAINT CK_AnalyticsActions_Status CHECK (status IN ('ACCEPTED','IN_PROGRESS','DONE','REJECTED'))
    );
    CREATE INDEX IX_AnalyticsActions_StatusUpdated
        ON dbo.Analytics_Actions(status, updated_at DESC);
END;

COMMIT TRANSACTION;
