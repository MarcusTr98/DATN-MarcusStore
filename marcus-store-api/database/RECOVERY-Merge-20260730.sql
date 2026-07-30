USE MarcusStoreDB;
GO

/*
    Marcus cứu dữ liệu 30/07/2026:
    - Nguồn: MarcusStoreDB_Recovery_20260726 (restore side-by-side).
    - Đích: MarcusStoreDB.
    - Chỉ INSERT bản ghi bị thiếu, không xóa và không ghi đè dữ liệu mới.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    SET IDENTITY_INSERT dbo.Permissions ON;
    INSERT dbo.Permissions (permission_id, permission_name, description, module_name)
    SELECT r.permission_id, r.permission_name, r.description, r.module_name
    FROM MarcusStoreDB_Recovery_20260726.dbo.Permissions r
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Permissions c
        WHERE c.permission_id = r.permission_id
           OR c.permission_name = r.permission_name
    );
    SET IDENTITY_INSERT dbo.Permissions OFF;

    INSERT dbo.Role_Permissions (role_id, permission_id)
    SELECT r.role_id, current_permission.permission_id
    FROM MarcusStoreDB_Recovery_20260726.dbo.Role_Permissions r
    INNER JOIN MarcusStoreDB_Recovery_20260726.dbo.Permissions backup_permission
        ON backup_permission.permission_id = r.permission_id
    INNER JOIN dbo.Permissions current_permission
        ON current_permission.permission_name = backup_permission.permission_name
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Role_Permissions c
        WHERE c.role_id = r.role_id
          AND c.permission_id = current_permission.permission_id
    );

    SET IDENTITY_INSERT dbo.Attributes ON;
    INSERT dbo.Attributes (attribute_id, attribute_name)
    SELECT r.attribute_id, r.attribute_name
    FROM MarcusStoreDB_Recovery_20260726.dbo.Attributes r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Attributes c WHERE c.attribute_id = r.attribute_id);
    SET IDENTITY_INSERT dbo.Attributes OFF;

    SET IDENTITY_INSERT dbo.Attribute_Values ON;
    INSERT dbo.Attribute_Values (value_id, attribute_id, value_string, value_meta)
    SELECT r.value_id, r.attribute_id, r.value_string, r.value_meta
    FROM MarcusStoreDB_Recovery_20260726.dbo.Attribute_Values r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Attribute_Values c WHERE c.value_id = r.value_id);
    SET IDENTITY_INSERT dbo.Attribute_Values OFF;

    INSERT dbo.Sku_Attribute_Values (sku_id, value_id)
    SELECT r.sku_id, r.value_id
    FROM MarcusStoreDB_Recovery_20260726.dbo.Sku_Attribute_Values r
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Sku_Attribute_Values c
        WHERE c.sku_id = r.sku_id AND c.value_id = r.value_id
    );

    SET IDENTITY_INSERT dbo.Product_Images ON;
    INSERT dbo.Product_Images (image_id, product_id, image_url, is_primary, display_order)
    SELECT r.image_id, r.product_id, r.image_url, r.is_primary, r.display_order
    FROM MarcusStoreDB_Recovery_20260726.dbo.Product_Images r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Product_Images c WHERE c.image_id = r.image_id);
    SET IDENTITY_INSERT dbo.Product_Images OFF;

    SET IDENTITY_INSERT dbo.Banner_Positions ON;
    INSERT dbo.Banner_Positions (position_id, position_code, description)
    SELECT r.position_id, r.position_code, r.description
    FROM MarcusStoreDB_Recovery_20260726.dbo.Banner_Positions r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Banner_Positions c WHERE c.position_id = r.position_id);
    SET IDENTITY_INSERT dbo.Banner_Positions OFF;

    SET IDENTITY_INSERT dbo.Post_Categories ON;
    INSERT dbo.Post_Categories (post_category_id, name, slug, status)
    SELECT r.post_category_id, r.name, r.slug, r.status
    FROM MarcusStoreDB_Recovery_20260726.dbo.Post_Categories r
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Post_Categories c WHERE c.post_category_id = r.post_category_id
    );
    SET IDENTITY_INSERT dbo.Post_Categories OFF;

    SET IDENTITY_INSERT dbo.Posts ON;
    INSERT dbo.Posts (
        post_id, post_category_id, author_id, title, slug, thumbnail_url,
        excerpt, content, is_published, published_at, created_at, updated_at
    )
    SELECT
        r.post_id, r.post_category_id, r.author_id, r.title, r.slug, r.thumbnail_url,
        r.excerpt, r.content, r.is_published, r.published_at, r.created_at, r.updated_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.Posts r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Posts c WHERE c.post_id = r.post_id);
    SET IDENTITY_INSERT dbo.Posts OFF;

    SET IDENTITY_INSERT dbo.Banners ON;
    INSERT dbo.Banners (
        banner_id, position_id, title, image_url, target_url, display_order,
        is_active, start_date, end_date
    )
    SELECT
        r.banner_id, r.position_id, r.title, r.image_url, r.target_url, r.display_order,
        r.is_active, r.start_date, r.end_date
    FROM MarcusStoreDB_Recovery_20260726.dbo.Banners r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Banners c WHERE c.banner_id = r.banner_id);
    SET IDENTITY_INSERT dbo.Banners OFF;

    INSERT dbo.System_Settings (
        setting_key, setting_value, setting_group, description, updated_at
    )
    SELECT r.setting_key, r.setting_value, r.setting_group, r.description, r.updated_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.System_Settings r
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.System_Settings c WHERE c.setting_key = r.setting_key
    );

    SET IDENTITY_INSERT dbo.Vouchers ON;
    INSERT dbo.Vouchers (
        voucher_id, voucher_code, discount_value, discount_type, max_discount_amount,
        min_order_value, start_date, end_date, quantity, is_active, target_type
    )
    SELECT
        r.voucher_id, r.voucher_code, r.discount_value, r.discount_type,
        r.max_discount_amount, r.min_order_value, r.start_date, r.end_date,
        r.quantity, r.is_active, r.target_type
    FROM MarcusStoreDB_Recovery_20260726.dbo.Vouchers r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Vouchers c WHERE c.voucher_id = r.voucher_id);
    SET IDENTITY_INSERT dbo.Vouchers OFF;

    -- Cart có thể đã được backend tạo lại với cart_id mới. Marcus ánh xạ theo
    -- user_id để không xung đột identity/unique.
    INSERT dbo.Carts (user_id, created_at)
    SELECT r.user_id, r.created_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.Carts r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Carts c WHERE c.user_id = r.user_id);

    INSERT dbo.Cart_Items (
        cart_id, sku_id, quantity, flash_sale_price, flash_sale_slot_id
    )
    SELECT
        current_cart.cart_id, r.sku_id, r.quantity,
        r.flash_sale_price, r.flash_sale_slot_id
    FROM MarcusStoreDB_Recovery_20260726.dbo.Cart_Items r
    INNER JOIN MarcusStoreDB_Recovery_20260726.dbo.Carts backup_cart
        ON backup_cart.cart_id = r.cart_id
    INNER JOIN dbo.Carts current_cart
        ON current_cart.user_id = backup_cart.user_id
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Cart_Items c
        WHERE c.cart_id = current_cart.cart_id AND c.sku_id = r.sku_id
    );

    SET IDENTITY_INSERT dbo.Wishlists ON;
    INSERT dbo.Wishlists (wishlist_id, user_id, product_id, created_at)
    SELECT r.wishlist_id, r.user_id, r.product_id, r.created_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.Wishlists r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Wishlists c WHERE c.wishlist_id = r.wishlist_id);
    SET IDENTITY_INSERT dbo.Wishlists OFF;

    SET IDENTITY_INSERT dbo.User_Addresses ON;
    INSERT dbo.User_Addresses (
        address_id, user_id, recipient_name, phone_number, province_name, district_name,
        ward_name, detail_address, note, is_default, created_at, updated_at,
        latitude, longitude, province_id, district_id, ward_code
    )
    SELECT
        r.address_id, r.user_id, r.recipient_name, r.phone_number, r.province_name,
        r.district_name, r.ward_name, r.detail_address, r.note, r.is_default,
        r.created_at, r.updated_at, r.latitude, r.longitude,
        r.province_id, r.district_id, r.ward_code
    FROM MarcusStoreDB_Recovery_20260726.dbo.User_Addresses r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.User_Addresses c WHERE c.address_id = r.address_id);
    SET IDENTITY_INSERT dbo.User_Addresses OFF;

    SET IDENTITY_INSERT dbo.User_Vouchers ON;
    INSERT dbo.User_Vouchers (
        id, voucher_id, user_id, assigned_at, is_used, used_at, created_at
    )
    SELECT r.id, r.voucher_id, r.user_id, r.assigned_at, r.is_used, r.used_at, r.created_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.User_Vouchers r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.User_Vouchers c WHERE c.id = r.id);
    SET IDENTITY_INSERT dbo.User_Vouchers OFF;

    INSERT dbo.Flash_Sale_Items (
        slot_id, sku_id, original_price, flash_sale_price,
        flash_sale_quantity, sold_quantity, created_at
    )
    SELECT
        r.slot_id, r.sku_id, r.original_price, r.flash_sale_price,
        r.flash_sale_quantity, r.sold_quantity, r.created_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.Flash_Sale_Items r
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Flash_Sale_Items c
        WHERE c.slot_id = r.slot_id AND c.sku_id = r.sku_id
    );

    SET IDENTITY_INSERT dbo.Order_Items ON;
    INSERT dbo.Order_Items (
        order_item_id, order_id, sku_id, quantity, price_at_purchase,
        is_flash_sale, original_price, flash_sale_slot_name, flash_sale_slot_id
    )
    SELECT
        r.order_item_id, r.order_id, r.sku_id, r.quantity, r.price_at_purchase,
        r.is_flash_sale, r.original_price, r.flash_sale_slot_name, r.flash_sale_slot_id
    FROM MarcusStoreDB_Recovery_20260726.dbo.Order_Items r
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Order_Items c WHERE c.order_item_id = r.order_item_id
    );
    SET IDENTITY_INSERT dbo.Order_Items OFF;

    SET IDENTITY_INSERT dbo.Order_Status_History ON;
    INSERT dbo.Order_Status_History (
        history_id, order_id, status, title, note, created_by, created_at
    )
    SELECT
        r.history_id, r.order_id, r.status, r.title, r.note, r.created_by, r.created_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.Order_Status_History r
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Order_Status_History c WHERE c.history_id = r.history_id
    );
    SET IDENTITY_INSERT dbo.Order_Status_History OFF;

    INSERT dbo.User_Permissions (user_id, permission_id)
    SELECT r.user_id, current_permission.permission_id
    FROM MarcusStoreDB_Recovery_20260726.dbo.User_Permissions r
    INNER JOIN MarcusStoreDB_Recovery_20260726.dbo.Permissions backup_permission
        ON backup_permission.permission_id = r.permission_id
    INNER JOIN dbo.Permissions current_permission
        ON current_permission.permission_name = backup_permission.permission_name
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.User_Permissions c
        WHERE c.user_id = r.user_id
          AND c.permission_id = current_permission.permission_id
    );

    SET IDENTITY_INSERT dbo.Audit_Logs ON;
    INSERT dbo.Audit_Logs (
        log_id, user_id, action_type, table_name, description, ip_address, created_at
    )
    SELECT
        r.log_id, r.user_id, r.action_type, r.table_name,
        r.description, r.ip_address, r.created_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.Audit_Logs r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Audit_Logs c WHERE c.log_id = r.log_id);
    SET IDENTITY_INSERT dbo.Audit_Logs OFF;

    SET IDENTITY_INSERT dbo.Comments_Evaluations ON;
    INSERT dbo.Comments_Evaluations (
        review_id, user_id, product_id, order_item_id,
        rating, comment_text, is_approved, created_at
    )
    SELECT
        r.review_id, r.user_id, r.product_id, r.order_item_id,
        r.rating, r.comment_text, r.is_approved, r.created_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.Comments_Evaluations r
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Comments_Evaluations c WHERE c.review_id = r.review_id
    );
    SET IDENTITY_INSERT dbo.Comments_Evaluations OFF;

    SET IDENTITY_INSERT dbo.Review_Images ON;
    INSERT dbo.Review_Images (
        image_id, review_id, image_url, display_order, created_at
    )
    SELECT r.image_id, r.review_id, r.image_url, r.display_order, r.created_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.Review_Images r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Review_Images c WHERE c.image_id = r.image_id);
    SET IDENTITY_INSERT dbo.Review_Images OFF;

    SET IDENTITY_INSERT dbo.Review_Replies ON;
    INSERT dbo.Review_Replies (
        reply_id, review_id, staff_id, reply_text, created_at, updated_at
    )
    SELECT r.reply_id, r.review_id, r.staff_id, r.reply_text, r.created_at, r.updated_at
    FROM MarcusStoreDB_Recovery_20260726.dbo.Review_Replies r
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Review_Replies c WHERE c.reply_id = r.reply_id);
    SET IDENTITY_INSERT dbo.Review_Replies OFF;

    COMMIT TRANSACTION;
    PRINT N'Marcus: merge dữ liệu từ backup 26/07 thành công.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
