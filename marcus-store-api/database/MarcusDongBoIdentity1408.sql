USE MarcusStoreDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/*
    Marcus thêm 14/08 - Đồng bộ IDENTITY sau khi restore hoặc seeding dữ liệu.

    Mục đích:
    - Tự tìm TẤT CẢ bảng có cột IDENTITY trong database.
    - Chỉ reseed khi bộ đếm hiện tại thấp hơn ID lớn nhất đang có.
    - Không xóa, sửa hoặc chèn bản ghi nghiệp vụ.
    - Có thể chạy lại nhiều lần.

    Thành viên chỉ cần chạy toàn bộ file và kiểm tra overall_result = DAT.
*/

IF DB_NAME() <> N'MarcusStoreDB'
    THROW 51600, N'Bạn đang chọn sai database. Hãy chạy file trên MarcusStoreDB.', 1;

DROP TABLE IF EXISTS #IdentitySyncResult;
CREATE TABLE #IdentitySyncResult
(
    table_name       NVARCHAR(517) NOT NULL,
    identity_column  SYSNAME       NOT NULL,
    identity_before  BIGINT        NOT NULL,
    max_existing_id  BIGINT        NOT NULL,
    identity_after   BIGINT        NULL,
    action_taken     VARCHAR(20)    NOT NULL,
    result_status    VARCHAR(20)    NOT NULL
);

DECLARE
    @schemaName SYSNAME,
    @tableName SYSNAME,
    @columnName SYSNAME,
    @qualifiedTable NVARCHAR(517),
    @displayTable NVARCHAR(517),
    @identityBefore BIGINT,
    @identityAfter BIGINT,
    @maxId BIGINT,
    @sql NVARCHAR(MAX);

DECLARE identity_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT schema_info.name, table_info.name, identity_info.name
FROM sys.identity_columns identity_info
JOIN sys.tables table_info ON table_info.object_id = identity_info.object_id
JOIN sys.schemas schema_info ON schema_info.schema_id = table_info.schema_id
WHERE table_info.is_ms_shipped = 0
  AND CONVERT(BIGINT, identity_info.increment_value) > 0
ORDER BY schema_info.name, table_info.name;

OPEN identity_cursor;
FETCH NEXT FROM identity_cursor INTO @schemaName, @tableName, @columnName;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @qualifiedTable = QUOTENAME(@schemaName) + N'.' + QUOTENAME(@tableName);
    SET @displayTable = @schemaName + N'.' + @tableName;
    SET @identityBefore = CONVERT(BIGINT, IDENT_CURRENT(@qualifiedTable));
    SET @maxId = 0;

    SET @sql = N'SELECT @maxOutput = ISNULL(MAX(CONVERT(BIGINT, '
        + QUOTENAME(@columnName) + N')), 0) FROM ' + @qualifiedTable + N';';
    EXEC sys.sp_executesql @sql, N'@maxOutput BIGINT OUTPUT', @maxOutput = @maxId OUTPUT;

    IF @identityBefore < @maxId
    BEGIN
        SET @sql = N'DBCC CHECKIDENT ('''
            + REPLACE(@qualifiedTable, N'''', N'''''') + N''', RESEED, '
            + CONVERT(NVARCHAR(30), @maxId) + N') WITH NO_INFOMSGS;';
        EXEC sys.sp_executesql @sql;
    END;

    SET @identityAfter = CONVERT(BIGINT, IDENT_CURRENT(@qualifiedTable));

    INSERT INTO #IdentitySyncResult
    (
        table_name, identity_column, identity_before, max_existing_id,
        identity_after, action_taken, result_status
    )
    VALUES
    (
        @displayTable,
        @columnName,
        @identityBefore,
        @maxId,
        @identityAfter,
        CASE WHEN @identityBefore < @maxId THEN 'RESEED' ELSE 'KEEP' END,
        CASE WHEN @identityAfter >= @maxId THEN 'OK' ELSE 'ERROR' END
    );

    FETCH NEXT FROM identity_cursor INTO @schemaName, @tableName, @columnName;
END;

CLOSE identity_cursor;
DEALLOCATE identity_cursor;

-- Result 1: những bảng đã được sửa hoặc vẫn còn lỗi.
SELECT
    table_name,
    identity_column,
    identity_before,
    max_existing_id,
    identity_after,
    action_taken,
    result_status
FROM #IdentitySyncResult
WHERE action_taken = 'RESEED' OR result_status <> 'OK'
ORDER BY table_name;

-- Result 2: kết luận cuối cùng cho thành viên.
SELECT
    COUNT(*) AS total_identity_tables,
    SUM(CASE WHEN action_taken = 'RESEED' THEN 1 ELSE 0 END) AS repaired_tables,
    SUM(CASE WHEN result_status <> 'OK' THEN 1 ELSE 0 END) AS failed_tables,
    CASE
        WHEN SUM(CASE WHEN result_status <> 'OK' THEN 1 ELSE 0 END) = 0
            THEN N'DAT - IDENTITY đã đồng bộ, có thể khởi động backend.'
        ELSE N'CHUA DAT - Gửi nguyên hai bảng Result cho Marcus.'
    END AS overall_result
FROM #IdentitySyncResult;
GO
