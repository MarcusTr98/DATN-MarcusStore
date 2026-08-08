USE MarcusStoreDB;
GO

/*
    Marcus thêm: dữ liệu lịch sử riêng cho tab Phân tích kinh doanh.

    Mục tiêu:
    - Phủ dữ liệu từ 01/01/2025 đến hôm qua để xem xu hướng dài hạn.
    - Tạo nhịp bán hàng tăng dần, có mùa vụ, cuối tuần và dịp cuối tháng.
    - Có COD/VNPAY, đơn hoàn tất/hủy, voucher, flash sale snapshot và hoàn tiền.
    - Chỉ dùng SKU quản lý theo SỐ LƯỢNG (không có Product_Items).
    - Không cập nhật Product_Skus.stock_quantity, Product_Items hoặc IMEI.

    Có thể chạy lại an toàn:
    - Chỉ làm mới đơn có prefix ANL-SEED-.
    - Không xóa hoặc sửa đơn thật, đơn test và dữ liệu kho.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @StartDate DATE = '2025-01-01';
DECLARE @EndDate DATE = DATEADD(DAY, -1, CAST(GETDATE() AS DATE));
DECLARE @SeedPrefix VARCHAR(20) = 'ANL-SEED-';

IF @EndDate < @StartDate
BEGIN
    THROW 51000, N'Ngày kết thúc seed phải từ 01/01/2025 trở đi.', 1;
END;

IF OBJECT_ID(N'dbo.Order_Shipping_Details', N'U') IS NULL
BEGIN
    THROW 51001, N'Chưa có Order_Shipping_Details. Hãy chạy ORDER-NORMALIZE-SHIPPING-CANCELLATION.sql trước.', 1;
END;

IF OBJECT_ID(N'dbo.Order_Cancellations', N'U') IS NULL
    THROW 51005, N'Chưa có Order_Cancellations. Hãy chạy file chuẩn hóa Orders trước.', 1;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.Users u
    INNER JOIN dbo.Roles r ON r.role_id = u.role_id
    WHERE r.role_name = 'CUSTOMER'
      AND u.is_active = 1
)
BEGIN
    THROW 51002, N'Cần ít nhất một tài khoản CUSTOMER đang hoạt động để seed đơn hàng.', 1;
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.Product_Skus sku
    INNER JOIN dbo.Products p ON p.product_id = sku.product_id
    WHERE sku.is_active = 1
      AND p.status = 1
      AND NOT EXISTS (
          SELECT 1
          FROM dbo.Product_Items item
          WHERE item.sku_id = sku.sku_id
      )
)
BEGIN
    THROW 51003, N'Không có SKU quản lý theo số lượng phù hợp để seed dữ liệu phân tích.', 1;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    /* Marcus làm: hai voucher chỉ phục vụ dữ liệu lịch sử, để inactive nhằm
       không xuất hiện như voucher đang dùng ở Checkout. */
    IF NOT EXISTS (SELECT 1 FROM dbo.Vouchers WHERE voucher_code = 'ANL-SEED-10P')
    BEGIN
        INSERT INTO dbo.Vouchers (
            voucher_code,
            discount_value,
            discount_type,
            max_discount_amount,
            min_order_value,
            start_date,
            end_date,
            quantity,
            is_active,
            target_type
        )
        VALUES (
            'ANL-SEED-10P',
            10,
            'PERCENT',
            500000,
            1000000,
            @StartDate,
            DATEADD(DAY, 1, @EndDate),
            100000,
            0,
            'ALL'
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM dbo.Vouchers WHERE voucher_code = 'ANL-SEED-300K')
    BEGIN
        INSERT INTO dbo.Vouchers (
            voucher_code,
            discount_value,
            discount_type,
            max_discount_amount,
            min_order_value,
            start_date,
            end_date,
            quantity,
            is_active,
            target_type
        )
        VALUES (
            'ANL-SEED-300K',
            300000,
            'AMOUNT',
            NULL,
            5000000,
            @StartDate,
            DATEADD(DAY, 1, @EndDate),
            100000,
            0,
            'ALL'
        );
    END;

    /* Marcus sửa: ngày kết thúc voucher seed được kéo dài khi chạy lại vào
       thời điểm mới; các thuộc tính voucher thật không bị tác động. */
    UPDATE dbo.Vouchers
    SET start_date = @StartDate,
        end_date = DATEADD(DAY, 1, @EndDate),
        is_active = 0
    WHERE voucher_code IN ('ANL-SEED-10P', 'ANL-SEED-300K');

    /* Marcus làm: dọn đúng dữ liệu do script này sở hữu. Các bảng con của đơn
       được xóa theo khóa ngoại ON DELETE CASCADE. */
    DELETE FROM dbo.Orders
    WHERE order_code LIKE @SeedPrefix + '%';

    CREATE TABLE #Customers (
        customer_no INT NOT NULL PRIMARY KEY,
        user_id INT NOT NULL,
        full_name NVARCHAR(100) NULL,
        phone_number VARCHAR(15) NULL
    );

    INSERT INTO #Customers (customer_no, user_id, full_name, phone_number)
    SELECT
        ROW_NUMBER() OVER (ORDER BY u.user_id),
        u.user_id,
        u.full_name,
        u.phone_number
    FROM dbo.Users u
    INNER JOIN dbo.Roles r ON r.role_id = u.role_id
    WHERE r.role_name = 'CUSTOMER'
      AND u.is_active = 1;

    DECLARE @CustomerCount INT = (SELECT COUNT(*) FROM #Customers);

    /* Marcus làm: mỗi sản phẩm lấy một SKU đang bán và không quản lý IMEI.
       Cách này giúp phủ nhiều sản phẩm nhưng không tạo đơn hoàn tất thiếu IMEI. */
    CREATE TABLE #SkuPool (
        product_no INT NOT NULL PRIMARY KEY,
        sku_id INT NOT NULL,
        product_id INT NOT NULL,
        product_name NVARCHAR(150) NOT NULL,
        price DECIMAL(18,2) NOT NULL
    );

    WITH RankedSkus AS (
        SELECT
            sku.sku_id,
            sku.product_id,
            p.product_name,
            sku.price,
            ROW_NUMBER() OVER (
                PARTITION BY sku.product_id
                ORDER BY sku.stock_quantity DESC, sku.price, sku.sku_id
            ) AS sku_rank
        FROM dbo.Product_Skus sku
        INNER JOIN dbo.Products p ON p.product_id = sku.product_id
        WHERE sku.is_active = 1
          AND p.status = 1
          AND NOT EXISTS (
              SELECT 1
              FROM dbo.Product_Items item
              WHERE item.sku_id = sku.sku_id
          )
    )
    INSERT INTO #SkuPool (product_no, sku_id, product_id, product_name, price)
    SELECT
        ROW_NUMBER() OVER (ORDER BY product_id),
        sku_id,
        product_id,
        product_name,
        price
    FROM RankedSkus
    WHERE sku_rank = 1;

    DECLARE @ProductCount INT = (SELECT COUNT(*) FROM #SkuPool);
    DECLARE @VoucherPercentId INT = (
        SELECT voucher_id FROM dbo.Vouchers WHERE voucher_code = 'ANL-SEED-10P'
    );
    DECLARE @VoucherAmountId INT = (
        SELECT voucher_id FROM dbo.Vouchers WHERE voucher_code = 'ANL-SEED-300K'
    );

    CREATE TABLE #SeedOrders (
        seed_no INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        order_date DATETIME2 NOT NULL,
        user_id INT NOT NULL,
        recipient_name NVARCHAR(100) NOT NULL,
        recipient_phone VARCHAR(15) NOT NULL,
        sku_id INT NOT NULL,
        quantity INT NOT NULL,
        original_unit_price DECIMAL(18,2) NOT NULL,
        sale_unit_price DECIMAL(18,2) NOT NULL,
        is_flash_sale BIT NOT NULL,
        voucher_id INT NULL,
        discount_amount DECIMAL(18,2) NOT NULL,
        shipping_fee DECIMAL(18,2) NOT NULL,
        fulfillment_method VARCHAR(30) NOT NULL,
        payment_method NVARCHAR(50) NOT NULL,
        order_status NVARCHAR(50) NOT NULL,
        payment_status NVARCHAR(50) NOT NULL
    );

    /*
       Marcus làm: số đơn tăng dần theo giai đoạn.
       - Nửa đầu 2025: 1 đơn/ngày, cuối tuần có thêm đơn.
       - Nửa cuối 2025: 2 đơn/ngày.
       - Từ 2026: 2 đơn/ngày, cuối tuần/cuối tháng có thể có đơn thứ ba.
       Dữ liệu có tính mùa vụ nhưng vẫn xác định, chạy lại cho cùng kết quả.
    */
    ;WITH DateSeries AS (
        SELECT @StartDate AS sale_date
        UNION ALL
        SELECT DATEADD(DAY, 1, sale_date)
        FROM DateSeries
        WHERE sale_date < @EndDate
    ),
    Slots AS (
        SELECT 1 AS slot_no
        UNION ALL SELECT 2
        UNION ALL SELECT 3
    ),
    Demand AS (
        SELECT
            d.sale_date,
            s.slot_no,
            DATEDIFF(MONTH, @StartDate, d.sale_date) AS month_index,
            ABS(CHECKSUM(CONVERT(CHAR(8), d.sale_date, 112), s.slot_no)) AS seed_hash
        FROM DateSeries d
        CROSS JOIN Slots s
        WHERE s.slot_no = 1
           OR (
                s.slot_no = 2
                AND (
                    d.sale_date >= '2025-07-01'
                    OR DATEDIFF(DAY, '19000101', d.sale_date) % 7 IN (5, 6)
                )
           )
           OR (
                s.slot_no = 3
                AND d.sale_date >= '2026-01-01'
                AND (
                    DATEDIFF(DAY, '19000101', d.sale_date) % 7 IN (5, 6)
                    OR DAY(d.sale_date) >= 25
                )
           )
    ),
    Prepared AS (
        SELECT
            DATEADD(
                MINUTE,
                540 + ((d.seed_hash / 7) % 720),
                CAST(d.sale_date AS DATETIME2)
            ) AS order_date,
            customer.user_id,
            COALESCE(NULLIF(customer.full_name, N''), N'Khách hàng Marcus') AS recipient_name,
            COALESCE(NULLIF(customer.phone_number, ''), '0900000000') AS recipient_phone,
            sku.sku_id,
            CASE
                WHEN sku.price < 3000000 AND d.seed_hash % 5 = 0 THEN 2
                ELSE 1
            END AS quantity,
            sku.price AS original_unit_price,
            CAST(
                FLOOR(
                    (
                        sku.price
                        * CASE
                            WHEN d.sale_date < '2025-07-01' THEN 0.90
                            WHEN d.sale_date < '2026-01-01' THEN 0.93
                            ELSE 0.96
                          END
                        * CASE WHEN d.seed_hash % 9 = 0 THEN 0.95 ELSE 1 END
                    ) / 1000
                ) * 1000
                AS DECIMAL(18,2)
            ) AS sale_unit_price,
            CAST(CASE WHEN d.seed_hash % 9 = 0 THEN 1 ELSE 0 END AS BIT) AS is_flash_sale,
            CASE
                WHEN d.seed_hash % 10 IN (0, 1) AND sku.price >= 5000000
                    THEN CASE WHEN d.seed_hash % 2 = 0
                        THEN @VoucherPercentId ELSE @VoucherAmountId END
                ELSE NULL
            END AS voucher_id,
            CAST(CASE
                WHEN d.sale_date >= '2026-05-01' AND d.seed_hash % 20 = 0 THEN 0
                ELSE 30000
            END AS DECIMAL(18,2)) AS shipping_fee,
            CASE
                WHEN d.sale_date >= '2026-05-01' AND d.seed_hash % 20 = 0
                    THEN 'STORE_PICKUP'
                ELSE 'DELIVERY'
            END AS fulfillment_method,
            CASE WHEN d.seed_hash % 100 < 48 THEN N'VNPAY' ELSE N'COD' END AS payment_method,
            CASE WHEN d.seed_hash % 100 < 84 THEN N'COMPLETED' ELSE N'CANCELLED' END AS order_status,
            d.seed_hash
        FROM Demand d
        INNER JOIN #Customers customer
            ON customer.customer_no = 1 + (d.seed_hash % @CustomerCount)
        INNER JOIN #SkuPool sku
            ON sku.product_no = 1 + (
                (
                    DATEDIFF(DAY, @StartDate, d.sale_date) * 3
                    + d.slot_no
                    + d.month_index * 7
                )
                % @ProductCount
            )
    )
    INSERT INTO #SeedOrders (
        order_date,
        user_id,
        recipient_name,
        recipient_phone,
        sku_id,
        quantity,
        original_unit_price,
        sale_unit_price,
        is_flash_sale,
        voucher_id,
        discount_amount,
        shipping_fee,
        fulfillment_method,
        payment_method,
        order_status,
        payment_status
    )
    SELECT
        p.order_date,
        p.user_id,
        p.recipient_name,
        p.recipient_phone,
        p.sku_id,
        p.quantity,
        p.original_unit_price,
        p.sale_unit_price,
        p.is_flash_sale,
        p.voucher_id,
        CAST(CASE
            WHEN p.voucher_id = @VoucherPercentId
                THEN IIF(p.sale_unit_price * p.quantity * 0.10 > 500000,
                         500000,
                         p.sale_unit_price * p.quantity * 0.10)
            WHEN p.voucher_id = @VoucherAmountId THEN 300000
            ELSE 0
        END AS DECIMAL(18,2)),
        p.shipping_fee,
        p.fulfillment_method,
        p.payment_method,
        p.order_status,
        CASE
            WHEN p.order_status = N'COMPLETED' THEN N'PAID'
            WHEN p.payment_method = N'VNPAY' AND p.seed_hash % 100 IN (84, 85)
                THEN N'REFUNDED'
            WHEN p.payment_method = N'VNPAY' THEN N'FAILED'
            ELSE N'UNPAID'
        END
    FROM Prepared p
    OPTION (MAXRECURSION 2000);

    CREATE TABLE #InsertedOrders (
        order_id INT NOT NULL PRIMARY KEY,
        seed_no INT NOT NULL UNIQUE,
        order_date DATETIME2 NOT NULL,
        final_amount DECIMAL(18,2) NOT NULL,
        payment_method NVARCHAR(50) NOT NULL,
        order_status NVARCHAR(50) NOT NULL,
        payment_status NVARCHAR(50) NOT NULL
    );

    /* Marcus thêm: dùng MERGE chỉ để lấy mapping seed_no -> order_id sau insert.
       Nguồn và đích đều giới hạn trong dữ liệu ANL-SEED của transaction hiện tại. */
    MERGE INTO dbo.Orders AS target
    USING (
        SELECT
            s.seed_no,
            s.user_id,
            s.voucher_id,
            CONCAT(
                @SeedPrefix,
                CONVERT(CHAR(8), CAST(s.order_date AS DATE), 112),
                '-',
                RIGHT('000' + CAST(s.seed_no AS VARCHAR(10)), 3)
            ) AS order_code,
            s.recipient_name,
            s.recipient_phone,
            CASE
                WHEN s.fulfillment_method = 'STORE_PICKUP'
                    THEN N'Nhận tại Marcus Store - 118 Cát Bi, Hải An, Hải Phòng'
                ELSE N'Địa chỉ giao hàng lịch sử phục vụ dữ liệu phân tích'
            END AS shipping_address,
            s.sale_unit_price * s.quantity AS total_amount,
            s.discount_amount,
            s.sale_unit_price * s.quantity - s.discount_amount + s.shipping_fee AS final_amount,
            s.payment_method,
            s.payment_status,
            s.order_status,
            s.order_date
        FROM #SeedOrders s
    ) AS source
        ON 1 = 0
    WHEN NOT MATCHED THEN
        INSERT (
            user_id,
            voucher_id,
            order_code,
            recipient_name,
            recipient_phone,
            shipping_address,
            total_amount,
            discount_amount,
            final_amount,
            payment_method,
            payment_status,
            order_status,
            created_at,
            updated_at,
            payment_date
        )
        VALUES (
            source.user_id,
            source.voucher_id,
            source.order_code,
            source.recipient_name,
            source.recipient_phone,
            source.shipping_address,
            source.total_amount,
            source.discount_amount,
            source.final_amount,
            source.payment_method,
            source.payment_status,
            source.order_status,
            source.order_date,
            source.order_date,
            CASE WHEN source.payment_status IN (N'PAID', N'REFUNDED')
                THEN DATEADD(MINUTE, 5, source.order_date) ELSE NULL END
        )
    OUTPUT
        inserted.order_id,
        source.seed_no,
        source.order_date,
        inserted.final_amount,
        source.payment_method,
        source.order_status,
        source.payment_status
    INTO #InsertedOrders (
        order_id,
        seed_no,
        order_date,
        final_amount,
        payment_method,
        order_status,
        payment_status
    );

    /* Marcus sửa: snapshot giao nhận của dữ liệu seed nằm ở bảng chuyên biệt,
       không tiếp tục làm phình Orders. */
    INSERT INTO dbo.Order_Shipping_Details (
        order_id, fulfillment_method, shipping_fee, shipping_subsidy,
        customer_shipping_fee, ghn_integration_status, ghn_retry_count
    )
    SELECT
        inserted.order_id,
        seed.fulfillment_method,
        seed.shipping_fee,
        0,
        seed.shipping_fee,
        CASE WHEN seed.fulfillment_method = 'STORE_PICKUP' THEN 'NOT_REQUIRED' ELSE 'SUCCESS' END,
        0
    FROM #InsertedOrders inserted
    INNER JOIN #SeedOrders seed ON seed.seed_no = inserted.seed_no;

    /* Marcus thêm: dữ liệu hủy seed có lý do cấu trúc để Analytics đọc đúng
       nguồn mới; không sửa timeline/module đơn hàng của thành viên. */
    INSERT INTO dbo.Order_Cancellations (order_id, reason_code, actor_type, detail, cancelled_at)
    SELECT
        inserted.order_id,
        CASE inserted.seed_no % 5
            WHEN 0 THEN 'CUSTOMER_WRONG_ITEM'
            WHEN 1 THEN 'CUSTOMER_CHANGE_ADDRESS'
            WHEN 2 THEN 'CUSTOMER_DELIVERY_TIME'
            WHEN 3 THEN 'SYSTEM_VNPAY_EXPIRED'
            ELSE 'ADMIN_CANNOT_CONTACT'
        END,
        CASE WHEN inserted.seed_no % 5 = 3 THEN 'SYSTEM'
             WHEN inserted.seed_no % 5 = 4 THEN 'ADMIN'
             ELSE 'CUSTOMER' END,
        N'Dữ liệu lịch sử tổng hợp phục vụ Analytics.',
        DATEADD(MINUTE, 30, inserted.order_date)
    FROM #InsertedOrders inserted
    WHERE inserted.order_status = N'CANCELLED';

    INSERT INTO dbo.Order_Items (
        order_id,
        sku_id,
        quantity,
        price_at_purchase,
        is_flash_sale,
        original_price,
        flash_sale_slot_name,
        flash_sale_slot_id
    )
    SELECT
        inserted.order_id,
        seed.sku_id,
        seed.quantity,
        seed.sale_unit_price,
        seed.is_flash_sale,
        seed.original_unit_price,
        CASE WHEN seed.is_flash_sale = 1
            THEN N'Flash Sale lịch sử - dữ liệu phân tích' ELSE NULL END,
        NULL
    FROM #InsertedOrders inserted
    INNER JOIN #SeedOrders seed ON seed.seed_no = inserted.seed_no;

    /* Marcus làm: đơn hoàn tất luôn có đúng một giao dịch thu SUCCESS.
       Giao dịch seed có idempotency_key riêng, không xung đột giao dịch thật. */
    INSERT INTO dbo.Order_Transactions (
        order_id,
        amount,
        type,
        status,
        note,
        created_at,
        is_reconciled,
        idempotency_key,
        provider_transaction_id,
        provider_response_code,
        provider_transaction_date
    )
    SELECT
        o.order_id,
        o.final_amount,
        CASE WHEN o.payment_method = N'VNPAY'
            THEN 'VNPAY_PAYMENT' ELSE 'COD_COLLECTION' END,
        'SUCCESS',
        N'Marcus thêm: giao dịch lịch sử phục vụ tab Phân tích.',
        DATEADD(MINUTE, 5, o.order_date),
        1,
        CONCAT('ANL-SEED-PAYMENT-', o.order_id),
        CONCAT('ANL', o.order_id),
        '00',
        FORMAT(DATEADD(MINUTE, 5, o.order_date), 'yyyyMMddHHmmss')
    FROM #InsertedOrders o
    WHERE o.order_status = N'COMPLETED';

    /* Marcus thêm: một tỷ lệ nhỏ đơn VNPAY đã thanh toán rồi hủy được hoàn
       thành công, giúp biểu đồ refund có dữ liệu nhưng không tạo trạng thái treo. */
    INSERT INTO dbo.Order_Transactions (
        order_id,
        amount,
        type,
        status,
        note,
        created_at,
        is_reconciled,
        idempotency_key,
        provider_transaction_id,
        provider_response_code,
        provider_transaction_date
    )
    SELECT
        o.order_id,
        o.final_amount,
        tx.transaction_type,
        'SUCCESS',
        tx.note,
        DATEADD(MINUTE, tx.minute_offset, o.order_date),
        1,
        CONCAT('ANL-SEED-', tx.key_name, '-', o.order_id),
        CONCAT('ANL', tx.key_name, o.order_id),
        '00',
        FORMAT(DATEADD(MINUTE, tx.minute_offset, o.order_date), 'yyyyMMddHHmmss')
    FROM #InsertedOrders o
    CROSS APPLY (
        VALUES
            ('VNPAY_PAYMENT', 'PAID', 5, N'Marcus thêm: khoản thu VNPAY trước khi khách hủy đơn.'),
            ('REFUND', 'REFUND', 60, N'Marcus thêm: hoàn tiền thành công cho đơn lịch sử đã hủy.')
    ) tx(transaction_type, key_name, minute_offset, note)
    WHERE o.order_status = N'CANCELLED'
      AND o.payment_status = N'REFUNDED';

    DECLARE @SeededOrders INT = (
        SELECT COUNT(*) FROM dbo.Orders WHERE order_code LIKE @SeedPrefix + '%'
    );
    DECLARE @CompletedOrders INT = (
        SELECT COUNT(*) FROM dbo.Orders
        WHERE order_code LIKE @SeedPrefix + '%'
          AND order_status = N'COMPLETED'
    );
    DECLARE @CancelledOrders INT = @SeededOrders - @CompletedOrders;
    DECLARE @SeededProducts INT = (
        SELECT COUNT(DISTINCT sku.product_id)
        FROM dbo.Orders o
        INNER JOIN dbo.Order_Items oi ON oi.order_id = o.order_id
        INNER JOIN dbo.Product_Skus sku ON sku.sku_id = oi.sku_id
        WHERE o.order_code LIKE @SeedPrefix + '%'
    );

    /* Marcus kiểm tra: seed không được tạo đơn hoàn tất thiếu IMEI cho SKU
       vốn đang được quản lý theo IMEI. */
    IF EXISTS (
        SELECT 1
        FROM dbo.Orders o
        INNER JOIN dbo.Order_Items oi ON oi.order_id = o.order_id
        WHERE o.order_code LIKE @SeedPrefix + '%'
          AND o.order_status = N'COMPLETED'
          AND EXISTS (
              SELECT 1 FROM dbo.Product_Items item WHERE item.sku_id = oi.sku_id
          )
    )
    BEGIN
        THROW 51004, N'Seed đã chạm SKU quản lý IMEI; transaction được rollback.', 1;
    END;

    COMMIT TRANSACTION;

    SELECT
        @StartDate AS from_date,
        @EndDate AS to_date,
        @SeededOrders AS seeded_orders,
        @CompletedOrders AS completed_orders,
        @CancelledOrders AS cancelled_orders,
        @SeededProducts AS covered_products,
        CAST(0 AS INT) AS changed_stock_rows,
        CAST(0 AS INT) AS changed_imei_rows,
        N'Thành công. Dữ liệu kho và IMEI không bị thay đổi.' AS note;

    SELECT
        YEAR(o.created_at) AS report_year,
        MONTH(o.created_at) AS report_month,
        COUNT(*) AS total_orders,
        SUM(CASE WHEN o.order_status = N'COMPLETED' THEN 1 ELSE 0 END) AS completed_orders,
        SUM(CASE WHEN o.order_status = N'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_orders,
        SUM(CASE WHEN o.order_status = N'COMPLETED' THEN o.final_amount ELSE 0 END)
            AS completed_sales
    FROM dbo.Orders o
    WHERE o.order_code LIKE @SeedPrefix + '%'
    GROUP BY YEAR(o.created_at), MONTH(o.created_at)
    ORDER BY report_year, report_month;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
GO
