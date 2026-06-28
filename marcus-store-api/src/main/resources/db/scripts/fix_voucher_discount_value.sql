-- Script fix voucher: Khi discountType = 'PERCENT', discountValue phải là % (0-100)
-- Nếu discountValue > 100, đây là data lỗi - cần xem lại ý nghĩa

-- Bước 1: Kiểm tra các voucher PERCENT có discountValue > 100 (đây là lỗi data)
SELECT 
    voucher_id,
    voucher_code,
    discount_type,
    discount_value,
    max_discount_amount,
    min_order_value
FROM Vouchers 
WHERE discount_type = 'PERCENT' 
  AND discount_value > 100;

-- Bước 2 (TÙY CHỌN): Nếu muốn fix data - Ví dụ: 
-- Giả sử voucher có discountValue = 2000000, đây là số tiền cần giảm, 
-- nhưng người dùng nhập nhầm. Bạn cần xác định % đúng
-- Ví dụ: nếu muốn 25%, update như sau:
-- UPDATE Vouchers SET discount_value = 25 WHERE voucher_id = <id>;
