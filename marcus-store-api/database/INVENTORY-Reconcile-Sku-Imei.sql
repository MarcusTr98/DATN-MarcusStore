/*
 * Marcus làm: đồng bộ tồn kho SKU với IMEI cho dữ liệu hiện có.
 *
 * Quy tắc:
 *   - SKU chưa từng có Product_Items: quản lý theo số lượng, giữ nguyên stock_quantity.
 *   - SKU đã có Product_Items: quản lý theo IMEI, stock_quantity = số IMEI status = 1.
 *   - Order_Item COMPLETED còn thiếu IMEI được ưu tiên nhận IMEI status=2 đang
 *     rời cùng SKU. Nếu không còn, xuất một IMEI status=1 đúng SKU sang status=2.
 *     Script không tự sinh hoặc đổi mã IMEI.
 *
 * Script có thể chạy lại: lần sau sẽ không cập nhật khi dữ liệu đã khớp.
 * Mọi thay đổi được lưu trong hai bảng log để có thể đối chiếu/khôi phục.
 */
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.Inventory_Reconciliation_Runs', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.Inventory_Reconciliation_Runs (
            run_id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
            executed_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
            tracked_skus INT NOT NULL,
            updated_skus INT NOT NULL,
            relinked_imeis INT NOT NULL,
            unresolved_completed_units INT NOT NULL,
            note NVARCHAR(500) NULL
        );
    END;

    IF OBJECT_ID(N'dbo.Inventory_Stock_Reconciliation_Log', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.Inventory_Stock_Reconciliation_Log (
            log_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            run_id UNIQUEIDENTIFIER NOT NULL,
            sku_id INT NOT NULL,
            old_stock_quantity INT NOT NULL,
            new_stock_quantity INT NOT NULL,
            total_imeis INT NOT NULL,
            available_imeis INT NOT NULL,
            sold_imeis INT NOT NULL,
            warranty_imeis INT NOT NULL,
            logged_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
            CONSTRAINT FK_InventoryStockLog_Run
                FOREIGN KEY (run_id) REFERENCES dbo.Inventory_Reconciliation_Runs(run_id),
            CONSTRAINT FK_InventoryStockLog_Sku
                FOREIGN KEY (sku_id) REFERENCES dbo.Product_Skus(sku_id)
        );
    END;

    IF OBJECT_ID(N'dbo.Inventory_Imei_Reconciliation_Log', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.Inventory_Imei_Reconciliation_Log (
            log_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            run_id UNIQUEIDENTIFIER NOT NULL,
            item_id INT NOT NULL,
            sku_id INT NOT NULL,
            old_order_item_id INT NULL,
            new_order_item_id INT NOT NULL,
            old_status INT NULL,
            new_status INT NULL,
            logged_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
            CONSTRAINT FK_InventoryImeiLog_Run
                FOREIGN KEY (run_id) REFERENCES dbo.Inventory_Reconciliation_Runs(run_id),
            CONSTRAINT FK_InventoryImeiLog_Item
                FOREIGN KEY (item_id) REFERENCES dbo.Product_Items(item_id)
        );
    END;

    -- Marcus thêm: nâng bảng log đã tạo bởi phiên bản script đầu tiên.
    IF COL_LENGTH('dbo.Inventory_Imei_Reconciliation_Log', 'old_status') IS NULL
        ALTER TABLE dbo.Inventory_Imei_Reconciliation_Log ADD old_status INT NULL;

    IF COL_LENGTH('dbo.Inventory_Imei_Reconciliation_Log', 'new_status') IS NULL
        ALTER TABLE dbo.Inventory_Imei_Reconciliation_Log ADD new_status INT NULL;

    DECLARE @runId UNIQUEIDENTIFIER = NEWID();
    DECLARE @trackedSkus INT;
    DECLARE @updatedSkus INT = 0;
    DECLARE @relinkedImeis INT = 0;
    DECLARE @unresolvedCompletedUnits INT = 0;

    SELECT @trackedSkus = COUNT(*)
    FROM dbo.Product_Skus sku
    WHERE EXISTS (
        SELECT 1
        FROM dbo.Product_Items item
        WHERE item.sku_id = sku.sku_id
    );

    -- Marcus thêm: tạo bản ghi run trước để các bảng log giữ được khóa ngoại.
    INSERT INTO dbo.Inventory_Reconciliation_Runs (
        run_id,
        tracked_skus,
        updated_skus,
        relinked_imeis,
        unresolved_completed_units,
        note
    )
    VALUES (
        @runId,
        @trackedSkus,
        0,
        0,
        0,
        N'Đang thực hiện đồng bộ SKU và IMEI'
    );

    /*
     * Marcus sửa: nối lại IMEI đã bán nhưng đang rời vào dòng đơn COMPLETED
     * còn thiếu IMEI. ROW_NUMBER ghép theo đúng sku_id, không ghép chéo SKU.
     */
    IF OBJECT_ID('tempdb..#MissingImeiSlots') IS NOT NULL
        DROP TABLE #MissingImeiSlots;

    ;WITH OrderItemNeed AS (
        SELECT
            order_item.order_item_id,
            order_item.sku_id,
            order_item.quantity
                - COUNT(product_item.item_id) AS missing_quantity
        FROM dbo.Order_Items order_item
        INNER JOIN dbo.Orders customer_order
            ON customer_order.order_id = order_item.order_id
           AND customer_order.order_status = 'COMPLETED'
        LEFT JOIN dbo.Product_Items product_item
            ON product_item.order_item_id = order_item.order_item_id
        WHERE EXISTS (
            SELECT 1
            FROM dbo.Product_Items tracked_item
            WHERE tracked_item.sku_id = order_item.sku_id
        )
        GROUP BY
            order_item.order_item_id,
            order_item.sku_id,
            order_item.quantity
        HAVING order_item.quantity > COUNT(product_item.item_id)
    ),
    Tally AS (
        SELECT TOP (1000)
            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS number
        FROM sys.all_objects first_source
        CROSS JOIN sys.all_objects second_source
    )
    SELECT
        need.order_item_id,
        need.sku_id,
        ROW_NUMBER() OVER (
            PARTITION BY need.sku_id
            ORDER BY need.order_item_id, tally.number
        ) AS sku_slot_number
    INTO #MissingImeiSlots
    FROM OrderItemNeed need
    INNER JOIN Tally tally
        ON tally.number <= need.missing_quantity;

    IF OBJECT_ID('tempdb..#ImeiRelinkMap') IS NOT NULL
        DROP TABLE #ImeiRelinkMap;

    ;WITH CandidateImei AS (
        SELECT
            item.item_id,
            item.sku_id,
            item.status AS old_status,
            ROW_NUMBER() OVER (
                PARTITION BY item.sku_id
                -- Marcus sửa: dùng IMEI đã bán đang rời trước, chỉ xuất kho mới
                -- khi cùng SKU không còn IMEI status=2 phù hợp.
                ORDER BY
                    CASE WHEN item.status = 2 THEN 0 ELSE 1 END,
                    item.updated_at,
                    item.created_at,
                    item.item_id
            ) AS sku_slot_number
        FROM dbo.Product_Items item
        WHERE item.status IN (1, 2)
          AND item.order_item_id IS NULL
    )
    SELECT
        candidate.item_id,
        candidate.sku_id,
        candidate.old_status,
        missing.order_item_id,
        missing.sku_slot_number
    INTO #ImeiRelinkMap
    FROM CandidateImei candidate
    INNER JOIN #MissingImeiSlots missing
        ON missing.sku_id = candidate.sku_id
       AND missing.sku_slot_number = candidate.sku_slot_number;

    INSERT INTO dbo.Inventory_Imei_Reconciliation_Log (
        run_id,
        item_id,
        sku_id,
        old_order_item_id,
        new_order_item_id,
        old_status,
        new_status
    )
    SELECT
        @runId,
        mapping.item_id,
        mapping.sku_id,
        NULL,
        mapping.order_item_id,
        mapping.old_status,
        2
    FROM #ImeiRelinkMap mapping;

    UPDATE product_item
    SET
        product_item.order_item_id = mapping.order_item_id,
        product_item.status = 2,
        product_item.updated_at = SYSDATETIME()
    FROM dbo.Product_Items product_item
    INNER JOIN #ImeiRelinkMap mapping
        ON mapping.item_id = product_item.item_id
    WHERE product_item.status IN (1, 2)
      AND product_item.order_item_id IS NULL;

    SET @relinkedImeis = @@ROWCOUNT;

    /*
     * Marcus sửa: với SKU quản lý IMEI, tồn có thể bán chính là số IMEI status=1.
     * Ghi log trước rồi mới cập nhật để có thể khôi phục theo run_id.
     */
    IF OBJECT_ID('tempdb..#TrackedSkuStock') IS NOT NULL
        DROP TABLE #TrackedSkuStock;

    SELECT
        sku.sku_id,
        sku.stock_quantity AS old_stock_quantity,
        COUNT(product_item.item_id) AS total_imeis,
        SUM(CASE WHEN product_item.status = 1 THEN 1 ELSE 0 END) AS available_imeis,
        SUM(CASE WHEN product_item.status = 2 THEN 1 ELSE 0 END) AS sold_imeis,
        SUM(CASE WHEN product_item.status = 3 THEN 1 ELSE 0 END) AS warranty_imeis
    INTO #TrackedSkuStock
    FROM dbo.Product_Skus sku
    INNER JOIN dbo.Product_Items product_item
        ON product_item.sku_id = sku.sku_id
    GROUP BY
        sku.sku_id,
        sku.stock_quantity;

    INSERT INTO dbo.Inventory_Stock_Reconciliation_Log (
        run_id,
        sku_id,
        old_stock_quantity,
        new_stock_quantity,
        total_imeis,
        available_imeis,
        sold_imeis,
        warranty_imeis
    )
    SELECT
        @runId,
        tracked.sku_id,
        tracked.old_stock_quantity,
        tracked.available_imeis,
        tracked.total_imeis,
        tracked.available_imeis,
        tracked.sold_imeis,
        tracked.warranty_imeis
    FROM #TrackedSkuStock tracked
    WHERE tracked.old_stock_quantity <> tracked.available_imeis;

    UPDATE sku
    SET sku.stock_quantity = tracked.available_imeis
    FROM dbo.Product_Skus sku
    INNER JOIN #TrackedSkuStock tracked
        ON tracked.sku_id = sku.sku_id
    WHERE sku.stock_quantity <> tracked.available_imeis;

    SET @updatedSkus = @@ROWCOUNT;

    SELECT
        @unresolvedCompletedUnits = COUNT(*)
    FROM #MissingImeiSlots missing
    WHERE NOT EXISTS (
        SELECT 1
        FROM #ImeiRelinkMap mapping
        WHERE mapping.sku_id = missing.sku_id
          AND mapping.sku_slot_number = missing.sku_slot_number
    );

    -- Marcus thêm: chặn commit nếu vẫn còn SKU có IMEI mà tồn kho chưa khớp.
    IF EXISTS (
        SELECT 1
        FROM dbo.Product_Skus sku
        CROSS APPLY (
            SELECT COUNT(*) AS available_imeis
            FROM dbo.Product_Items item
            WHERE item.sku_id = sku.sku_id
              AND item.status = 1
        ) imei
        WHERE EXISTS (
            SELECT 1
            FROM dbo.Product_Items tracked_item
            WHERE tracked_item.sku_id = sku.sku_id
        )
          AND sku.stock_quantity <> imei.available_imeis
    )
    BEGIN
        THROW 51001, N'Đồng bộ thất bại: vẫn còn SKU có tồn kho lệch IMEI.', 1;
    END;

    UPDATE dbo.Inventory_Reconciliation_Runs
    SET
        updated_skus = @updatedSkus,
        relinked_imeis = @relinkedImeis,
        unresolved_completed_units = @unresolvedCompletedUnits,
        note = N'Đồng bộ thành công. SKU số lượng thường không bị thay đổi.'
    WHERE run_id = @runId;

    COMMIT TRANSACTION;

    SELECT
        run_id,
        executed_at,
        tracked_skus,
        updated_skus,
        relinked_imeis,
        unresolved_completed_units,
        note
    FROM dbo.Inventory_Reconciliation_Runs
    WHERE run_id = @runId;

    SELECT
        product.product_name,
        sku.sku_code,
        stock_log.old_stock_quantity,
        stock_log.new_stock_quantity,
        stock_log.total_imeis,
        stock_log.available_imeis,
        stock_log.sold_imeis,
        stock_log.warranty_imeis
    FROM dbo.Inventory_Stock_Reconciliation_Log stock_log
    INNER JOIN dbo.Product_Skus sku
        ON sku.sku_id = stock_log.sku_id
    INNER JOIN dbo.Products product
        ON product.product_id = sku.product_id
    WHERE stock_log.run_id = @runId
    ORDER BY product.product_name, sku.sku_code;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;

/*
 * Marcus thêm: mẫu khôi phục một lần chạy nếu cần.
 * Thay @runIdToRestore bằng run_id cần khôi phục, kiểm tra SELECT rồi mới COMMIT.
 *
 * DECLARE @runIdToRestore UNIQUEIDENTIFIER = '...';
 * BEGIN TRANSACTION;
 *
 * UPDATE sku
 * SET sku.stock_quantity = stock_log.old_stock_quantity
 * FROM dbo.Product_Skus sku
 * INNER JOIN dbo.Inventory_Stock_Reconciliation_Log stock_log
 *     ON stock_log.sku_id = sku.sku_id
 * WHERE stock_log.run_id = @runIdToRestore;
 *
 * UPDATE item
 * SET
 *     item.order_item_id = imei_log.old_order_item_id,
 *     item.status = COALESCE(imei_log.old_status, item.status)
 * FROM dbo.Product_Items item
 * INNER JOIN dbo.Inventory_Imei_Reconciliation_Log imei_log
 *     ON imei_log.item_id = item.item_id
 * WHERE imei_log.run_id = @runIdToRestore;
 *
 * -- COMMIT TRANSACTION;
 * ROLLBACK TRANSACTION;
 */
