-- =============================================================================
-- HOTFIX: Khôi phục giá SKU bị "kẹt" ở flashSalePrice do admin hủy Flash Sale
--
-- Bối cảnh bug:
--   Khi admin hủy Flash Sale, code chỉ clear FK trong cart_items mà KHÔNG
--   khôi phục product_sku.price về giá gốc. Scheduler chỉ restore giá khi
--   slot ACTIVE quá endDate, không xử lý slot CANCELLED → giá SKU bị kẹt
--   vĩnh viễn ở flashSalePrice.
--
-- Cách chạy:
--   1. Chạy SELECT ở PHẦN 1 để xem SKU nào bị ảnh hưởng
--   2. Backup DB (mysqldump hoặc tool tương đương)
--   3. Chạy UPDATE ở PHẦN 2
--   4. Chạy lại SELECT ở PHẦN 1 để verify kết quả = 0
-- =============================================================================

-- =============================================================================
-- PHẦN 1: KIỂM TRA TRƯỚC KHI FIX (DRY-RUN)
-- =============================================================================

SELECT
    ps.sku_id,
    ps.sku_code,
    p.product_name,
    ps.price              AS current_price_db,
    fi.original_price     AS should_be_original,
    fi.flash_sale_price   AS currently_set_as_fs,
    (fi.original_price - ps.price) AS diff_amount,
    fs.status             AS slot_status,
    fs.slot_id,
    fs.name               AS slot_name,
    fs.end_date           AS slot_end_date
FROM product_sku ps
JOIN flash_sale_items fi ON ps.sku_id = fi.sku_id
JOIN flash_sale_slots fs ON fi.slot_id = fs.slot_id
LEFT JOIN products p ON ps.product_id = p.product_id
WHERE fs.status IN (3, 4)              -- ENDED hoặc CANCELLED
  AND ps.price < fi.original_price      -- giá hiện tại đang ở giá FS (nhỏ hơn giá gốc)
  -- Safety: SKU không thuộc slot SCHEDULED/ACTIVE nào khác
  AND ps.sku_id NOT IN (
      SELECT DISTINCT fi2.sku_id
      FROM flash_sale_items fi2
      JOIN flash_sale_slots fs2 ON fi2.slot_id = fs2.slot_id
      WHERE fs2.status IN (1, 2)
  )
ORDER BY fs.end_date DESC, ps.sku_id;

-- Đếm tổng số SKU bị ảnh hưởng
SELECT COUNT(*) AS total_stuck_skus
FROM product_sku ps
JOIN flash_sale_items fi ON ps.sku_id = fi.sku_id
JOIN flash_sale_slots fs ON fi.slot_id = fs.slot_id
WHERE fs.status IN (3, 4)
  AND ps.price < fi.original_price
  AND ps.sku_id NOT IN (
      SELECT DISTINCT fi2.sku_id
      FROM flash_sale_items fi2
      JOIN flash_sale_slots fs2 ON fi2.slot_id = fs2.slot_id
      WHERE fs2.status IN (1, 2)
  );


-- =============================================================================
-- PHẦN 2: KHÔI PHỤC GIÁ SKU VỀ GIÁ GỐC (CHẠY SAU KHI ĐÃ XÁC NHẬN PHẦN 1)
-- =============================================================================

START TRANSACTION;

UPDATE product_sku ps
JOIN flash_sale_items fi ON ps.sku_id = fi.sku_id
JOIN flash_sale_slots fs ON fi.slot_id = fs.slot_id
SET ps.price = fi.original_price
WHERE fs.status IN (3, 4)
  AND ps.price < fi.original_price
  AND ps.sku_id NOT IN (
      SELECT DISTINCT fi2.sku_id
      FROM flash_sale_items fi2
      JOIN flash_sale_slots fs2 ON fi2.slot_id = fs2.slot_id
      WHERE fs2.status IN (1, 2)
  );

-- Kiểm tra số row đã update (MySQL: ROW_COUNT())
SELECT ROW_COUNT() AS rows_updated;

COMMIT;


-- =============================================================================
-- PHẦN 3: VERIFY SAU KHI FIX (MONG ĐỢI: 0 ROWS)
-- =============================================================================

SELECT COUNT(*) AS remaining_stuck_skus
FROM product_sku ps
JOIN flash_sale_items fi ON ps.sku_id = fi.sku_id
JOIN flash_sale_slots fs ON fi.slot_id = fs.slot_id
WHERE fs.status IN (3, 4)
  AND ps.price < fi.original_price
  AND ps.sku_id NOT IN (
      SELECT DISTINCT fi2.sku_id
      FROM flash_sale_items fi2
      JOIN flash_sale_slots fs2 ON fi2.slot_id = fs2.slot_id
      WHERE fs2.status IN (1, 2)
  );