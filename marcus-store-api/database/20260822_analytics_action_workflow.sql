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
    CREATE INDEX IX_AnalyticsActions_StatusUpdated ON dbo.Analytics_Actions(status, updated_at DESC);
END;
