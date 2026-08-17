USE MarcusStoreDB;
GO

IF COL_LENGTH('dbo.Orders', 'auto_assign_at') IS NULL
    ALTER TABLE Orders ADD auto_assign_at DATETIME2 NULL;
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
        CONSTRAINT CK_OrderAssignments_Type CHECK (assignment_type IN ('AUTO', 'MANUAL'))
    );

    CREATE UNIQUE INDEX UX_OrderAssignments_CurrentOrder
        ON Order_Assignments(order_id) WHERE is_current = 1;
    CREATE INDEX IX_OrderAssignments_Staff_Current
        ON Order_Assignments(staff_id, is_current, assigned_at DESC);
END
GO
