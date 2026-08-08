/*
===============================================================================
 MARCUS - FILE DUY NHẤT CHUYỂN DỮ LIỆU GIAO NHẬN/HỦY ĐƠN KHỎI ORDERS
===============================================================================

KHÔNG COPY VÀ CHẠY TỪNG ĐOẠN SQL.
MỖI LẦN THAO TÁC: MỞ FILE NÀY -> CTRL+A -> EXECUTE TOÀN BỘ FILE.

========================================================================
TRƯỜNG HỢP A - MÁY ĐÃ CÓ DATABASE MarcusStoreDB VÀ DỮ LIỆU CŨ
========================================================================

LẦN CHẠY 1 - TẠO BẢNG MỚI VÀ COPY DỮ LIỆU, CHƯA XÓA CỘT CŨ

  1. Dừng backend Spring Boot.
  2. Backup database MarcusStoreDB bằng SSMS.
  3. Mở chính file ORDER-NORMALIZE-SHIPPING-CANCELLATION.sql.
  4. Giữ nguyên dòng: DECLARE @MIGRATION_STEP TINYINT = 1;
  5. Nhấn Ctrl+A, sau đó nhấn Execute.
  6. Xem Result cuối cùng. Chỉ được đi tiếp khi:
       migration_result          = DAT
       missing_shipping_rows     = 0
       missing_cancellation_rows = 0
  7. Khởi động backend bằng CODE MỚI.
  8. Thao tác thử trên website theo 4 bài kiểm tra bên dưới.

LẦN CHẠY 2 - DỌN CỘT CŨ SAU KHI WEBSITE ĐÃ KIỂM TRA THÀNH CÔNG

  9. Chỉ làm bước này khi cả 4 bài kiểm tra trên website đều thành công.
 10. Dừng backend và backup MarcusStoreDB lần nữa.
 11. Trong file này, chỉ đổi số 1 thành số 2 tại dòng:
       DECLARE @MIGRATION_STEP TINYINT = 2;
 12. Nhấn Ctrl+A, sau đó nhấn Execute toàn bộ file.
 13. Khởi động backend và test nhanh lại Checkout + hủy đơn + Analytics.

4 BÀI KIỂM TRA TRÊN WEBSITE GIỮA LẦN CHẠY 1 VÀ LẦN CHẠY 2

  - KIỂM TRA 1 - ĐƠN GIAO HÀNG:
    Đăng nhập khách -> thêm một sản phẩm -> Giỏ hàng -> Mua hàng -> chọn
    "Giao tận nơi" và COD -> Đặt hàng. Sau đó đăng nhập Admin -> Đơn hàng ->
    mở đơn vừa tạo. Kết quả đúng: đơn hiện địa chỉ, phí ship và phương thức
    "Giao tận nơi"; trang không báo lỗi 500.

  - KIỂM TRA 2 - ĐƠN NHẬN TẠI CỬA HÀNG:
    Khách tạo thêm một đơn nhưng chọn "Nhận tại cửa hàng". Kết quả đúng:
    phí giao hàng bằng 0 và chi tiết đơn ghi "Nhận tại cửa hàng".

  - KIỂM TRA 3 - HỦY ĐƠN:
    Tại Đơn hàng của tôi, mở một đơn mới còn nút Hủy -> bấm Hủy -> chọn lý do
    -> xác nhận. Admin mở lại đơn đó. Kết quả đúng: trạng thái "Đã hủy" và
    Admin xem được lý do hủy; trang không báo lỗi 500.

  - KIỂM TRA 4 - CÁC MÀN ĐỌC DỮ LIỆU:
    Admin lần lượt mở "Phân tích kinh doanh" và "Đối soát tài chính".
    Kết quả đúng: cả hai trang tải được dữ liệu, không báo lỗi 500.

SAU LẦN CHẠY 2, KIỂM TRA NHANH BẰNG SQL

  SELECT TOP (20) * FROM dbo.Order_Shipping_Details ORDER BY order_id DESC;
  SELECT TOP (20) * FROM dbo.Order_Cancellations ORDER BY cancelled_at DESC;

========================================================================
TRƯỜNG HỢP B - MÁY TẠO DATABASE MỚI TỪ FILE BẢNG MarcusStoreDB2.sql
========================================================================

  Không chạy file migration này. File bảng mới đã tạo sẵn hai bảng chuẩn.

LƯU Ý QUAN TRỌNG

  - Đoạn USE/SET/kiểm tra Orders ở đầu file không làm thay đổi dữ liệu.
  - Nếu lỡ chỉ chạy đoạn đầu, hãy dừng backend rồi Ctrl+A/Execute toàn file.
  - Lần 1 an toàn: chỉ COPY, chưa DROP cột.
  - Không chạy lần 2 nếu hai chỉ số missing chưa bằng 0 hoặc 4 bài kiểm tra
    trên website chưa thành công.
  - transaction_id và payment_date vẫn ở Orders vì còn thuộc luồng VNPAY.
===============================================================================
*/

USE MarcusStoreDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

-- Lần đầu để 1. Chỉ đổi thành 2 sau khi 4 bài kiểm tra website đều thành công.
DECLARE @MIGRATION_STEP TINYINT = 1;

IF @MIGRATION_STEP NOT IN (1, 2)
    THROW 51009, N'MIGRATION_STEP chỉ được phép là 1 hoặc 2.', 1;

-- ============================================================
-- BƯỚC 1: KIỂM TRA ĐIỀU KIỆN
-- ============================================================
IF OBJECT_ID(N'dbo.Orders', N'U') IS NULL
    THROW 51000, N'Không tìm thấy dbo.Orders trong MarcusStoreDB.', 1;

-- ============================================================
-- BƯỚC 2: TẠO HAI BẢNG MỚI
-- ============================================================
BEGIN TRANSACTION;

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
        CONSTRAINT FK_OrderShipping_Orders
            FOREIGN KEY (order_id) REFERENCES dbo.Orders(order_id) ON DELETE CASCADE,
        CONSTRAINT CK_OrderShipping_Fulfillment
            CHECK (fulfillment_method IN ('DELIVERY', 'STORE_PICKUP')),
        CONSTRAINT CK_OrderShipping_Fees
            CHECK (
                (shipping_fee IS NULL OR shipping_fee >= 0)
                AND shipping_subsidy >= 0
                AND (customer_shipping_fee IS NULL OR customer_shipping_fee >= 0)
            ),
        CONSTRAINT CK_OrderShipping_GhnRetry CHECK (ghn_retry_count >= 0)
    );

    CREATE UNIQUE INDEX UX_OrderShipping_TrackingCode
        ON dbo.Order_Shipping_Details(tracking_code)
        WHERE tracking_code IS NOT NULL;
END;

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
        CONSTRAINT FK_OrderCancellations_Orders
            FOREIGN KEY (order_id) REFERENCES dbo.Orders(order_id) ON DELETE CASCADE,
        CONSTRAINT CK_OrderCancellations_Actor
            CHECK (actor_type IN ('CUSTOMER', 'ADMIN', 'SYSTEM', 'GHN'))
    );
END;

COMMIT TRANSACTION;

-- ============================================================
-- BƯỚC 3: COPY DỮ LIỆU CŨ (KHÔNG XÓA CỘT CŨ)
-- ============================================================
-- Các cột nguồn đang tồn tại trong MarcusStoreDB hiện tại. Nếu chạy cho DB của
-- thành viên chưa có đủ cột, cần chạy file bổ sung schema cũ trước file này.
IF @MIGRATION_STEP = 1
BEGIN
BEGIN TRANSACTION;

MERGE dbo.Order_Shipping_Details AS target
USING (
    SELECT
        order_id,
        CASE UPPER(LTRIM(RTRIM(COALESCE(fulfillment_method, 'DELIVERY'))))
            WHEN 'STORE_PICKUP' THEN 'STORE_PICKUP'
            ELSE 'DELIVERY'
        END AS fulfillment_method,
        shipping_fee,
        COALESCE(shipping_subsidy, 0) AS shipping_subsidy,
        customer_shipping_fee,
        tracking_code,
        to_district_id,
        to_ward_code,
        delivery_note,
        COALESCE(NULLIF(UPPER(LTRIM(RTRIM(ghn_integration_status))), ''), 'NOT_REQUIRED') AS ghn_integration_status,
        COALESCE(ghn_retry_count, 0) AS ghn_retry_count,
        ghn_last_error,
        ghn_last_attempt_at
    FROM dbo.Orders
) AS source
ON target.order_id = source.order_id
WHEN MATCHED THEN UPDATE SET
    target.fulfillment_method = source.fulfillment_method,
    target.shipping_fee = source.shipping_fee,
    target.shipping_subsidy = source.shipping_subsidy,
    target.customer_shipping_fee = source.customer_shipping_fee,
    target.tracking_code = source.tracking_code,
    target.to_district_id = source.to_district_id,
    target.to_ward_code = source.to_ward_code,
    target.delivery_note = source.delivery_note,
    target.ghn_integration_status = source.ghn_integration_status,
    target.ghn_retry_count = source.ghn_retry_count,
    target.ghn_last_error = source.ghn_last_error,
    target.ghn_last_attempt_at = source.ghn_last_attempt_at
WHEN NOT MATCHED THEN INSERT (
    order_id, fulfillment_method, shipping_fee, shipping_subsidy,
    customer_shipping_fee, tracking_code, to_district_id, to_ward_code,
    delivery_note, ghn_integration_status, ghn_retry_count, ghn_last_error,
    ghn_last_attempt_at
) VALUES (
    source.order_id, source.fulfillment_method, source.shipping_fee, source.shipping_subsidy,
    source.customer_shipping_fee, source.tracking_code, source.to_district_id, source.to_ward_code,
    source.delivery_note, source.ghn_integration_status, source.ghn_retry_count, source.ghn_last_error,
    source.ghn_last_attempt_at
);

MERGE dbo.Order_Cancellations AS target
USING (
    SELECT
        order_id,
        COALESCE(NULLIF(cancellation_reason_code, ''), 'SYSTEM_OTHER') AS reason_code,
        CASE UPPER(COALESCE(cancellation_actor, 'SYSTEM'))
            WHEN 'CUSTOMER' THEN 'CUSTOMER'
            WHEN 'ADMIN' THEN 'ADMIN'
            WHEN 'GHN' THEN 'GHN'
            ELSE 'SYSTEM'
        END AS actor_type,
        CAST(NULL AS NVARCHAR(500)) AS detail,
        COALESCE(cancelled_at, updated_at, created_at, GETDATE()) AS cancelled_at
    FROM dbo.Orders
    WHERE order_status = 'CANCELLED'
       OR cancellation_reason_code IS NOT NULL
       OR cancelled_at IS NOT NULL
) AS source
ON target.order_id = source.order_id
WHEN MATCHED THEN UPDATE SET
    target.reason_code = source.reason_code,
    target.actor_type = source.actor_type,
    target.cancelled_at = source.cancelled_at
WHEN NOT MATCHED THEN INSERT (order_id, reason_code, actor_type, detail, cancelled_at)
VALUES (source.order_id, source.reason_code, source.actor_type, source.detail, source.cancelled_at);

COMMIT TRANSACTION;
END;

-- ============================================================
-- BƯỚC 4: ĐỐI CHIẾU. CẢ HAI mismatch PHẢI BẰNG 0.
-- ============================================================
SELECT
    (SELECT COUNT(*) FROM dbo.Orders) AS orders_count,
    (SELECT COUNT(*) FROM dbo.Order_Shipping_Details) AS shipping_rows,
    (SELECT COUNT(*) FROM dbo.Orders o
        LEFT JOIN dbo.Order_Shipping_Details s ON s.order_id = o.order_id
        WHERE s.order_id IS NULL) AS missing_shipping_rows;

SELECT
    (SELECT COUNT(*) FROM dbo.Orders
        WHERE order_status = 'CANCELLED'
           OR cancellation_reason_code IS NOT NULL
           OR cancelled_at IS NOT NULL) AS cancellation_source_rows,
    (SELECT COUNT(*) FROM dbo.Order_Cancellations) AS cancellation_rows,
    (SELECT COUNT(*)
        FROM dbo.Orders o
        LEFT JOIN dbo.Order_Cancellations c ON c.order_id = o.order_id
        WHERE (o.order_status = 'CANCELLED'
            OR o.cancellation_reason_code IS NOT NULL
            OR o.cancelled_at IS NOT NULL)
          AND c.order_id IS NULL) AS missing_cancellation_rows;

SELECT TOP (20)
    o.order_code,
    s.fulfillment_method,
    s.tracking_code,
    s.ghn_integration_status,
    s.ghn_retry_count,
    c.reason_code,
    c.actor_type,
    c.cancelled_at
FROM dbo.Orders o
INNER JOIN dbo.Order_Shipping_Details s ON s.order_id = o.order_id
LEFT JOIN dbo.Order_Cancellations c ON c.order_id = o.order_id
ORDER BY o.order_id DESC;

-- Marcus thêm kết luận rõ ràng để người chạy không phải tự suy luận các số liệu.
DECLARE @missing_shipping_rows INT = (
    SELECT COUNT(*)
    FROM dbo.Orders o
    LEFT JOIN dbo.Order_Shipping_Details s ON s.order_id = o.order_id
    WHERE s.order_id IS NULL
);
DECLARE @missing_cancellation_rows INT = (
    SELECT COUNT(*)
    FROM dbo.Orders o
    LEFT JOIN dbo.Order_Cancellations c ON c.order_id = o.order_id
    WHERE (o.order_status = 'CANCELLED'
        OR o.cancellation_reason_code IS NOT NULL
        OR o.cancelled_at IS NOT NULL)
      AND c.order_id IS NULL
);

SELECT
    @MIGRATION_STEP AS migration_step,
    CASE
        WHEN @missing_shipping_rows = 0 AND @missing_cancellation_rows = 0
            THEN N'DAT'
        ELSE N'CHUA_DAT'
    END AS migration_result,
    @missing_shipping_rows AS missing_shipping_rows,
    @missing_cancellation_rows AS missing_cancellation_rows,
    CASE
        WHEN @missing_shipping_rows <> 0 OR @missing_cancellation_rows <> 0
            THEN N'DỪNG LẠI: không chạy bước 2; gửi bảng kết quả này cho Marcus.'
        WHEN @MIGRATION_STEP = 1
            THEN N'Khởi động backend mới và làm 4 bài kiểm tra website. Thành công hết mới đổi MIGRATION_STEP = 2.'
        ELSE N'Đối chiếu đạt. File sẽ tiếp tục cleanup các cột cũ ở phần bên dưới.'
    END AS next_action;

-- ============================================================
-- BƯỚC 5: CHỈ DỌN CỘT CŨ SAU KHI 4 BÀI KIỂM TRA WEBSITE THÀNH CÔNG
-- ============================================================
IF @MIGRATION_STEP = 2
BEGIN
    IF @missing_shipping_rows <> 0
        THROW 51001, N'Không được cleanup: còn đơn chưa có Order_Shipping_Details.', 1;

    IF @missing_cancellation_rows <> 0
        THROW 51002, N'Không được cleanup: còn đơn hủy chưa có Order_Cancellations.', 1;

    DECLARE @columns TABLE (column_name SYSNAME);
    INSERT INTO @columns(column_name) VALUES
        ('fulfillment_method'), ('shipping_fee'), ('shipping_subsidy'),
        ('customer_shipping_fee'), ('tracking_code'), ('to_district_id'),
        ('to_ward_code'), ('delivery_note'), ('ghn_integration_status'),
        ('ghn_retry_count'), ('ghn_last_error'), ('ghn_last_attempt_at'),
        ('cancellation_reason_code'), ('cancellation_actor'), ('cancelled_at');

    DECLARE @sql NVARCHAR(MAX) = N'';

    -- Xóa index trên Orders có tham chiếu tới cột được chuyển đi.
    SELECT @sql = @sql + N'DROP INDEX ' + QUOTENAME(indexes.name)
        + N' ON dbo.Orders;' + CHAR(13)
    FROM sys.indexes indexes
    WHERE indexes.object_id = OBJECT_ID(N'dbo.Orders')
      AND indexes.is_primary_key = 0
      AND indexes.is_unique_constraint = 0
      AND EXISTS (
          SELECT 1
          FROM sys.index_columns index_columns
          INNER JOIN sys.columns columns
              ON columns.object_id = index_columns.object_id
             AND columns.column_id = index_columns.column_id
          INNER JOIN @columns selected ON selected.column_name = columns.name
          WHERE index_columns.object_id = indexes.object_id
            AND index_columns.index_id = indexes.index_id
      );

    IF @sql <> N'' EXEC sys.sp_executesql @sql;
    SET @sql = N'';

    -- Xóa default/check constraint có tham chiếu tới các cột sẽ dọn.
    SELECT @sql = @sql + N'ALTER TABLE dbo.Orders DROP CONSTRAINT '
        + QUOTENAME(object_name) + N';' + CHAR(13)
    FROM (
        SELECT DISTINCT constraints.name AS object_name
        FROM sys.default_constraints constraints
        INNER JOIN sys.columns columns
            ON columns.object_id = constraints.parent_object_id
           AND columns.column_id = constraints.parent_column_id
        INNER JOIN @columns selected ON selected.column_name = columns.name
        WHERE constraints.parent_object_id = OBJECT_ID(N'dbo.Orders')
        UNION
        SELECT DISTINCT checks.name
        FROM sys.check_constraints checks
        WHERE checks.parent_object_id = OBJECT_ID(N'dbo.Orders')
          AND EXISTS (
              SELECT 1 FROM @columns selected
              WHERE checks.definition LIKE N'%' + selected.column_name + N'%'
          )
    ) dependencies;

    IF @sql <> N'' EXEC sys.sp_executesql @sql;

    SET @sql = N'';
    SELECT @sql = @sql + N'ALTER TABLE dbo.Orders DROP COLUMN ' + QUOTENAME(selected.column_name) + N';' + CHAR(13)
    FROM @columns selected
    WHERE COL_LENGTH('dbo.Orders', selected.column_name) IS NOT NULL;

    IF @sql <> N'' EXEC sys.sp_executesql @sql;

    PRINT N'BƯỚC 2 hoàn tất. Đã xóa các cột giao nhận/GHN/hủy cũ khỏi Orders.';
    PRINT N'transaction_id/payment_date vẫn được giữ để chuyển đổi Order_Transactions ở task riêng.';
END
ELSE
BEGIN
    PRINT N'BƯỚC 1 hoàn tất. Đã tạo bảng mới và copy dữ liệu an toàn.';
    PRINT N'Chưa xóa cột cũ khỏi Orders. Hãy kiểm tra website trước khi chạy BƯỚC 2.';
END;

SELECT
    @MIGRATION_STEP AS completed_step,
    CASE WHEN @MIGRATION_STEP = 1
        THEN N'ĐÃ COPY AN TOÀN - BẬT BACKEND VÀ LÀM 4 BÀI KIỂM TRA WEBSITE'
        ELSE N'ĐÃ CLEANUP - BÂY GIỜ KHỞI ĐỘNG BACKEND VÀ TEST NHANH'
    END AS final_result;
GO
