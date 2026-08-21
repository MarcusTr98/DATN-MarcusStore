USE MarcusStoreDB;
GO

IF COL_LENGTH('dbo.Orders', 'auto_assign_at') IS NULL
    ALTER TABLE Orders ADD auto_assign_at DATETIME2 NULL;
GO

IF COL_LENGTH('dbo.Users', 'accepting_orders') IS NULL
    ALTER TABLE Users ADD accepting_orders BIT NOT NULL CONSTRAINT DF_Users_AcceptingOrders DEFAULT 1;
IF COL_LENGTH('dbo.Users', 'max_active_orders') IS NULL
    ALTER TABLE Users ADD max_active_orders INT NOT NULL CONSTRAINT DF_Users_MaxActiveOrders DEFAULT 5;
IF COL_LENGTH('dbo.Users', 'last_assigned_at') IS NULL
    ALTER TABLE Users ADD last_assigned_at DATETIME2 NULL;
GO

IF OBJECT_ID('dbo.Order_Assignments', 'U') IS NULL
BEGIN
    CREATE TABLE Order_Assignments (
        assignment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        order_id INT NOT NULL,
        staff_id INT NOT NULL,
        assigned_by INT NULL,
        assignment_type VARCHAR(20) NOT NULL,
        reason NVARCHAR(500) NULL,
        is_current BIT NOT NULL CONSTRAINT DF_OrderAssignments_IsCurrent DEFAULT 1,
        assigned_at DATETIME2 NOT NULL CONSTRAINT DF_OrderAssignments_AssignedAt DEFAULT GETDATE(),
        CONSTRAINT FK_OrderAssignments_Order FOREIGN KEY (order_id) REFERENCES Orders(order_id) ON DELETE CASCADE,
        CONSTRAINT FK_OrderAssignments_Staff FOREIGN KEY (staff_id) REFERENCES Users(user_id),
        CONSTRAINT FK_OrderAssignments_AssignedBy FOREIGN KEY (assigned_by) REFERENCES Users(user_id),
        CONSTRAINT CK_OrderAssignments_Type CHECK (assignment_type IN ('AUTO', 'MANUAL', 'SELF'))
    );

    CREATE UNIQUE INDEX UX_OrderAssignments_CurrentOrder
        ON Order_Assignments(order_id) WHERE is_current = 1;
    CREATE INDEX IX_OrderAssignments_Staff_Current
        ON Order_Assignments(staff_id, is_current, assigned_at DESC);
END
GO

IF OBJECT_ID('dbo.CK_OrderAssignments_Type', 'C') IS NOT NULL
    ALTER TABLE Order_Assignments DROP CONSTRAINT CK_OrderAssignments_Type;
ALTER TABLE Order_Assignments ADD CONSTRAINT CK_OrderAssignments_Type
    CHECK (assignment_type IN ('AUTO', 'MANUAL', 'SELF'));
GO

-- Đưa đơn cũ đủ điều kiện vào hàng chờ. VNPAY chưa thanh toán tiếp tục chờ IPN.
UPDATE Orders
SET auto_assign_at = DATEADD(MINUTE, 5, COALESCE(created_at, GETDATE()))
WHERE order_status = 'PENDING'
  AND auto_assign_at IS NULL
  AND (UPPER(payment_method) <> 'VNPAY' OR UPPER(payment_status) = 'PAID');
GO
