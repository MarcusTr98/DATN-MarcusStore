SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

DECLARE @OrderCode VARCHAR(50) = 'ORD-GHN-WEBHOOK-TEST';
DECLARE @TrackingCode VARCHAR(100) = 'GHN-POSTMAN-COD-TEST-001';
DECLARE @OrderId INT;
DECLARE @UserId INT;
DECLARE @SkuId INT;
DECLARE @Price DECIMAL(18,2);

SELECT TOP (1) @UserId = user_id
FROM Carts
ORDER BY cart_id;

IF @UserId IS NULL
    SELECT TOP (1) @UserId = user_id FROM Users ORDER BY user_id;

SELECT TOP (1)
    @SkuId = sku_id,
    @Price = price
FROM Product_Skus
WHERE is_active = 1 AND stock_quantity > 0
ORDER BY sku_id;

IF @UserId IS NULL
    THROW 50001, N'Không có user để tạo đơn test.', 1;

IF @SkuId IS NULL
    THROW 50002, N'Không có SKU active còn tồn kho để tạo đơn test.', 1;

SELECT @OrderId = order_id
FROM Orders WITH (UPDLOCK, HOLDLOCK)
WHERE order_code = @OrderCode;

IF @OrderId IS NULL
BEGIN
    INSERT INTO Orders (
        user_id, voucher_id, order_code,
        recipient_name, recipient_phone, shipping_address,
        total_amount, discount_amount, final_amount,
        payment_method, payment_status, order_status,
        shipping_fee, shipping_subsidy, tracking_code,
        payment_date, to_district_id, to_ward_code, delivery_note, is_hidden
    ) VALUES (
        @UserId, NULL, @OrderCode,
        N'Khách Test GHN', '0900000001', N'Địa chỉ test webhook GHN',
        @Price, 0, @Price,
        'COD', 'UNPAID', 'SHIPPING',
        0, 0, @TrackingCode,
        NULL, 1526, '20310', N'Đơn seed phục vụ test webhook', 0
    );

    SET @OrderId = SCOPE_IDENTITY();

    INSERT INTO Order_Items (order_id, sku_id, quantity, price_at_purchase)
    VALUES (@OrderId, @SkuId, 1, @Price);

    UPDATE Product_Skus
    SET stock_quantity = stock_quantity - 1
    WHERE sku_id = @SkuId;

    INSERT INTO Order_Transactions (order_id, amount, type, status, note, is_reconciled)
    VALUES (@OrderId, @Price, 'COD_COLLECTION', 'PENDING',
            N'Seed giao dịch COD chờ GHN delivered', 0);

    INSERT INTO Order_Status_History (order_id, status, title, note, created_by)
    VALUES (@OrderId, 'SHIPPING', N'Đơn hàng đang được giao',
            N'Seed phục vụ test webhook GHN', NULL);
END
ELSE
BEGIN
    UPDATE Orders
    SET payment_method = 'COD',
        payment_status = 'UNPAID',
        payment_date = NULL,
        order_status = 'SHIPPING',
        tracking_code = @TrackingCode,
        updated_at = GETDATE()
    WHERE order_id = @OrderId;

    UPDATE Order_Transactions
    SET status = 'PENDING',
        note = N'Reset giao dịch COD chờ GHN delivered',
        is_reconciled = 0
    WHERE order_id = @OrderId AND type = 'COD_COLLECTION';

    IF NOT EXISTS (
        SELECT 1 FROM Order_Transactions
        WHERE order_id = @OrderId AND type = 'COD_COLLECTION'
    )
    BEGIN
        INSERT INTO Order_Transactions (order_id, amount, type, status, note, is_reconciled)
        SELECT @OrderId, final_amount, 'COD_COLLECTION', 'PENDING',
               N'Reset giao dịch COD chờ GHN delivered', 0
        FROM Orders WHERE order_id = @OrderId;
    END
END;

COMMIT TRANSACTION;

SELECT
    o.order_id, o.order_code, o.payment_method, o.payment_status,
    o.order_status, o.final_amount, o.tracking_code,
    t.transaction_id, t.type, t.status AS transaction_status
FROM Orders o
JOIN Order_Transactions t ON t.order_id = o.order_id
WHERE o.order_code = @OrderCode AND t.type = 'COD_COLLECTION';
