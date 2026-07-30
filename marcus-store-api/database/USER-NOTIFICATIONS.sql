IF OBJECT_ID('dbo.User_Notifications', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.User_Notifications (
        id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id INT NOT NULL,
        type VARCHAR(50) NOT NULL,
        title NVARCHAR(255) NOT NULL,
        message NVARCHAR(1000) NOT NULL,
        reference_id VARCHAR(50) NULL,
        is_read BIT NOT NULL CONSTRAINT DF_UserNotifications_IsRead DEFAULT 0,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_UserNotifications_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_UserNotifications_User FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id)
    );
    CREATE INDEX IX_UserNotifications_User_Created
        ON dbo.User_Notifications(user_id, created_at DESC);
END;
