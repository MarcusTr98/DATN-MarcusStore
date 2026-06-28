# Test Cases Voucher - Full Logic (Checklist Manual)

> Pham vi: Verify toan bo luong logic voucher (apply / confirm / rollback / re-use) tren he thong Marcus Store.
> Loai tru: logic FREE SHIP (chi tap trung PERCENT va AMOUNT).
> Database: SQL Server, bang `Vouchers` va `User_Vouchers` (xem file fix_voucher_discount_value.sql de biet schema thuc te).
> Format moi test case: 8 buoc co verify DB.

---

## Quy uoc chung

### Trang thai ban dau (Pre-condition)
- User A (id=A), User B (id=B), User C (id=C) da dang ky tai khoan, co dia chi giao hang hop le
- San pham P1 co nhieu SKU de tuy chinh gia dat `subtotal >= minOrderValue`
- DB clean truoc moi nhom test: `DELETE FROM User_Vouchers; DELETE FROM Vouchers;`

### Format 8 buoc (ap dung cho moi test case)
1. Login user theo test case (A/B/C)
2. Chuan bi du lieu (gio hang / voucher)
3. Setup voucher (qua API Admin `POST /api/admin/voucher` hoac SQL)
4. Hanh dong chinh (apply / checkout / cancel)
5. Mo DB kiem tra bang `Vouchers` (quantity, is_active)
6. Mo DB kiem tra bang `User_Vouchers` (is_used, used_at)
7. Mo DB kiem tra bang lien quan (`Orders`, `Order_Items`)
8. Verify Expected vs Actual + Ket luan PASS/FAIL

### Cac query verify chuan (SQL Server)

```sql
-- Voucher state
SELECT voucher_id, voucher_code, discount_type, quantity, is_active, target_type,
       start_date, end_date
FROM Vouchers
WHERE voucher_id = ?;

-- UserVoucher state
SELECT id, voucher_id, user_id, is_used, used_at, assigned_at
FROM User_Vouchers
WHERE voucher_id = ? AND user_id = ?;

-- Order state
SELECT order_id, order_code, order_status, voucher_id, discount_amount,
       total_amount, final_amount
FROM Orders
WHERE order_code = ?;

-- Dem so user da dung voucher (cross-check voi GET /usage-count)
SELECT COUNT(*) AS used_count
FROM User_Vouchers
WHERE voucher_id = ? AND is_used = 1;
```

---

## PHAN A - HAPPY PATH (OK)

### TC-01: PERCENT ALL - User A dat hang thanh cong

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (gia 1.000.000d, qty=1) vao gio | - |
| B3 | Admin tao voucher: code=`VC01`, PERCENT 20%, maxDiscount=300.000, minOrder=500.000, quantity=10, targetType=ALL, startDate=hqua, endDate=ngaymai, isActive=true | `Vouchers`: quantity=10, is_active=1 |
| B4 | User A POST `/api/client/vouchers/preview` voi code=`VC01`, orderAmount=1.000.000 | Response: applied=true, discountAmount=200.000 |
| B5 | User A POST `/api/checkout` voi voucherCode=`VC01` | Order tao thanh cong, status=PENDING |
| B6 | `SELECT quantity, is_active FROM Vouchers WHERE voucher_code='VC01'` | quantity=9, is_active=1 |
| B7 | `SELECT is_used, used_at FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=1, used_at NOT NULL |
| B8 | So sanh Expected vs Actual | PASS neu dung het |

---

### TC-02: AMOUNT ALL - User A dat hang thanh cong

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (gia 800.000d, qty=1) vao gio | - |
| B3 | Admin tao voucher: code=`VC02`, AMOUNT 50.000, minOrder=500.000, quantity=5, targetType=ALL | `Vouchers`: quantity=5 |
| B4 | User A preview voi code=`VC02`, orderAmount=800.000 | applied=true, discountAmount=50.000 |
| B5 | User A checkout voi code=`VC02` | Order thanh cong, finalAmount=750.000 |
| B6 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC02'` | quantity=4 |
| B7 | `SELECT is_used FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=1 |
| B8 | So sanh | PASS |

---

### TC-03: PERCENT SPECIFIC - User duoc gan dat hang thanh cong

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login admin, sau do login user A | - |
| B2 | User A them P1 (1.000.000d) vao gio | - |
| B3 | Admin tao voucher: code=`VC03`, PERCENT 15%, maxDiscount=200.000, minOrder=300.000, quantity=10, targetType=SPECIFIC, targetUserIds=[A] | `User_Vouchers`: 1 row (A, is_used=0, used_at=NULL) |
| B4 | User A preview voi code=`VC03` | applied=true, discountAmount=150.000 |
| B5 | User A checkout voi code=`VC03` | Order thanh cong |
| B6 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC03'` | quantity=9 |
| B7 | `SELECT is_used, used_at FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=1, used_at NOT NULL |
| B8 | So sanh | PASS |

---

### TC-04: Cancel don PENDING -> voucher rollback hoan toan

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | Lap lai TC-01 (User A dat hang voi VC01) | Order PENDING, VC01 quantity=9, UserVoucher(A) is_used=1 |
| B6 | Admin POST `/api/admin/orders/{orderCode}/status` status=CANCELLED, note="Khach khong nhan hang" | Order status=CANCELLED |
| B7 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC01'` | quantity=10 |
| B8 | `SELECT is_used, used_at FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=0, used_at=NULL |
| B9 | So sanh | PASS |

---

### TC-05: Re-use voucher sau khi cancel don cu (vi du mau)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (1.000.000d, qty=1) vao gio, subtotal >= minOrder | - |
| B3 | Admin tao voucher ALL PERCENT 20%, minOrder=500.000, quantity=10 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC05'` → quantity=10 |
| B4 | User A checkout voi voucher `VC05` → dat hang thanh cong | Order PENDING |
| B5 | DB check | `SELECT quantity, is_active FROM Vouchers WHERE voucher_code='VC05'` → quantity=9, is_active=1. `SELECT is_used, used_at FROM User_Vouchers WHERE voucher_id=? AND user_id=A` → is_used=1, used_at NOT NULL |
| B6 | Admin vao trang quan ly don hang → huy don (status CANCELLED, co note) | - |
| B7 | DB check lai | `SELECT quantity FROM Vouchers WHERE voucher_code='VC05'` → quantity=10 (tang lai). `SELECT is_used, used_at FROM User_Vouchers WHERE voucher_id=? AND user_id=A` → is_used=0, used_at=NULL |
| B8 | User A tao don moi voi P1 + voucher `VC05` → DAT DUOC | DB verify: quantity=9, is_used=1 |
| B9 | So sanh Expected vs Actual | PASS neu ca 3 lan verify deu dung |

---

## PHAN B - NGHIEP VU NG (Validation)

### TC-06: Voucher khong ton tai

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (500.000d) vao gio | - |
| B3 | Khong tao voucher nao | `SELECT * FROM Vouchers WHERE voucher_code='FAKE'` → empty |
| B4 | User A preview voi code=`FAKE123` | Response: applied=false, message="Ma giam gia khong ton tai." |
| B5-B7 | Khong co thay doi DB | `Vouchers`: khong co row moi. `User_Vouchers`: empty. Khong co order |
| B8 | So sanh | PASS neu message dung |

---

### TC-07: Voucher chua bat dau (startDate tuong lai)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (1.000.000d) vao gio | - |
| B3 | Admin tao voucher: code=`VC07`, PERCENT 10%, startDate=ngay mai 00:00, endDate=ngay kia 23:59, quantity=10, isActive=true | `Vouchers`: 1 row |
| B4 | User A preview voi code=`VC07` | applied=false, message="Ma giam gia chua co hieu luc." |
| B5 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC07'` | quantity=10 (khong bi tru) |
| B6 | `SELECT * FROM User_Vouchers WHERE voucher_code...` | empty (khong tao record) |
| B7 | Khong co order | - |
| B8 | So sanh | PASS |

---

### TC-08: Voucher da het han (endDate qua khu)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (1.000.000d) vao gio | - |
| B3 | Admin tao voucher: code=`VC08`, PERCENT 10%, startDate=2 ngay truoc, endDate=1 ngay truoc 23:59, quantity=10, isActive=true | `Vouchers`: 1 row, is_active=1 |
| B4 | User A preview voi code=`VC08` | applied=false, message="Voucher da het han..." |
| B5 | `SELECT is_active FROM Vouchers WHERE voucher_code='VC08'` | is_active=0 (auto deactivate) |
| B6 | `SELECT * FROM User_Vouchers WHERE voucher_id=?` | empty |
| B7 | Khong co order | - |
| B8 | So sanh | PASS (xac nhan co auto-deactivate) |

---

### TC-09: Voucher is_active = false

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (500.000d) vao gio | - |
| B3 | Admin tao voucher: code=`VC09`, PERCENT 10%, quantity=10, isActive=true, sau do PUT `/api/admin/voucher/{id}` isActive=false | `Vouchers`: is_active=0 |
| B4 | User A preview voi code=`VC09` | applied=false, message="Voucher da het han hoac het luot su dung..." |
| B5-B7 | DB khong doi | - |
| B8 | So sanh | PASS |

---

### TC-10: Don hang < minOrderValue

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (gia 100.000d, qty=1) vao gio → subtotal=100.000d | - |
| B3 | Admin tao voucher: code=`VC10`, AMOUNT 20.000, minOrder=500.000, quantity=10, ALL | `Vouchers`: 1 row |
| B4 | User A preview voi code=`VC10`, orderAmount=100.000 | applied=false, message="Don hang chua dat gia tri toi thieu..." |
| B5-B7 | DB khong doi, khong co User_Vouchers, khong co order | - |
| B8 | So sanh | PASS |

---

### TC-11: SPECIFIC - User khong nam trong danh sach gan

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login admin, tao User B (neu chua co) | - |
| B2 | Login user A | - |
| B3 | Admin tao voucher: code=`VC11`, PERCENT 10%, minOrder=100.000, quantity=10, targetType=SPECIFIC, targetUserIds=[B] | `User_Vouchers`: 1 row (B, is_used=0) |
| B4 | User A preview voi code=`VC11`, orderAmount=1.000.000 | applied=false, message="Ban khong duoc phep su dung ma giam gia nay." |
| B5 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC11'` | quantity=10 (khong tru) |
| B6 | `SELECT * FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | empty (khong tao record) |
| B7 | User A checkout (neu preview loi, checkout se fail) | Order khong duoc tao |
| B8 | So sanh | PASS |

---

### TC-12: User da dung voucher (1 user / 1 voucher / 1 lan)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | Lap lai TC-01 de User A da dung VC01, is_used=1 | Order PENDING, VC01 quantity=9 |
| B6 | User A preview LAI voi code=`VC01` (lan 2) | applied=false, message="Ban da su dung ma giam gia nay roi." |
| B7 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC01'` | quantity=9 (preview khong tru) |
| B8 | So sanh | PASS |

---

### TC-13: Voucher het quantity (quantity=0)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (1.000.000d) vao gio | - |
| B3 | Admin tao voucher: code=`VC13`, PERCENT 10%, quantity=1 | `Vouchers`: quantity=1 |
| B4 | User A preview va checkout → quantity=0, is_used=1 | TC-01 mini |
| B5 | User A preview LAI voi code=`VC13` | applied=false, message="Voucher da het luot su dung..." |
| B6 | `SELECT quantity, is_active FROM Vouchers WHERE voucher_code='VC13'` | quantity=0, is_active=0 (auto tat khi quantity ve 0) |
| B7 | `SELECT is_used FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=1 (giu nguyen) |
| B8 | So sanh | PASS |

---

## PHAN C - TINH TOAN DISCOUNT

### TC-14: PERCENT khong vuot maxDiscount

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (200.000d, qty=1) vao gio | - |
| B3 | Admin tao voucher: PERCENT 10%, maxDiscount=500.000, minOrder=100.000, quantity=10 | - |
| B4 | User A preview voi orderAmount=200.000 | applied=true, discountAmount=20.000 |
| B5-B7 | DB khong doi | - |
| B8 | So sanh | PASS (200.000 x 10% = 20.000 < 500.000) |

---

### TC-15: PERCENT vuot maxDiscount -> cap theo max

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (10.000.000d, qty=1) vao gio | - |
| B3 | Admin tao voucher: PERCENT 20%, maxDiscount=1.000.000, minOrder=100.000, quantity=10 | - |
| B4 | User A preview voi orderAmount=10.000.000 | applied=true, discountAmount=1.000.000 (khong phai 2.000.000) |
| B5-B7 | DB khong doi | - |
| B8 | So sanh | PASS (cap theo max) |

---

### TC-16: AMOUNT lon hon don hang -> cap theo orderAmount (khong am)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (100.000d, qty=1) vao gio | - |
| B3 | Admin tao voucher: AMOUNT 500.000, minOrder=50.000, quantity=10 | - |
| B4 | User A preview voi orderAmount=100.000 | applied=true, discountAmount=100.000 (khong am, khong vuot 100.000) |
| B5 | User A checkout | finalAmount=0 |
| B6-B7 | DB kiem tra | `Vouchers`: quantity=9, `Orders`: finalAmount=0 |
| B8 | So sanh | PASS |

---

### TC-17: PERCENT 100% giam dung bang don

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (500.000d) vao gio | - |
| B3 | Admin tao voucher: PERCENT 100, maxDiscount=10.000.000, minOrder=100.000, quantity=10 | - |
| B4 | User A preview voi orderAmount=500.000 | applied=true, discountAmount=500.000 |
| B5-B7 | DB kiem tra binh thuong | - |
| B8 | So sanh | PASS |

---

### TC-18: PERCENT lam tron HALF_UP voi don le

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (105.555d, qty=1) vao gio → orderAmount=105.555 | - |
| B3 | Admin tao voucher: PERCENT 10%, maxDiscount=1.000.000, minOrder=100.000, quantity=10 | - |
| B4 | User A preview voi orderAmount=105.555 | applied=true, discountAmount=10.555,5 hoac 10.555,50 (lam tron HALF_UP 2 chu so) |
| B5-B7 | DB khong doi | - |
| B8 | So sanh | PASS (verify gia tri chinh xac theo HALF_UP) |

---

### TC-19: Discount khong am (BigDecimal edge case)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (1d, qty=1) vao gio → orderAmount=1 | - |
| B3 | Admin tao voucher: AMOUNT 100.000, minOrder=0, quantity=10 | - |
| B4 | User A preview | applied=true, discountAmount=1 (khong am) |
| B5-B7 | DB | `Orders.discount_amount=1`, `final_amount=0` |
| B8 | So sanh | PASS |

---

## PHAN D - ROLLBACK & RE-USE

### TC-20: Cancel CONFIRMED -> rollback voucher

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | User A dat hang VC01 → order PENDING, quantity=9, is_used=1 | - |
| B6 | Admin chuyen status PENDING → CONFIRMED | - |
| B7 | Admin chuyen status CONFIRMED → CANCELLED, co note | - |
| B8 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC01'` | quantity=10 |
| B9 | `SELECT is_used, used_at FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=0, used_at=NULL |
| B10 | So sanh | PASS |

---

### TC-21: Cancel PROCESSING -> rollback voucher

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | User A dat hang VC01 → order PENDING | - |
| B6 | Admin chuyen: PENDING → CONFIRMED → PROCESSING | - |
| B7 | Admin chuyen PROCESSING → CANCELLED, co note | - |
| B8 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC01'` | quantity=10 |
| B9 | `SELECT is_used FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=0 |
| B10 | So sanh | PASS |

---

### TC-22: Cancel khi voucher da het han (endDate qua) -> KHONG tang quantity, CHI reset is_used

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | User A dat hang VC01 → order PENDING, VC01 quantity=9 | - |
| B6 | Admin update Vouchers set endDate=hom qua voi VC01 | `Vouchers`: endDate qua khu |
| B7 | Admin cancel don (PENDING → CANCELLED) | - |
| B8 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC01'` | quantity=9 (KHONG tang lai vi voucher da chet) |
| B9 | `SELECT is_used FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=0 (reset) |
| B10 | So sanh | PASS (kiem tra logic dac biet trong OrderServiceImpl cancel) |

---

### TC-23: Cancel khi don khong co voucher -> bo qua

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Them P1 (500.000d) vao gio | - |
| B3 | User A checkout KHONG truyen voucherCode | Order thanh cong, voucher_id=NULL |
| B4 | Admin cancel don | - |
| B5 | `SELECT * FROM User_Vouchers WHERE user_id=A` | empty (khong anh huong) |
| B6 | So sanh | PASS |

---

### TC-24: Rollback 2 lan lien tiep (canh bao bug nghi van)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | User A dat hang VC01 (quantity=10) → quantity=9, is_used=1 | - |
| B6 | Admin cancel don 1 lan (qua API) | quantity=10, is_used=0 |
| B7 | Admin goi lai API cancel don lan 2 (cung orderCode, cung status CANCELLED) | Co the: (a) throw loi "Khong the chuyen trang thai tu CANCELLED sang CANCELLED" HOAC (b) tang quantity len 11 (BUG) |
| B8 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC01'` | Neu (a): quantity=10. Neu (b): quantity=11 (BUG, can fix) |
| B9 | So sanh | PASS neu a, FAIL neu b (ghi nhan bug) |

---

## PHAN E - PERMISSION & TARGET TYPE

### TC-25: ALL - User A dung xong, User B van dung duoc

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | User A dat hang VC01 (quantity=10 → 9), is_used=1 | - |
| B6 | Login user B, them P1 (1.000.000d) vao gio | - |
| B7 | User B preview va checkout voi VC01 | Order thanh cong |
| B8 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC01'` | quantity=8 |
| B9 | `SELECT is_used FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=1 |
| B10 | `SELECT is_used FROM User_Vouchers WHERE voucher_id=? AND user_id=B` | is_used=1 |
| B11 | So sanh | PASS (moi user co 1 User_Voucher rieng) |

---

### TC-26: SPECIFIC - User ngoai danh sach bi reject (mo rong TC-11)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B3 | Admin tao voucher SPECIFIC, targetUserIds=[A] | `User_Vouchers`: 1 row (A) |
| B4 | Login user B, preview VC03 | applied=false, "khong duoc phep..." |
| B5 | Login user C, preview VC03 | applied=false, "khong duoc phep..." |
| B6 | Login user A, preview VC03 | applied=true |
| B7 | So sanh | PASS |

---

### TC-27: SPECIFIC - User trong danh sach nhung is_used=true -> reject

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | User A da dung VC03 (SPECIFIC) → is_used=1 | - |
| B6 | User A preview LAI VC03 | applied=false, "Ban da su dung..." |
| B7 | `SELECT is_used FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=1 |
| B8 | So sanh | PASS |

---

### TC-28: Admin sua voucher ALL -> SPECIFIC (UserVoucher moi duoc sinh)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B3 | Admin tao voucher VC28 ALL, quantity=10 | `User_Vouchers`: empty |
| B4 | Admin PUT `/api/admin/voucher/{id}` chuyen targetType=SPECIFIC, targetUserIds=[A,B] | - |
| B5 | `SELECT * FROM User_Vouchers WHERE voucher_id=?` | 2 rows (A va B, is_used=0) |
| B6 | `SELECT target_type FROM Vouchers WHERE voucher_code='VC28'` | SPECIFIC |
| B7 | So sanh | PASS |

---

## PHAN F - RACE CONDITION (canh bao bug)

### TC-29: 2 user cung apply PERCENT ALL quantity=1

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A va B (2 session) | - |
| B2 | Ca 2 them P1 (1.000.000d) vao gio | - |
| B3 | Admin tao voucher: PERCENT 10%, quantity=1, ALL | `Vouchers`: quantity=1 |
| B4 | A va B cung POST preview (dong thoi) | Ca 2 deu nhan applied=true (preview khong lock) |
| B5 | A checkout truoc → quantity=0, is_active=0 | `Vouchers`: quantity=0 |
| B6 | B checkout sau (chay dong thoi voi A) | Co 2 truong hop: (a) order B thanh cong nhung confirm fail → "Voucher da het luot su dung" (race condition tot), HOAC (b) ca 2 deu thanh cong → quantity=-1 (BUG) |
| B7 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC29'` | Neu (a): quantity=0. Neu (b): quantity=-1 |
| B8 | So sanh | PASS neu a, FAIL neu b |

---

### TC-30: Preview nhieu lan khong checkout → quantity giu nguyen

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A, them P1 (1.000.000d) vao gio | - |
| B2 | Admin tao voucher VC30, quantity=10 | - |
| B3 | User A goi preview 10 lan lien tiep voi VC30 | 10 lan deu applied=true |
| B4 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC30'` | quantity=10 (khong bi tru) |
| B5 | `SELECT * FROM User_Vouchers WHERE user_id=A` | empty (preview khong tao record) |
| B6 | So sanh | PASS (preview chi check, khong ghi) |

---

### TC-31: Confirm fail sau khi order luu (canh bao bug W10)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | User A checkout voi VC31 → order PENDING (Binh thuong) | - |
| B6 | Truoc khi confirmVoucherUsage chay, Admin set VC31.is_active=false (qua SQL) | `Vouchers`: is_active=0 |
| B7 | Quan sat confirmVoucherUsage | Co throw RuntimeException, nhung CheckoutService chi log warning → Order van ton tai |
| B8 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC31'` | quantity=10 (khong bi tru do confirm fail) |
| B9 | `SELECT * FROM User_Vouchers WHERE voucher_id=?` | empty (khong tao) |
| B10 | `SELECT order_status FROM Orders WHERE voucher_id=?` | PENDING (order van ton tai) |
| B11 | So sanh | PASS neu order van ton tai nhung voucher khong bi tru (can fix de rollback order neu confirm fail) |

---

## PHAN G - EDGE CASES

### TC-32: Voucher PERCENT value=0.5 (so thap phan nho)

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A, them P1 (1.000.000d) | - |
| B2 | Admin tao voucher: PERCENT 0.5 (cho phep? validate chi check > 0 va <= 100), maxDiscount=10.000, quantity=10 | Tao thanh cong |
| B3 | User A preview voi orderAmount=1.000.000 | applied=true, discountAmount=5.000 |
| B4 | So sanh | PASS (kiem tra co cho phep so thap phan khong) |

---

### TC-33: Order amount = 0

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Trong gio hang co 1 san pham gia 0d (hoac gia tri am do loi) - can setup test data | - |
| B3 | User A preview voi orderAmount=0 | applied=true (neu minOrder=0), discountAmount=0 |
| B4 | So sanh | PASS |

---

### TC-34: Cancel tu SHIPPING/COMPLETED -> KHONG cho rollback

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1-B5 | User A dat hang VC34 → order PENDING, quantity=9, is_used=1 | - |
| B6 | Admin chuyen: PENDING → CONFIRMED → PROCESSING → SHIPPING | - |
| B7 | Admin goi cancel voi status=CANCELLED | Throw "Khong the chuyen trang thai tu SHIPPING sang CANCELLED" |
| B8 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC34'` | quantity=9 (khong rollback) |
| B9 | `SELECT is_used FROM User_Vouchers WHERE voucher_id=? AND user_id=A` | is_used=1 (giu nguyen) |
| B10 | So sanh | PASS |

---

### TC-35: Preview 10 lan lien tiep khong checkout → quantity nguyen

| Buoc | Hanh dong | Verify DB / Expected |
|------|-----------|----------------------|
| B1 | Login user A | - |
| B2 | Admin tao voucher VC35, quantity=10 | - |
| B3 | User A goi `/api/client/vouchers/preview` 10 lan voi VC35 | Tat ca applied=true |
| B4 | `SELECT quantity FROM Vouchers WHERE voucher_code='VC35'` | quantity=10 (khong tru) |
| B5 | `SELECT * FROM User_Vouchers WHERE user_id=A` | empty (khong tao record) |
| B6 | So sanh | PASS |

---

## BANG TONG HOP PASS/FAIL

| Test Case | Trang thai | Nguoi test | Ngay test | Ghi chu |
|-----------|------------|------------|-----------|---------|
| TC-01 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-02 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-03 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-04 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-05 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-06 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-07 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-08 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-09 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-10 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-11 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-12 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-13 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-14 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-15 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-16 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-17 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-18 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-19 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-20 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-21 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-22 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-23 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-24 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-25 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-26 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-27 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-28 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-29 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-30 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-31 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-32 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-33 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-34 | [ ] PASS / [ ] FAIL |  |  |  |
| TC-35 | [ ] PASS / [ ] FAIL |  |  |  |

---

## PHU LUC - DANH SACH BUG NGHI VAN (tu phan tich code)

| ID | Muc do | Mo ta | Vi tri file |
|----|--------|-------|-------------|
| W1 | Thap | Entity cho phep `discountType='GIFT'` nhung Service reject | `Voucher.java:34` vs `VoucherServiceImpl.java:227-234` |
| W2 | Thap | `quantity=null` mac dinh thanh 1 (co the gay nham) | `VoucherServiceImpl.java:190, 197, 204` |
| W3 | Trung binh | `applyVoucher` SPECIFIC reset isUsed=false khi het quantity (logic dac biet) | `VoucherServiceImpl.java:567-577` |
| W4 | Cao | Confirm fail sau khi order da luu → order van ton tai, voucher khong bi tru | `CheckoutService.java:217-225` (xem TC-31) |
| W5 | Trung binh | `rollbackVoucherUsage` tang quantity khong check gioi han tren | `VoucherServiceImpl.java:640-644` (xem TC-24) |
| W6 | Trung binh | `OrderServiceImpl.cancel` check endDate co the bypass neu admin doi ngay | `OrderServiceImpl.java:362-369` (xem TC-22) |
| W7 | Thap | `checkAndRecordVoucherUsage` (method cu) con ton tai nhung khong duoc goi → dead code | `VoucherServiceImpl.java:391-458` |
| W8 | Trung binh | Update voucher SPECIFIC ma thieu targetUserIds → bi im lang (khong throw) | `VoucherServiceImpl.java:357` |
| W9 | Thap | Validate `voucherCode` khong check ky tu dac biet / do dai | `AddVoucherRequest.java:15` (chi @NotBlank) |
| W10 | Trung binh | Cancel nhieu lan co the tang quantity vuot qua ban dau (neu bypass canChangeStatus) | `VoucherServiceImpl.java:642` (xem TC-24) |

---

## Huong dan chay test

1. **Chuan bi DB test rieng** (khac DB production)
2. **Mo SQL Server Management Studio** de chay cac query verify
3. **Mo Postman** de goi API Admin (tao voucher, gan user) va Client (preview, checkout)
4. **Tung test case**: thuc hien 8 buoc, ghi ket qua vao bang tong hop
5. **Bug phat hien**: them vao file `docs/voucher-bugs.md` voi ID (W1-W10+) de theo doi
