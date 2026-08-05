/*
 * Chuẩn hóa Banner_Positions và seed banner cho feature/huy/client-banner.
 * Có thể chạy lại nhiều lần: chỉ xóa/seed ba vị trí banner trang chủ được quản lý.
 * Không xóa banner ở các vị trí khác ngoài nhóm legacy/canonical bên dưới.
 */

USE MarcusStoreDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.Banner_Positions', N'U') IS NULL
        THROW 52000, N'Không tồn tại bảng dbo.Banner_Positions.', 1;

    IF OBJECT_ID(N'dbo.Banners', N'U') IS NULL
        THROW 52001, N'Không tồn tại bảng dbo.Banners.', 1;

    /*
     * Xóa dữ liệu banner cũ thuộc các position sẽ được chuẩn hóa.
     * Bao gồm tên đang có trong DB và tên code mới đang gọi.
     */
    DELETE banner
    FROM dbo.Banners banner
    INNER JOIN dbo.Banner_Positions position
        ON position.position_id = banner.position_id
    WHERE position.position_code IN (
        'HOME_HERO_SLIDER',
        'HOME_SLIDER',
        'HOME_MIDDLE',
        'HOME_BANNER_1',
        'HOME_BANNER_2',
        'CATEGORY_TOP',
        'CATEGORY_BANNER'
    );

    /* HOME_HERO_SLIDER cũ tương ứng HOME_SLIDER mới. */
    IF EXISTS (
        SELECT 1 FROM dbo.Banner_Positions
        WHERE position_code = 'HOME_HERO_SLIDER'
    )
    BEGIN
        IF EXISTS (
            SELECT 1 FROM dbo.Banner_Positions
            WHERE position_code = 'HOME_SLIDER'
        )
        BEGIN
            DELETE FROM dbo.Banner_Positions
            WHERE position_code = 'HOME_HERO_SLIDER';
        END
        ELSE
        BEGIN
            UPDATE dbo.Banner_Positions
            SET
                position_code = 'HOME_SLIDER',
                description = N'Slider chính trang chủ (tối đa 5 banner)'
            WHERE position_code = 'HOME_HERO_SLIDER';
        END;
    END;

    /* HOME_MIDDLE cũ được dùng làm banner nhỏ phía trên bên phải. */
    IF EXISTS (
        SELECT 1 FROM dbo.Banner_Positions
        WHERE position_code = 'HOME_MIDDLE'
    )
    BEGIN
        IF EXISTS (
            SELECT 1 FROM dbo.Banner_Positions
            WHERE position_code = 'HOME_BANNER_1'
        )
        BEGIN
            DELETE FROM dbo.Banner_Positions
            WHERE position_code = 'HOME_MIDDLE';
        END
        ELSE
        BEGIN
            UPDATE dbo.Banner_Positions
            SET
                position_code = 'HOME_BANNER_1',
                description = N'Banner nhỏ phía trên bên phải trang chủ'
            WHERE position_code = 'HOME_MIDDLE';
        END;
    END;

    /* Hai position danh mục không còn được feature client-banner sử dụng. */
    DELETE FROM dbo.Banner_Positions
    WHERE position_code IN ('CATEGORY_TOP', 'CATEGORY_BANNER');

    /* Tạo bổ sung position nếu DB chưa từng có tên cũ tương ứng. */
    IF NOT EXISTS (
        SELECT 1 FROM dbo.Banner_Positions WHERE position_code = 'HOME_SLIDER'
    )
    BEGIN
        INSERT INTO dbo.Banner_Positions (position_code, description)
        VALUES ('HOME_SLIDER', N'Slider chính trang chủ (tối đa 5 banner)');
    END;

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Banner_Positions WHERE position_code = 'HOME_BANNER_1'
    )
    BEGIN
        INSERT INTO dbo.Banner_Positions (position_code, description)
        VALUES ('HOME_BANNER_1', N'Banner nhỏ phía trên bên phải trang chủ');
    END;

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Banner_Positions WHERE position_code = 'HOME_BANNER_2'
    )
    BEGIN
        INSERT INTO dbo.Banner_Positions (position_code, description)
        VALUES ('HOME_BANNER_2', N'Banner nhỏ phía dưới bên phải trang chủ');
    END;

    /* Đồng bộ mô tả ngay cả khi position đã tồn tại sẵn. */
    UPDATE dbo.Banner_Positions
    SET description = CASE position_code
        WHEN 'HOME_SLIDER' THEN N'Slider chính trang chủ (tối đa 5 banner)'
        WHEN 'HOME_BANNER_1' THEN N'Banner nhỏ phía trên bên phải trang chủ'
        WHEN 'HOME_BANNER_2' THEN N'Banner nhỏ phía dưới bên phải trang chủ'
        ELSE description
    END
    WHERE position_code IN ('HOME_SLIDER', 'HOME_BANNER_1', 'HOME_BANNER_2');

    DECLARE @HomeSliderPositionId INT = (
        SELECT position_id
        FROM dbo.Banner_Positions
        WHERE position_code = 'HOME_SLIDER'
    );

    DECLARE @HomeBanner1PositionId INT = (
        SELECT position_id
        FROM dbo.Banner_Positions
        WHERE position_code = 'HOME_BANNER_1'
    );

    DECLARE @HomeBanner2PositionId INT = (
        SELECT position_id
        FROM dbo.Banner_Positions
        WHERE position_code = 'HOME_BANNER_2'
    );

    IF @HomeSliderPositionId IS NULL
       OR @HomeBanner1PositionId IS NULL
       OR @HomeBanner2PositionId IS NULL
        THROW 52002, N'Không thể chuẩn hóa đủ ba vị trí banner trang chủ.', 1;

    /*
     * Dùng ngày kết thúc NULL để dữ liệu seed không tự hết hạn.
     * target_url được đổi sang route thực tế đang có trong Vue Router.
     */
    INSERT INTO dbo.Banners (
        position_id,
        title,
        image_url,
        target_url,
        display_order,
        is_active,
        start_date,
        end_date
    )
    VALUES
        (
            @HomeSliderPositionId,
            N'iPhone 17 Series — Hiệu Năng Đỉnh Cao',
            'https://res.cloudinary.com/dyeb3lju6/image/upload/v1785583747/marcus-store/u0fe5tuygmoho2ailcfs.png',
            '/product/iphone-17-pro-max',
            1, 1, '2026-08-01T00:00:00', NULL
        ),
        (
            @HomeSliderPositionId,
            N'Samsung Galaxy S24 Series — Galaxy AI Thế Hệ Mới',
            'https://res.cloudinary.com/dyeb3lju6/image/upload/v1785583776/marcus-store/mwo5qr51bgxczd1srlnn.png',
            '/product/samsung-galaxy-s24-ultra',
            2, 1, '2026-08-01T00:00:00', NULL
        ),
        (
            @HomeSliderPositionId,
            N'Samsung Galaxy S26 Ultra — Công Nghệ Tương Lai',
            'https://res.cloudinary.com/dyeb3lju6/image/upload/v1785583804/marcus-store/hsvczx1vt5xvrwh5ajyd.png',
            '/search',
            3, 1, '2026-08-01T00:00:00', NULL
        ),
        (
            @HomeSliderPositionId,
            N'Flash Sale — Giảm Giá Đến 50%',
            'https://res.cloudinary.com/dyeb3lju6/image/upload/v1785583828/marcus-store/fq5ofceffkkmiexlqw0s.png',
            '/khuyen-mai',
            4, 1, '2026-08-01T00:00:00', NULL
        ),
        (
            @HomeSliderPositionId,
            N'Xiaomi Chính Hãng — Giá Tốt Mỗi Ngày',
            'https://res.cloudinary.com/dyeb3lju6/image/upload/v1785584302/marcus-store/huri3d0r67rr9ly4bhma.png',
            '/product/xiaomi-14-ultra',
            5, 1, '2026-08-01T00:00:00', NULL
        ),
        (
            @HomeBanner1PositionId,
            N'AirPods Pro — Âm Thanh Chuẩn Studio',
            'https://res.cloudinary.com/dyeb3lju6/image/upload/v1785584333/marcus-store/vfulhksgzqad64m8icsq.png',
            '/product/airpods-pro-the-he-2',
            1, 1, '2026-08-01T00:00:00', NULL
        ),
        (
            @HomeBanner2PositionId,
            N'iPad Thế Hệ Mới — Mạnh Mẽ Cho Công Việc & Giải Trí',
            'https://res.cloudinary.com/dyeb3lju6/image/upload/v1785584398/marcus-store/o4a4sh1ttq2fblf5whes.png',
            '/',
            1, 1, '2026-08-01T00:00:00', NULL
        );

    /* Hậu kiểm: đúng 5 slider và mỗi side position đúng 1 banner. */
    IF (SELECT COUNT(*) FROM dbo.Banners WHERE position_id = @HomeSliderPositionId) <> 5
        THROW 52003, N'Seed HOME_SLIDER không đủ đúng 5 banner.', 1;

    IF (SELECT COUNT(*) FROM dbo.Banners WHERE position_id = @HomeBanner1PositionId) <> 1
        THROW 52004, N'Seed HOME_BANNER_1 không đủ đúng 1 banner.', 1;

    IF (SELECT COUNT(*) FROM dbo.Banners WHERE position_id = @HomeBanner2PositionId) <> 1
        THROW 52005, N'Seed HOME_BANNER_2 không đủ đúng 1 banner.', 1;

    COMMIT TRANSACTION;

    SELECT
        position.position_id,
        position.position_code,
        position.description
    FROM dbo.Banner_Positions position
    ORDER BY position.position_id;

    SELECT
        banner.banner_id,
        position.position_code,
        position.description AS vi_tri,
        banner.title,
        banner.display_order AS thu_tu,
        banner.image_url,
        banner.target_url,
        banner.is_active,
        banner.start_date,
        banner.end_date
    FROM dbo.Banners banner
    INNER JOIN dbo.Banner_Positions position
        ON position.position_id = banner.position_id
    WHERE position.position_code IN (
        'HOME_SLIDER',
        'HOME_BANNER_1',
        'HOME_BANNER_2'
    )
    ORDER BY
        CASE position.position_code
            WHEN 'HOME_SLIDER' THEN 1
            WHEN 'HOME_BANNER_1' THEN 2
            WHEN 'HOME_BANNER_2' THEN 3
            ELSE 4
        END,
        banner.display_order;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

