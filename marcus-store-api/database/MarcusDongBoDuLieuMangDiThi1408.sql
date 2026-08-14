USE MarcusStoreDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/*
 Marcus làm: đồng bộ dữ liệu demo trước khi mang đi bảo vệ.
 - Không thay đổi cấu trúc bảng.
 - Chạy lặp lại an toàn: lần sau không sinh thêm IMEI nếu đơn đã đủ.
 - Phải backup .bak trước khi chạy.
*/

BEGIN TRY
    BEGIN TRANSACTION;

    /* =========================================================
       C. Chuẩn hóa mã IMEI seed AUTO-* thành chuỗi số 15 ký tự.
       Prefix 98 dùng để nhận biết dữ liệu demo đã chuyển đổi.
       ========================================================= */
    UPDATE pi
       SET imei_code = '98' + RIGHT(REPLICATE('0', 13) + CAST(pi.item_id AS VARCHAR(13)), 13),
           note = CONCAT(COALESCE(NULLIF(pi.note, ''), N''),
                         CASE WHEN NULLIF(pi.note, '') IS NULL THEN N'' ELSE N' | ' END,
                         N'Marcus đồng bộ IMEI seed AUTO sang định dạng số để demo')
    FROM dbo.Product_Items pi
    WHERE pi.imei_code LIKE 'AUTO-%';

    /* =========================================================
       B. Gắn IMEI SOLD còn rời vào đơn lịch sử đã giao/hoàn thành.
       Nếu dữ liệu seed thiếu IMEI, sinh IMEI demo 15 số prefix 96.
       ========================================================= */
    CREATE TABLE #FulfilledDemand (
        demand_id INT IDENTITY(1,1) PRIMARY KEY,
        order_item_id INT NOT NULL,
        sku_id INT NOT NULL,
        unit_no INT NOT NULL,
        sku_row_no INT NULL
    );

    INSERT INTO #FulfilledDemand(order_item_id, sku_id, unit_no)
    SELECT oi.order_item_id, oi.sku_id, units.unit_no
    FROM dbo.Order_Items oi
    JOIN dbo.Orders o ON o.order_id = oi.order_id
    JOIN dbo.Product_Skus sku ON sku.sku_id = oi.sku_id
    JOIN dbo.Products p ON p.product_id = sku.product_id AND p.status_imei = 1
    CROSS APPLY (
        SELECT TOP (
            CASE WHEN oi.quantity > (
                SELECT COUNT(*) FROM dbo.Product_Items linked
                WHERE linked.order_item_id = oi.order_item_id
            ) THEN oi.quantity - (
                SELECT COUNT(*) FROM dbo.Product_Items linked
                WHERE linked.order_item_id = oi.order_item_id
            ) ELSE 0 END
        ) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS unit_no
        FROM sys.all_objects
    ) units
    WHERE o.order_status IN ('DELIVERED', 'COMPLETED');

    ;WITH RankedDemand AS (
        SELECT demand_id,
               ROW_NUMBER() OVER (PARTITION BY sku_id ORDER BY order_item_id, unit_no) AS rn
        FROM #FulfilledDemand
    )
    UPDATE d SET sku_row_no = r.rn
    FROM #FulfilledDemand d JOIN RankedDemand r ON r.demand_id = d.demand_id;

    ;WITH SoldSupply AS (
        SELECT pi.item_id, pi.sku_id,
               ROW_NUMBER() OVER (PARTITION BY pi.sku_id ORDER BY pi.item_id) AS rn
        FROM dbo.Product_Items pi
        WHERE pi.status = 2 AND pi.order_item_id IS NULL
    )
    UPDATE pi
       SET order_item_id = d.order_item_id,
           note = CONCAT(COALESCE(NULLIF(pi.note, ''), N''),
                         CASE WHEN NULLIF(pi.note, '') IS NULL THEN N'' ELSE N' | ' END,
                         N'Marcus liên kết IMEI seed với đơn lịch sử')
    FROM dbo.Product_Items pi
    JOIN SoldSupply s ON s.item_id = pi.item_id
    JOIN #FulfilledDemand d ON d.sku_id = s.sku_id AND d.sku_row_no = s.rn;

    DELETE FROM #FulfilledDemand;

    -- Tính lại phần còn thiếu sau khi đã dùng hết IMEI SOLD rời.
    INSERT INTO #FulfilledDemand(order_item_id, sku_id, unit_no)
    SELECT oi.order_item_id, oi.sku_id, units.unit_no
    FROM dbo.Order_Items oi
    JOIN dbo.Orders o ON o.order_id = oi.order_id
    JOIN dbo.Product_Skus sku ON sku.sku_id = oi.sku_id
    JOIN dbo.Products p ON p.product_id = sku.product_id AND p.status_imei = 1
    CROSS APPLY (
        SELECT TOP (
            CASE WHEN oi.quantity > (
                SELECT COUNT(*) FROM dbo.Product_Items linked
                WHERE linked.order_item_id = oi.order_item_id
            ) THEN oi.quantity - (
                SELECT COUNT(*) FROM dbo.Product_Items linked
                WHERE linked.order_item_id = oi.order_item_id
            ) ELSE 0 END
        ) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS unit_no
        FROM sys.all_objects
    ) units
    WHERE o.order_status IN ('DELIVERED', 'COMPLETED');

    INSERT INTO dbo.Product_Items(sku_id, imei_code, status, order_item_id, created_at, updated_at, note)
    SELECT d.sku_id,
           '96' + RIGHT(REPLICATE('0', 10) + CAST(d.order_item_id AS VARCHAR(10)), 10)
                + RIGHT('000' + CAST(d.unit_no AS VARCHAR(3)), 3),
           2,
           d.order_item_id,
           SYSDATETIME(), SYSDATETIME(),
           N'Marcus sinh IMEI seed hợp lệ để khớp đơn lịch sử demo'
    FROM #FulfilledDemand d
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Product_Items pi
        WHERE pi.imei_code = '96' + RIGHT(REPLICATE('0', 10) + CAST(d.order_item_id AS VARCHAR(10)), 10)
                             + RIGHT('000' + CAST(d.unit_no AS VARCHAR(3)), 3)
    );

    -- Marcus sửa: IMEI mang trạng thái SOLD nhưng không còn chứng từ bán
    -- không được giữ là "Đã bán". Sau khi đã ưu tiên ghép cho đơn lịch sử,
    -- phần dư được trả về kho để có một nguồn tồn duy nhất và có lưu dấu.
    UPDATE dbo.Product_Items
       SET status = 1,
           updated_at = SYSDATETIME(),
           note = CONCAT(COALESCE(NULLIF(note, ''), N''),
                         CASE WHEN NULLIF(note, '') IS NULL THEN N'' ELSE N' | ' END,
                         N'Marcus đồng bộ: trả về IN_STOCK vì không có dòng đơn hàng chứng minh đã bán')
    WHERE status = 2 AND order_item_id IS NULL;

    /* =========================================================
       E1. Hủy các đơn VNPAY seed đã treo quá hạn nhưng scheduler
       cũ chưa xử lý. Hoàn Flash Sale/voucher bằng dữ liệu tổng hợp.
       Tồn SKU được tính lại ở mục A nên không cộng thủ công tại đây.
       ========================================================= */
    CREATE TABLE #ExpiredVnPay(order_id INT PRIMARY KEY);
    INSERT INTO #ExpiredVnPay(order_id)
    SELECT o.order_id
    FROM dbo.Orders o
    WHERE o.payment_method = 'VNPAY'
      AND o.payment_status = 'PENDING'
      AND o.order_status <> 'CANCELLED'
      AND o.created_at < DATEADD(MINUTE, -20, SYSDATETIME());

    ;WITH RestoreFlash AS (
        SELECT oi.flash_sale_slot_id slot_id, oi.sku_id, SUM(oi.quantity) qty
        FROM dbo.Order_Items oi JOIN #ExpiredVnPay e ON e.order_id = oi.order_id
        WHERE oi.is_flash_sale = 1 AND oi.flash_sale_slot_id IS NOT NULL
        GROUP BY oi.flash_sale_slot_id, oi.sku_id
    )
    UPDATE f SET sold_quantity = CASE WHEN f.sold_quantity >= r.qty THEN f.sold_quantity-r.qty ELSE 0 END
    FROM dbo.Flash_Sale_Items f JOIN RestoreFlash r ON r.slot_id=f.slot_id AND r.sku_id=f.sku_id;

    ;WITH RestoreVoucher AS (
        SELECT o.voucher_id, COUNT(*) qty
        FROM dbo.Orders o JOIN #ExpiredVnPay e ON e.order_id=o.order_id
        WHERE o.voucher_id IS NOT NULL GROUP BY o.voucher_id
    )
    UPDATE v SET quantity = v.quantity + r.qty,
                 is_active = CASE WHEN SYSDATETIME() BETWEEN v.start_date AND v.end_date THEN 1 ELSE v.is_active END
    FROM dbo.Vouchers v JOIN RestoreVoucher r ON r.voucher_id=v.voucher_id;

    UPDATE uv SET is_used=0, used_at=NULL
    FROM dbo.User_Vouchers uv
    JOIN dbo.Orders o ON o.user_id=uv.user_id AND o.voucher_id=uv.voucher_id
    JOIN #ExpiredVnPay e ON e.order_id=o.order_id
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Orders other_order
        WHERE other_order.user_id=o.user_id AND other_order.voucher_id=o.voucher_id
          AND other_order.order_id<>o.order_id AND other_order.order_status<>'CANCELLED'
    );

    INSERT INTO dbo.Order_Cancellations(order_id, reason_code, actor_type, detail, cancelled_at)
    SELECT e.order_id, 'SYSTEM_VNPAY_EXPIRED', 'SYSTEM',
           N'Marcus đồng bộ dữ liệu demo: giao dịch VNPAY cũ quá hạn', SYSDATETIME()
    FROM #ExpiredVnPay e
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Order_Cancellations c WHERE c.order_id=e.order_id);

    UPDATE t SET status='FAILED', is_reconciled=0, reconciled_by=NULL, reconciled_at=NULL,
                 note=N'Marcus đồng bộ dữ liệu demo: giao dịch VNPAY quá hạn'
    FROM dbo.Order_Transactions t JOIN #ExpiredVnPay e ON e.order_id=t.order_id
    WHERE t.type='VNPAY_PAYMENT' AND t.status='PENDING';

    UPDATE o SET order_status='CANCELLED', payment_status='FAILED', updated_at=SYSDATETIME()
    FROM dbo.Orders o JOIN #ExpiredVnPay e ON e.order_id=o.order_id;

    -- Marcus thêm: giữ timeline cho đơn VNPAY seed bị hệ thống hủy quá hạn.
    INSERT INTO dbo.Order_Status_History(order_id,status,title,note,created_by,created_at)
    SELECT e.order_id,'CANCELLED',N'Đơn hàng đã hủy',
           N'Hệ thống tự hủy do giao dịch VNPAY quá hạn',NULL,SYSDATETIME()
    FROM #ExpiredVnPay e
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Order_Status_History h
        WHERE h.order_id=e.order_id AND h.status='CANCELLED'
    );

    UPDATE dbo.Orders
       SET payment_status='FAILED', updated_at=SYSDATETIME()
    WHERE order_status='CANCELLED' AND payment_method='VNPAY' AND payment_status='PENDING';

    /* =========================================================
       D. Gắn IMEI cho đơn seed đã thực sự vào khâu xử lý/xuất hàng.
       Ưu tiên IMEI IN_STOCK hiện có; thiếu mới sinh seed prefix 95.
       Không đụng PENDING/CONFIRMED vì các trạng thái đó mới giữ số lượng.
       ========================================================= */
    CREATE TABLE #ActiveDemand (
        demand_id INT IDENTITY(1,1) PRIMARY KEY,
        order_item_id INT NOT NULL,
        sku_id INT NOT NULL,
        unit_no INT NOT NULL,
        sku_row_no INT NULL
    );

    INSERT INTO #ActiveDemand(order_item_id, sku_id, unit_no)
    SELECT oi.order_item_id, oi.sku_id, units.unit_no
    FROM dbo.Order_Items oi
    JOIN dbo.Orders o ON o.order_id=oi.order_id
    JOIN dbo.Product_Skus sku ON sku.sku_id=oi.sku_id
    JOIN dbo.Products p ON p.product_id=sku.product_id AND p.status_imei=1
    CROSS APPLY (
        SELECT TOP (
            CASE WHEN oi.quantity > (SELECT COUNT(*) FROM dbo.Product_Items linked WHERE linked.order_item_id=oi.order_item_id)
                 THEN oi.quantity-(SELECT COUNT(*) FROM dbo.Product_Items linked WHERE linked.order_item_id=oi.order_item_id)
                 ELSE 0 END
        ) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) unit_no FROM sys.all_objects
    ) units
    WHERE o.order_status IN ('PROCESSING','READY_FOR_PICKUP','PACKED','SHIPPING','DELIVERED');

    ;WITH RankedDemand AS (
        SELECT demand_id, ROW_NUMBER() OVER (PARTITION BY sku_id ORDER BY order_item_id,unit_no) rn
        FROM #ActiveDemand
    )
    UPDATE d SET sku_row_no=r.rn FROM #ActiveDemand d JOIN RankedDemand r ON r.demand_id=d.demand_id;

    ;WITH StockSupply AS (
        SELECT pi.item_id,pi.sku_id,ROW_NUMBER() OVER(PARTITION BY pi.sku_id ORDER BY pi.item_id) rn
        FROM dbo.Product_Items pi WHERE pi.status=1 AND pi.order_item_id IS NULL
    )
    UPDATE pi SET status=2, order_item_id=d.order_item_id,
                  note=CONCAT(COALESCE(NULLIF(pi.note,''),N''),CASE WHEN NULLIF(pi.note,'') IS NULL THEN N'' ELSE N' | ' END,
                              N'Marcus gắn IMEI khi đồng bộ đơn demo đang xử lý')
    FROM dbo.Product_Items pi
    JOIN StockSupply s ON s.item_id=pi.item_id
    JOIN #ActiveDemand d ON d.sku_id=s.sku_id AND d.sku_row_no=s.rn;

    DELETE FROM #ActiveDemand;

    INSERT INTO #ActiveDemand(order_item_id, sku_id, unit_no)
    SELECT oi.order_item_id, oi.sku_id, units.unit_no
    FROM dbo.Order_Items oi
    JOIN dbo.Orders o ON o.order_id=oi.order_id
    JOIN dbo.Product_Skus sku ON sku.sku_id=oi.sku_id
    JOIN dbo.Products p ON p.product_id=sku.product_id AND p.status_imei=1
    CROSS APPLY (
        SELECT TOP (
            CASE WHEN oi.quantity > (SELECT COUNT(*) FROM dbo.Product_Items linked WHERE linked.order_item_id=oi.order_item_id)
                 THEN oi.quantity-(SELECT COUNT(*) FROM dbo.Product_Items linked WHERE linked.order_item_id=oi.order_item_id)
                 ELSE 0 END
        ) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) unit_no FROM sys.all_objects
    ) units
    WHERE o.order_status IN ('PROCESSING','READY_FOR_PICKUP','PACKED','SHIPPING','DELIVERED');

    INSERT INTO dbo.Product_Items(sku_id,imei_code,status,order_item_id,created_at,updated_at,note)
    SELECT d.sku_id,
           '95'+RIGHT(REPLICATE('0',10)+CAST(d.order_item_id AS VARCHAR(10)),10)+RIGHT('000'+CAST(d.unit_no AS VARCHAR(3)),3),
           2,d.order_item_id,SYSDATETIME(),SYSDATETIME(),
           N'Marcus sinh IMEI seed hợp lệ cho đơn demo đang xử lý'
    FROM #ActiveDemand d
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Product_Items pi WHERE pi.imei_code=
        '95'+RIGHT(REPLICATE('0',10)+CAST(d.order_item_id AS VARCHAR(10)),10)+RIGHT('000'+CAST(d.unit_no AS VARCHAR(3)),3));

    -- Chuẩn hóa trạng thái tích hợp GHN của đơn giao tận nơi đã đi tới khâu xuất.
    UPDATE sd
       SET ghn_integration_status = CASE
             WHEN o.order_status IN ('SHIPPING','DELIVERED') AND sd.tracking_code IS NOT NULL THEN 'SUCCESS'
             WHEN o.order_status='PACKED' AND sd.tracking_code IS NOT NULL THEN 'CREATED'
             ELSE sd.ghn_integration_status END
    FROM dbo.Order_Shipping_Details sd JOIN dbo.Orders o ON o.order_id=sd.order_id
    WHERE sd.fulfillment_method='DELIVERY';

    /* =========================================================
       A. Product_Skus.stock_quantity là tồn có thể bán:
       IMEI IN_STOCK trừ lượng của đơn đang giữ nhưng chưa gán IMEI.
       ========================================================= */
    ;WITH PhysicalStock AS (
        SELECT sku_id,COUNT(*) qty FROM dbo.Product_Items WHERE status=1 GROUP BY sku_id
    ), Reserved AS (
        SELECT oi.sku_id,SUM(oi.quantity-ISNULL(a.assigned_qty,0)) qty
        FROM dbo.Order_Items oi
        JOIN dbo.Orders o ON o.order_id=oi.order_id
        OUTER APPLY (SELECT COUNT(*) assigned_qty FROM dbo.Product_Items pi WHERE pi.order_item_id=oi.order_item_id) a
        WHERE o.order_status IN ('PENDING','CONFIRMED','PROCESSING','READY_FOR_PICKUP','PACKED','SHIPPING')
        GROUP BY oi.sku_id
    )
    UPDATE sku
       SET stock_quantity = CASE WHEN ISNULL(ps.qty,0)-ISNULL(r.qty,0)>0 THEN ISNULL(ps.qty,0)-ISNULL(r.qty,0) ELSE 0 END
    FROM dbo.Product_Skus sku
    JOIN dbo.Products p ON p.product_id=sku.product_id AND p.status_imei=1
    LEFT JOIN PhysicalStock ps ON ps.sku_id=sku.sku_id
    LEFT JOIN Reserved r ON r.sku_id=sku.sku_id;

    /* =========================================================
       E2. Chuẩn hóa lịch sử dòng tiền seed.
       - PAID phải có giao dịch thu tiền SUCCESS.
       - Giao dịch chưa SUCCESS không được đánh dấu đã đối soát.
       ========================================================= */
    ;WITH Candidate AS (
        SELECT t.transaction_id,
               ROW_NUMBER() OVER(PARTITION BY t.order_id ORDER BY t.created_at DESC,t.transaction_id DESC) rn
        FROM dbo.Order_Transactions t JOIN dbo.Orders o ON o.order_id=t.order_id
        WHERE o.payment_status='PAID'
          AND t.type IN ('VNPAY_PAYMENT','COD_COLLECTION','STORE_PAYMENT')
          AND NOT EXISTS (SELECT 1 FROM dbo.Order_Transactions ok WHERE ok.order_id=o.order_id AND ok.status='SUCCESS'
                          AND ok.type IN ('VNPAY_PAYMENT','COD_COLLECTION','STORE_PAYMENT'))
    )
    UPDATE t SET status='SUCCESS',note=N'Marcus đồng bộ giao dịch thu tiền theo trạng thái PAID của dữ liệu demo'
    FROM dbo.Order_Transactions t JOIN Candidate c ON c.transaction_id=t.transaction_id AND c.rn=1;

    INSERT INTO dbo.Order_Transactions(order_id,amount,type,status,note,created_at,is_reconciled,idempotency_key)
    SELECT o.order_id,o.final_amount,
           CASE WHEN o.payment_method='VNPAY' THEN 'VNPAY_PAYMENT'
                WHEN sd.fulfillment_method='STORE_PICKUP' THEN 'STORE_PAYMENT' ELSE 'COD_COLLECTION' END,
           'SUCCESS',N'Marcus bổ sung giao dịch thu tiền còn thiếu của dữ liệu demo',
           COALESCE(o.payment_date,o.updated_at,o.created_at),0,
           CONCAT('DATA-SYNC-PAID-',o.order_id)
    FROM dbo.Orders o JOIN dbo.Order_Shipping_Details sd ON sd.order_id=o.order_id
    WHERE o.payment_status='PAID'
      AND NOT EXISTS (SELECT 1 FROM dbo.Order_Transactions t WHERE t.order_id=o.order_id AND t.status='SUCCESS'
                      AND t.type IN ('VNPAY_PAYMENT','COD_COLLECTION','STORE_PAYMENT'));

    UPDATE dbo.Order_Transactions
       SET is_reconciled=0,reconciled_by=NULL,reconciled_at=NULL
    WHERE status<>'SUCCESS' AND is_reconciled=1;

    /* =========================================================
       F. Backfill audit Contact seed cũ. handled_by lưu username.
       ========================================================= */
    UPDATE dbo.Contact_Requests
       SET handled_by=COALESCE(handled_by,'admin'),
           processing_started_at=COALESCE(processing_started_at,created_at),
           resolved_at=COALESCE(resolved_at,updated_at,created_at),
           updated_at=COALESCE(updated_at,resolved_at,created_at)
    WHERE status='RESOLVED'
      AND (handled_by IS NULL OR processing_started_at IS NULL OR resolved_at IS NULL);

    COMMIT TRANSACTION;

    /* KẾT QUẢ CUỐI */
    SELECT
      (SELECT COUNT(*) FROM dbo.Product_Items WHERE imei_code LIKE '%[^0-9]%' OR LEN(imei_code)<8 OR LEN(imei_code)>20) invalid_imei,
      (SELECT COUNT(*) FROM dbo.Product_Items WHERE status=2 AND order_item_id IS NULL) sold_without_order,
      (SELECT COUNT(*) FROM dbo.Order_Items oi JOIN dbo.Orders o ON o.order_id=oi.order_id
       JOIN dbo.Product_Skus s ON s.sku_id=oi.sku_id JOIN dbo.Products p ON p.product_id=s.product_id AND p.status_imei=1
       WHERE o.order_status IN ('PROCESSING','READY_FOR_PICKUP','PACKED','SHIPPING','DELIVERED','COMPLETED')
         AND (SELECT COUNT(*) FROM dbo.Product_Items pi WHERE pi.order_item_id=oi.order_item_id)<>oi.quantity) imei_order_mismatch,
      (SELECT COUNT(*) FROM dbo.Order_Transactions WHERE status<>'SUCCESS' AND is_reconciled=1) reconciled_non_success,
      (SELECT COUNT(*) FROM dbo.Contact_Requests WHERE status='RESOLVED' AND (handled_by IS NULL OR resolved_at IS NULL)) contact_missing_audit,
      (SELECT COUNT(*)
       FROM dbo.Product_Skus sku
       JOIN dbo.Products p ON p.product_id=sku.product_id AND p.status_imei=1
       OUTER APPLY (SELECT COUNT(*) physical_qty FROM dbo.Product_Items pi WHERE pi.sku_id=sku.sku_id AND pi.status=1) ps
       OUTER APPLY (
          SELECT ISNULL(SUM(oi.quantity-ISNULL(a.assigned_qty,0)),0) reserved_qty
          FROM dbo.Order_Items oi JOIN dbo.Orders o ON o.order_id=oi.order_id
          OUTER APPLY (SELECT COUNT(*) assigned_qty FROM dbo.Product_Items x WHERE x.order_item_id=oi.order_item_id) a
          WHERE oi.sku_id=sku.sku_id
            AND o.order_status IN ('PENDING','CONFIRMED','PROCESSING','READY_FOR_PICKUP','PACKED','SHIPPING')
       ) r
       WHERE sku.stock_quantity <> CASE WHEN ps.physical_qty-r.reserved_qty>0 THEN ps.physical_qty-r.reserved_qty ELSE 0 END
      ) stock_mismatch;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
