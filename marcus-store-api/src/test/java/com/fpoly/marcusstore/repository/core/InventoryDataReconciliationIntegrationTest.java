package com.fpoly.marcusstore.repository.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional(readOnly = true)
class InventoryDataReconciliationIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void imeiInventoryAndOrderDocumentsAreReconciled() {
        var summary = jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(DISTINCT sku.sku_id) imei_skus,
                    COUNT(pi.item_id) total_imeis,
                    SUM(CASE WHEN pi.status = 1 AND pi.order_item_id IS NULL THEN 1 ELSE 0 END) in_stock,
                    SUM(CASE WHEN pi.status = 2 AND pi.order_item_id IS NOT NULL THEN 1 ELSE 0 END) sold,
                    SUM(CASE WHEN pi.status = 3 THEN 1 ELSE 0 END) in_warranty,
                    SUM(CASE WHEN pi.status = 4 THEN 1 ELSE 0 END) defective_or_returned
                FROM Product_Skus sku
                JOIN Products p ON p.product_id = sku.product_id AND p.status_imei = 1
                LEFT JOIN Product_Items pi ON pi.sku_id = sku.sku_id
                """);
        System.out.printf("INVENTORY_RECONCILIATION_SUMMARY=%s%n", summary);

        Integer issueCount = jdbcTemplate.queryForObject("""
                WITH ImeiStock AS (
                    SELECT sku_id,
                           SUM(CASE WHEN status = 1 AND order_item_id IS NULL THEN 1 ELSE 0 END) physical_qty
                    FROM Product_Items
                    GROUP BY sku_id
                ), Assigned AS (
                    SELECT order_item_id, COUNT(*) assigned_qty
                    FROM Product_Items
                    WHERE order_item_id IS NOT NULL
                    GROUP BY order_item_id
                ), Reserved AS (
                    SELECT oi.sku_id, SUM(oi.quantity - ISNULL(a.assigned_qty, 0)) reserved_qty
                    FROM Order_Items oi
                    JOIN Orders o ON o.order_id = oi.order_id
                    JOIN Product_Skus sku ON sku.sku_id = oi.sku_id
                    JOIN Products p ON p.product_id = sku.product_id AND p.status_imei = 1
                    LEFT JOIN Assigned a ON a.order_item_id = oi.order_item_id
                    WHERE o.order_status IN (
                        'PENDING','CONFIRMED','PROCESSING',
                        'READY_FOR_PICKUP','PACKED','SHIPPING'
                    )
                      AND oi.quantity > ISNULL(a.assigned_qty, 0)
                    GROUP BY oi.sku_id
                ), StockMismatch AS (
                    SELECT sku.sku_id
                    FROM Product_Skus sku
                    JOIN Products p ON p.product_id = sku.product_id AND p.status_imei = 1
                    LEFT JOIN ImeiStock i ON i.sku_id = sku.sku_id
                    LEFT JOIN Reserved r ON r.sku_id = sku.sku_id
                    WHERE sku.stock_quantity <> CASE
                        WHEN ISNULL(i.physical_qty, 0) - ISNULL(r.reserved_qty, 0) > 0
                        THEN ISNULL(i.physical_qty, 0) - ISNULL(r.reserved_qty, 0)
                        ELSE 0 END
                ), InvalidImei AS (
                    SELECT item_id
                    FROM Product_Items
                    WHERE imei_code LIKE '%[^0-9]%'
                       OR LEN(imei_code) < 8 OR LEN(imei_code) > 20
                       OR (status = 2 AND order_item_id IS NULL)
                       OR (status = 1 AND order_item_id IS NOT NULL)
                ), FulfilledMismatch AS (
                    SELECT oi.order_item_id
                    FROM Order_Items oi
                    JOIN Product_Skus sku ON sku.sku_id = oi.sku_id
                    JOIN Products p ON p.product_id = sku.product_id AND p.status_imei = 1
                    JOIN Orders o ON o.order_id = oi.order_id
                    LEFT JOIN Product_Items pi ON pi.order_item_id = oi.order_item_id
                    WHERE o.order_status IN (
                        'PROCESSING','READY_FOR_PICKUP','PACKED',
                        'SHIPPING','DELIVERED','COMPLETED'
                    )
                    GROUP BY oi.order_item_id, oi.quantity
                    HAVING COUNT(pi.item_id) <> oi.quantity
                )
                SELECT
                    (SELECT COUNT(*) FROM StockMismatch)
                  + (SELECT COUNT(*) FROM InvalidImei)
                  + (SELECT COUNT(*) FROM FulfilledMismatch)
                """, Integer.class);

        assertThat(issueCount)
                .as("số lỗi đối soát tồn SKU, danh sách IMEI và chứng từ đơn hàng")
                .isZero();
    }
}
