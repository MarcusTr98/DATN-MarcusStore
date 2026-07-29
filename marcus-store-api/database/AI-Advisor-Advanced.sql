-- Marcus thêm: migration tối thiểu cho thống kê click từ Marcus AI.
-- Không lưu nội dung chat, IP, user_id hoặc dữ liệu cá nhân.
IF OBJECT_ID(N'dbo.AI_Product_Clicks', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AI_Product_Clicks (
        click_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        product_id INT NOT NULL,
        session_id VARCHAR(36) NOT NULL,
        clicked_at DATETIME2 NOT NULL CONSTRAINT DF_AI_Product_Clicks_ClickedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_AI_Product_Clicks_Product
            FOREIGN KEY (product_id) REFERENCES dbo.Products(product_id)
    );

    CREATE INDEX IX_AI_Product_Clicks_Product_ClickedAt
        ON dbo.AI_Product_Clicks(product_id, clicked_at DESC);

    CREATE INDEX IX_AI_Product_Clicks_Session
        ON dbo.AI_Product_Clicks(session_id, product_id, clicked_at DESC);
END;
