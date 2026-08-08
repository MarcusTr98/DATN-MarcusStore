package com.fpoly.marcusstore.repository.analytics;

import com.fpoly.marcusstore.entity.shopping.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Marcus làm: nguồn truy vấn riêng cho tab Phân tích kinh doanh.
 *
 * Repository này không phụ thuộc StatisticsRepository của Dashboard. Các truy
 * vấn
 * bán hàng dùng Orders.created_at; các truy vấn dòng tiền dùng thời điểm giao
 * dịch.
 */
public interface AnalyticsRepository extends Repository<Order, Integer> {

    @Query(value = """
            SELECT
                (SELECT COUNT_BIG(*)
                 FROM Orders period_order
                 WHERE period_order.created_at >= :fromDateTime
                   AND period_order.created_at < :toDateTimeExclusive) AS totalOrders,
                (SELECT COUNT_BIG(*)
                 FROM Orders completed_order
                 WHERE completed_order.order_status = 'COMPLETED'
                   AND completed_order.created_at >= :fromDateTime
                   AND completed_order.created_at < :toDateTimeExclusive) AS cohortCompletedOrders,
                COUNT_BIG(DISTINCT paid.order_id) AS completedOrders,
                (SELECT COUNT_BIG(*)
                 FROM Orders cancelled_order
                 WHERE cancelled_order.order_status = 'CANCELLED'
                   AND cancelled_order.created_at >= :fromDateTime
                   AND cancelled_order.created_at < :toDateTimeExclusive) AS cancelledOrders,
                COALESCE(SUM(paid.amount), 0) AS completedSales,
                COUNT_BIG(DISTINCT paid.user_id) AS orderingCustomers,
                COALESCE((
                    SELECT SUM(oi.quantity)
                    FROM Order_Items oi
                    WHERE EXISTS (
                        SELECT 1
                        FROM Order_Transactions paid_tx
                        INNER JOIN Orders paid_order ON paid_order.order_id = paid_tx.order_id
                        WHERE paid_tx.order_id = oi.order_id
                          AND paid_tx.status = 'SUCCESS'
                          AND paid_tx.type <> 'REFUND'
                          AND paid_order.order_status = 'COMPLETED'
                          AND paid_tx.created_at >= :fromDateTime
                          AND paid_tx.created_at < :toDateTimeExclusive
                    )
                ), 0) AS unitsSold
            FROM (
                SELECT
                    t.order_id,
                    o.user_id,
                    SUM(t.amount) AS amount
                FROM Order_Transactions t
                INNER JOIN Orders o ON o.order_id = t.order_id
                WHERE t.status = 'SUCCESS'
                  AND t.type <> 'REFUND'
                  AND o.order_status = 'COMPLETED'
                  AND t.created_at >= :fromDateTime
                  AND t.created_at < :toDateTimeExclusive
                GROUP BY t.order_id, o.user_id
            ) paid
            """, nativeQuery = true)
    SalesSummaryProjection summarizeSales(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Order_Transactions t
            WHERE t.type = 'REFUND'
              AND t.status = 'SUCCESS'
              AND t.created_at >= :fromDateTime
              AND t.created_at < :toDateTimeExclusive
            """, nativeQuery = true)
    BigDecimal sumSuccessfulRefunds(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

    @Query(value = """
            WITH DateSeries AS (
                SELECT CAST(:fromDate AS DATE) AS reportDate
                UNION ALL
                SELECT DATEADD(DAY, 1, reportDate)
                FROM DateSeries
                WHERE reportDate < CAST(:toDate AS DATE)
            ),
            CompletedPaidOrders AS (
                SELECT
                    t.order_id,
                    CAST(MIN(t.created_at) AS DATE) AS reportDate,
                    SUM(t.amount) AS completedSales
                FROM Order_Transactions t
                INNER JOIN Orders o ON o.order_id = t.order_id
                WHERE t.status = 'SUCCESS'
                  AND t.type <> 'REFUND'
                  AND o.order_status = 'COMPLETED'
                  AND t.created_at >= :fromDateTime
                  AND t.created_at < :toDateTimeExclusive
                GROUP BY t.order_id
            ),
            UnitsByDay AS (
                SELECT
                    completed_order.reportDate,
                    SUM(oi.quantity) AS unitsSold
                FROM CompletedPaidOrders completed_order
                INNER JOIN Order_Items oi ON oi.order_id = completed_order.order_id
                GROUP BY completed_order.reportDate
            )
            SELECT
                ds.reportDate AS reportDate,
                COALESCE(SUM(completed_order.completedSales), 0) AS completedSales,
                COUNT_BIG(completed_order.order_id) AS completedOrders,
                COALESCE(MAX(units.unitsSold), 0) AS unitsSold
            FROM DateSeries ds
            LEFT JOIN CompletedPaidOrders completed_order
                ON completed_order.reportDate = ds.reportDate
            LEFT JOIN UnitsByDay units
                ON units.reportDate = ds.reportDate
            GROUP BY ds.reportDate
            ORDER BY ds.reportDate
            OPTION (MAXRECURSION 2000)
            """, nativeQuery = true)
    List<SalesTrendProjection> findSalesTrend(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

    @Query(value = """
            WITH PaidOrders AS (
                SELECT
                    t.order_id,
                    MIN(t.created_at) AS paid_at
                FROM Order_Transactions t
                INNER JOIN Orders o ON o.order_id = t.order_id
                WHERE t.status = 'SUCCESS'
                  AND t.type <> 'REFUND'
                  AND o.order_status = 'COMPLETED'
                  AND t.created_at >= :previousFrom
                  AND t.created_at < :currentToExclusive
                GROUP BY t.order_id
            )
            SELECT
                p.product_id AS productId,
                p.product_name AS productName,
                p.brand AS brand,
                COALESCE(SUM(CASE
                    WHEN paid.paid_at >= :currentFrom
                     AND paid.paid_at < :currentToExclusive
                    THEN oi.quantity ELSE 0 END), 0) AS currentUnits,
                COALESCE(SUM(CASE
                    WHEN paid.paid_at >= :previousFrom
                     AND paid.paid_at < :previousToExclusive
                    THEN oi.quantity ELSE 0 END), 0) AS previousUnits,
                COALESCE(SUM(CASE
                    WHEN paid.paid_at >= :currentFrom
                     AND paid.paid_at < :currentToExclusive
                    THEN oi.quantity * oi.price_at_purchase ELSE 0 END), 0)
                    AS currentMerchandiseSales,
                COALESCE(SUM(CASE
                    WHEN paid.paid_at >= :previousFrom
                     AND paid.paid_at < :previousToExclusive
                    THEN oi.quantity * oi.price_at_purchase ELSE 0 END), 0)
                    AS previousMerchandiseSales
            FROM Order_Items oi
            INNER JOIN PaidOrders paid ON paid.order_id = oi.order_id
            INNER JOIN Product_Skus sku ON sku.sku_id = oi.sku_id
            INNER JOIN Products p ON p.product_id = sku.product_id
            GROUP BY p.product_id, p.product_name, p.brand
            HAVING SUM(CASE
                WHEN paid.paid_at >= :currentFrom
                 AND paid.paid_at < :currentToExclusive
                THEN oi.quantity ELSE 0 END) > 0
            ORDER BY currentUnits DESC, currentMerchandiseSales DESC
            """, nativeQuery = true)
    List<ProductTrendProjection> findProductTrends(
            @Param("currentFrom") LocalDateTime currentFrom,
            @Param("currentToExclusive") LocalDateTime currentToExclusive,
            @Param("previousFrom") LocalDateTime previousFrom,
            @Param("previousToExclusive") LocalDateTime previousToExclusive);

    /**
     * Marcus thêm: mỗi đơn chỉ lấy một sự kiện CANCELLED mới nhất và gom ghi chú
     * về nhóm nghiệp vụ an toàn trước khi đưa sang Analytics/AI.
     */
    @Query(value = """
            WITH CancelEvents AS (
                SELECT
                    history.order_id,
                    history.note,
                    history.created_at,
                    ROW_NUMBER() OVER (
                        PARTITION BY history.order_id
                        ORDER BY history.created_at DESC, history.history_id DESC
                    ) AS eventRank
                FROM Order_Status_History history
                WHERE history.status = 'CANCELLED'
            ),
            NormalizedReasons AS (
                SELECT
                    CASE
                        WHEN note LIKE N'%Đặt nhầm sản phẩm hoặc số lượng%'
                            THEN N'Đặt nhầm sản phẩm hoặc số lượng'
                        WHEN note LIKE N'%thay đổi địa chỉ nhận hàng%'
                            THEN N'Muốn thay đổi địa chỉ nhận hàng'
                        WHEN note LIKE N'%sản phẩm hoặc giá phù hợp hơn%'
                            THEN N'Tìm được sản phẩm hoặc giá phù hợp hơn'
                        WHEN note LIKE N'%Thời gian giao hàng không phù hợp%'
                            THEN N'Thời gian giao hàng không phù hợp'
                        WHEN note LIKE N'%Không còn nhu cầu mua%'
                            THEN N'Không còn nhu cầu mua'
                        WHEN UPPER(COALESCE(note, '')) LIKE '%VNPAY%'
                          OR LOWER(COALESCE(note, '')) LIKE N'%thanh toán%'
                          OR LOWER(COALESCE(note, '')) LIKE N'%hết hạn%'
                            THEN N'Thanh toán gián đoạn hoặc hết hạn'
                        WHEN note IS NULL OR LTRIM(RTRIM(note)) = ''
                            THEN N'Không ghi nhận lý do'
                        ELSE N'Lý do khác'
                    END AS reason
                FROM CancelEvents
                WHERE eventRank = 1
                  AND created_at >= :fromDateTime
                  AND created_at < :toDateTimeExclusive
            )
            SELECT reason, COUNT_BIG(*) AS reasonCount
            FROM NormalizedReasons
            GROUP BY reason
            ORDER BY reasonCount DESC, reason
            """, nativeQuery = true)
    List<CancellationReasonProjection> findCancellationReasons(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

    /**
     * Marcus thêm: Analytics chỉ đọc trạng thái/lý do bảo hành đã chuẩn hóa;
     * không lấy mô tả khách nhập, admin_note, người dùng hay file đính kèm.
     */
    @Query(value = """
            SELECT
                COUNT_BIG(*) AS totalRequests,
                COALESCE(SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pendingRequests,
                COALESCE(SUM(CASE WHEN status = 'CONFIRMED' THEN 1 ELSE 0 END), 0) AS processingRequests,
                COALESCE(SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END), 0) AS approvedRequests,
                COALESCE(SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END), 0) AS rejectedRequests
            FROM Warranty_Returns
            WHERE created_at >= :fromDateTime
              AND created_at < :toDateTimeExclusive
            """, nativeQuery = true)
    WarrantySummaryProjection summarizeWarranties(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

    @Query(value = """
            SELECT reason, COUNT_BIG(*) AS reasonCount
            FROM Warranty_Returns
            WHERE created_at >= :fromDateTime
              AND created_at < :toDateTimeExclusive
            GROUP BY reason
            ORDER BY reasonCount DESC, reason
            """, nativeQuery = true)
    List<WarrantyReasonProjection> findWarrantyReasons(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

    @Query(value = """
            SELECT
                p.product_id AS productId,
                p.product_name AS productName,
                p.brand AS brand,
                SUM(CASE WHEN warranty.created_at >= :currentFrom
                          AND warranty.created_at < :currentToExclusive THEN 1 ELSE 0 END) AS currentRequests,
                SUM(CASE WHEN warranty.created_at >= :previousFrom
                          AND warranty.created_at < :previousToExclusive THEN 1 ELSE 0 END) AS previousRequests,
                SUM(CASE WHEN warranty.created_at >= :currentFrom
                          AND warranty.created_at < :currentToExclusive
                          AND warranty.status = 'APPROVED' THEN 1 ELSE 0 END) AS approvedRequests,
                SUM(CASE WHEN warranty.created_at >= :currentFrom
                          AND warranty.created_at < :currentToExclusive
                          AND warranty.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejectedRequests
            FROM Warranty_Returns warranty
            INNER JOIN Order_Items item ON item.order_item_id = warranty.order_item_id
            INNER JOIN Product_Skus sku ON sku.sku_id = item.sku_id
            INNER JOIN Products p ON p.product_id = sku.product_id
            WHERE warranty.created_at >= :previousFrom
              AND warranty.created_at < :currentToExclusive
            GROUP BY p.product_id, p.product_name, p.brand
            HAVING SUM(CASE WHEN warranty.created_at >= :currentFrom
                             AND warranty.created_at < :currentToExclusive THEN 1 ELSE 0 END) > 0
            ORDER BY currentRequests DESC, productName
            """, nativeQuery = true)
    List<ProductWarrantyProjection> findProductWarrantyQuality(
            @Param("currentFrom") LocalDateTime currentFrom,
            @Param("currentToExclusive") LocalDateTime currentToExclusive,
            @Param("previousFrom") LocalDateTime previousFrom,
            @Param("previousToExclusive") LocalDateTime previousToExclusive);

    interface SalesSummaryProjection {
        Long getTotalOrders();

        Long getCompletedOrders();

        Long getCohortCompletedOrders();

        Long getCancelledOrders();

        BigDecimal getCompletedSales();

        Long getUnitsSold();

        Long getOrderingCustomers();
    }

    interface SalesTrendProjection {
        LocalDate getReportDate();

        BigDecimal getCompletedSales();

        Long getCompletedOrders();

        Long getUnitsSold();
    }

    @Query(value = """
            SELECT TOP (?)
                p.product_id    AS productId,
                p.product_name  AS productName,
                p.slug          AS slug,
                p.thumbnail_url AS thumbnailUrl,
                p.brand         AS brand,
                COALESCE(SUM(oi.quantity), 0) AS unitsSold
            FROM Order_Items oi
            INNER JOIN Order_Transactions t ON t.order_id = oi.order_id
            INNER JOIN Orders o ON o.order_id = oi.order_id
            INNER JOIN Product_Skus sku ON sku.sku_id = oi.sku_id
            INNER JOIN Products p ON p.product_id = sku.product_id
            WHERE t.status = 'SUCCESS'
              AND t.type <> 'REFUND'
              AND o.order_status = 'COMPLETED'
              AND o.is_hidden = 0
              AND p.status = 1
            GROUP BY p.product_id, p.product_name, p.slug, p.thumbnail_url, p.brand
            ORDER BY unitsSold DESC
            """, nativeQuery = true)
    List<BestSellerProjection> findBestSellers(int limit);

    interface BestSellerProjection {
        Integer getProductId();

        String getProductName();

        String getSlug();

        String getThumbnailUrl();

        String getBrand();

        Long getUnitsSold();
    }

    interface ProductTrendProjection {
        Integer getProductId();

        String getProductName();

        String getBrand();

        Long getCurrentUnits();

        Long getPreviousUnits();

        BigDecimal getCurrentMerchandiseSales();

        BigDecimal getPreviousMerchandiseSales();
    }

    interface CancellationReasonProjection {
        String getReason();

        Long getReasonCount();
    }

    interface WarrantySummaryProjection {
        Long getTotalRequests();

        Long getPendingRequests();

        Long getProcessingRequests();

        Long getApprovedRequests();

        Long getRejectedRequests();
    }

    interface WarrantyReasonProjection {
        String getReason();

        Long getReasonCount();
    }

    interface ProductWarrantyProjection {
        Integer getProductId();

        String getProductName();

        String getBrand();

        Long getCurrentRequests();

        Long getPreviousRequests();

        Long getApprovedRequests();

        Long getRejectedRequests();
    }
}
