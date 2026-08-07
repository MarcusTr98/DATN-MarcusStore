-- ============================================================
-- WARRANTY & RETURNS MANAGEMENT
-- Tạo bảng yêu cầu bảo hành/đổi trả
-- ============================================================

-- Bảng chính lưu yêu cầu bảo hành/đổi trả
CREATE TABLE Warranty_Returns (
    warranty_id       INT IDENTITY(1,1) PRIMARY KEY,
    order_item_id     INT            NOT NULL,
    user_id           INT            NOT NULL,
    reason            NVARCHAR(50)   NOT NULL,  -- DEFECTIVE, DAMAGED, WRONG_ITEM, NOT_AS_DESCRIBED, ACCESSORY_MISSING, OTHER
    description       NVARCHAR(MAX)  NULL,
    status            NVARCHAR(50)   DEFAULT N'PENDING',  -- PENDING, APPROVED, REJECTED, COMPLETED
    admin_note        NVARCHAR(MAX)  NULL,
    processed_by      INT            NULL,
    processed_at      DATETIME2      NULL,
    created_at        DATETIME2      DEFAULT GETDATE(),
    updated_at        DATETIME2      DEFAULT GETDATE(),
    CONSTRAINT FK_Warranty_OrderItems FOREIGN KEY (order_item_id) REFERENCES Order_Items(order_item_id),
    CONSTRAINT FK_Warranty_Users       FOREIGN KEY (user_id)      REFERENCES Users(user_id),
    CONSTRAINT FK_Warranty_Admin       FOREIGN KEY (processed_by) REFERENCES Users(user_id)
);

-- Bảng lưu file đính kèm (ảnh/video) - URL Cloudinary
CREATE TABLE Warranty_Attachments (
    attachment_id   INT IDENTITY(1,1) PRIMARY KEY,
    warranty_id     INT            NOT NULL,
    file_url        NVARCHAR(500)  NOT NULL,  -- URL Cloudinary
    file_type       NVARCHAR(20)   NOT NULL,  -- IMAGE, VIDEO
    file_name       NVARCHAR(255)  NULL,
    file_size       INT            NULL,      -- bytes
    created_at      DATETIME2      DEFAULT GETDATE(),
    CONSTRAINT FK_Attachment_Warranty FOREIGN KEY (warranty_id) REFERENCES Warranty_Returns(warranty_id) ON DELETE CASCADE
);

-- Index để tối ưu truy vấn
CREATE INDEX IX_Warranty_Returns_User ON Warranty_Returns(user_id);
CREATE INDEX IX_Warranty_Returns_OrderItem ON Warranty_Returns(order_item_id);
CREATE INDEX IX_Warranty_Returns_Status ON Warranty_Returns(status);
CREATE INDEX IX_Warranty_Attachments_Warranty ON Warranty_Attachments(warranty_id);
GO

-- ============================================================
-- SEEDING DỮ LIỆU MẪU (nếu cần test)
-- ============================================================

-- Insert sample warranty request
-- INSERT INTO Warranty_Returns (order_item_id, user_id, reason, description, status)
-- VALUES (1, 1, N'DEFECTIVE', N'Sản phẩm bị lỗi màn hình sau 2 ngày sử dụng', N'PENDING');
