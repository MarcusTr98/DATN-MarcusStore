-- Hợp nhất bảng click AI cũ vào event store chính trước khi xóa bảng phụ.
IF OBJECT_ID('dbo.AI_Product_Clicks', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.Customer_Behavior_Events', 'U') IS NOT NULL
BEGIN
    INSERT INTO dbo.Customer_Behavior_Events(event_type, session_id, product_id, created_at)
    SELECT 'AI_PRODUCT_CLICK', old_click.session_id, old_click.product_id, old_click.clicked_at
    FROM dbo.AI_Product_Clicks old_click
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Customer_Behavior_Events behavior
        WHERE behavior.event_type = 'AI_PRODUCT_CLICK'
          AND behavior.session_id = old_click.session_id
          AND behavior.product_id = old_click.product_id
          AND ABS(DATEDIFF(SECOND, behavior.created_at, old_click.clicked_at)) <= 5
    );

    DROP TABLE dbo.AI_Product_Clicks;
END;
