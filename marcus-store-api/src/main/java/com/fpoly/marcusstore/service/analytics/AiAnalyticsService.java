package com.fpoly.marcusstore.service.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpoly.marcusstore.dto.analytics.AiAnalyticsReportResponse;
import com.fpoly.marcusstore.dto.analytics.AiAnalyticsReportResponse.Action;
import com.fpoly.marcusstore.dto.analytics.AiAnalyticsReportResponse.ProductOutlook;
import com.fpoly.marcusstore.dto.analytics.AiAnalyticsReportResponse.Signal;
import com.fpoly.marcusstore.dto.analytics.AnalyticsOverviewResponse;
import com.fpoly.marcusstore.dto.analytics.AnalyticsPeriod;
import com.fpoly.marcusstore.dto.analytics.AnalyticsTrendPoint;
import com.fpoly.marcusstore.dto.analytics.ProductTrendResponse;
import com.fpoly.marcusstore.entity.analytics.AiAnalyticsReport;
import com.fpoly.marcusstore.repository.analytics.AiAnalyticsReportRepository;
import com.fpoly.marcusstore.service.ai.AiUsageEventService;
import com.fpoly.marcusstore.service.ai.GeminiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiAnalyticsService {

        // Marcus sửa: đổi version để loại báo cáo cache còn dài và lộ tên khóa JSON kỹ
        // thuật.
        private static final String METRIC_SCHEMA_VERSION = "decision-readable-v8";
        private static final int PRODUCT_LIMIT = 10;
        private static final Set<String> OUTLOOKS = Set.of("GROWTH", "STEADY", "DECLINE", "UNCERTAIN");
        private static final Set<String> SEVERITIES = Set.of("POSITIVE", "INFO", "WARNING", "CRITICAL");
        private static final Set<String> PRIORITIES = Set.of("HIGH", "MEDIUM", "LOW");
        private static final Set<String> DIRECTIONS = Set.of("UP", "STEADY", "DOWN", "UNCERTAIN");

        private final AnalyticsService analyticsService;
        private final ObjectMapper objectMapper;
        private final AiAnalyticsReportRepository reportRepository;
        private final AiUsageEventService aiUsageEventService;
        private final GeminiClient geminiClient;
        private final Map<CacheKey, ReentrantLock> generationLocks = new ConcurrentHashMap<>();

        @Autowired
        public AiAnalyticsService(AnalyticsService analyticsService, ObjectMapper objectMapper,
                        AiAnalyticsReportRepository reportRepository,
                        AiUsageEventService aiUsageEventService,
                        GeminiClient geminiClient) {
                this.analyticsService = analyticsService;
                this.objectMapper = objectMapper;
                this.reportRepository = reportRepository;
                this.aiUsageEventService = aiUsageEventService;
                this.geminiClient = geminiClient;
        }

        // Marcus thêm: constructor hẹp cho unit test fallback, không gọi provider.
        AiAnalyticsService(AnalyticsService analyticsService, ObjectMapper objectMapper,
                        AiAnalyticsReportRepository reportRepository,
                        AiUsageEventService aiUsageEventService) {
                this(analyticsService, objectMapper, reportRepository, aiUsageEventService, null);
        }

        /**
         * Marcus thêm: Gemini chỉ nhận DTO tổng hợp từ AnalyticsService. Service
         * không có Repository nghiệp vụ nên AI không thể tự truy vấn hoặc thay đổi
         * đơn hàng, khách hàng hay sản phẩm. Repository duy nhất chỉ lưu bản tin AI.
         */
        public AiAnalyticsReportResponse findLatestReport(LocalDate fromDate, LocalDate toDate) {
                AnalyticsPeriod period = analyticsService.resolvePeriod(fromDate, toDate);
                return reportRepository
                                .findFirstByFromDateAndToDateAndModelNameOrderByGeneratedAtDesc(
                                                period.fromDate(),
                                                period.toDate(),
                                                cacheModelName())
                                .map(this::readStoredReport)
                                .orElse(null);
        }

        public AiAnalyticsReportResponse generateReport(LocalDate fromDate, LocalDate toDate) {
                AnalyticsOverviewResponse overview = analyticsService.getOverview(fromDate, toDate);
                CacheKey cacheKey = new CacheKey(
                                overview.period().fromDate(),
                                overview.period().toDate());
                ReentrantLock lock = generationLocks.computeIfAbsent(cacheKey, ignored -> new ReentrantLock());
                if (!lock.tryLock()) {
                        AiAnalyticsReportResponse stored = findLatestReport(fromDate, toDate);
                        if (stored != null) {
                                return stored;
                        }
                        throw new IllegalStateException("Báo cáo cho kỳ này đang được tạo. Vui lòng chờ ít phút.");
                }
                try {
                        return generateLocked(cacheKey, overview);
                } finally {
                        lock.unlock();
                        generationLocks.remove(cacheKey, lock);
                }
        }

        private AiAnalyticsReportResponse generateLocked(
                        CacheKey cacheKey,
                        AnalyticsOverviewResponse overview) {
                LocalDateTime now = LocalDateTime.now();
                List<ProductTrendResponse> products = analyticsService.getProductTrends(
                                overview.period().fromDate(),
                                overview.period().toDate(),
                                PRODUCT_LIMIT);
                List<AnalyticsTrendPoint> rawTrend = analyticsService.getSalesTrend(
                                overview.period().fromDate(), overview.period().toDate());
                List<Map<String, Object>> salesTrendBuckets = summarizeSalesTrend(rawTrend);
                String context = buildSafeContext(
                                overview,
                                products,
                                analyticsService.getCancellationReasons(
                                                overview.period().fromDate(), overview.period().toDate()),
                                salesTrendBuckets,
                                buildAlgorithmEvidence(salesTrendBuckets));
                String fingerprint = fingerprint(context);
                AiAnalyticsReportResponse exactCache = findStoredReport(cacheKey, fingerprint);
                if (exactCache != null) {
                        return exactCache;
                }
                AiAnalyticsReportResponse report;
                try {
                        if (geminiClient == null || !geminiClient.isConfigured()) {
                                throw new IllegalStateException("Gemini chưa cấu hình.");
                        }
                        JsonNode providerResponse = callGemini(context);
                        report = parseReport(providerResponse, products, now, now);
                } catch (RuntimeException providerFailure) {
                        // Marcus thêm: Gemini timeout/hết quota không được làm mất phân tích
                        // thuật toán; báo cáo fallback vẫn có bằng chứng và cách kiểm chứng.
                        report = algorithmFallback(overview, products, now);
                }

                saveReport(cacheKey, fingerprint, report);
                return report;
        }

        private AiAnalyticsReportResponse findStoredReport(CacheKey key, String fingerprint) {
                return reportRepository
                                .findFirstByFromDateAndToDateAndModelNameAndDataFingerprintOrderByGeneratedAtDesc(
                                                key.fromDate(), key.toDate(), cacheModelName(), fingerprint)
                                .map(this::readStoredReport)
                                .orElse(null);
        }

        private void saveReport(CacheKey key, String fingerprint, AiAnalyticsReportResponse report) {
                try {
                        AiAnalyticsReport entity = new AiAnalyticsReport();
                        entity.setFromDate(key.fromDate());
                        entity.setToDate(key.toDate());
                        entity.setReportJson(objectMapper.writeValueAsString(report));
                        entity.setModelName(cacheModelName());
                        entity.setDataFingerprint(fingerprint);
                        entity.setGeneratedAt(report.generatedAt());
                        reportRepository.save(entity);
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "AI đã phân tích nhưng không thể lưu báo cáo. Vui lòng kiểm tra database.");
                }
        }

        private String fingerprint(String safeContext) {
                try {
                        byte[] digest = MessageDigest.getInstance("SHA-256")
                                        .digest(safeContext.getBytes(StandardCharsets.UTF_8));
                        return java.util.HexFormat.of().formatHex(digest);
                } catch (Exception exception) {
                        throw new IllegalStateException("Không thể tạo dấu vân tay dữ liệu Analytics.");
                }
        }

        private AiAnalyticsReportResponse readStoredReport(AiAnalyticsReport entity) {
                try {
                        return objectMapper
                                        .readValue(entity.getReportJson(), AiAnalyticsReportResponse.class)
                                        .asCached();
                } catch (Exception exception) {
                        throw new IllegalStateException("Báo cáo AI đã lưu không còn đúng định dạng.");
                }
        }

        private String buildSafeContext(
                        AnalyticsOverviewResponse overview,
                        List<ProductTrendResponse> products,
                        List<com.fpoly.marcusstore.dto.analytics.CancellationReasonResponse> cancellationReasons,
                        List<Map<String, Object>> salesTrendBuckets,
                        Map<String, Object> algorithmEvidence) {
                Map<String, Object> context = new LinkedHashMap<>();
                // Marcus thêm: gửi định nghĩa chỉ số để AI không diễn giải final_amount
                // của đơn chưa thu được tiền thành doanh thu.
                context.put("metricPolicy", Map.of(
                                "completedSales",
                                "SUCCESS COD/VNPAY transactions of COMPLETED orders, grouped by transaction date",
                                "successfulRefundAmount",
                                "SUCCESS REFUND transactions, grouped by transaction date",
                                "profitAvailable",
                                false));
                context.put("period", overview.period());
                context.put("completedSales", overview.completedSales());
                context.put("completedOrders", overview.completedOrders());
                context.put("unitsSold", overview.unitsSold());
                context.put("averageOrderValue", overview.averageOrderValue());
                context.put("completionRate", overview.completionRate());
                context.put("cancellationRate", overview.cancellationRate());
                context.put("successfulRefundAmount", overview.successfulRefundAmount());
                context.put("orderingCustomers", overview.orderingCustomers());
                context.put("productTrends", products);
                // Marcus nâng cấp: nén chuỗi thời gian thành tối đa 12 giai đoạn để AI
                // nhận ra đà tăng/giảm mà vẫn tiết kiệm quota Gemini miễn phí.
                context.put("salesTrendBuckets", salesTrendBuckets);
                // Marcus thêm: Gemini đọc trực tiếp kết quả thuật toán đã tính và
                // kiểm chứng, không tự bịa lại hệ số hồi quy hay ngày bất thường.
                context.put("algorithmEvidence", algorithmEvidence);
                // Marcus thêm: AI chỉ nhận nhóm lý do và số đếm đã chuẩn hóa, không nhận
                // ghi chú hủy tự do hay thông tin nhận diện khách hàng.
                context.put("cancellationReasons", cancellationReasons);
                // Marcus thêm: chỉ chuyển các số đếm/lý do enum theo sản phẩm; tuyệt đối
                // không gửi description, admin_note, user hay attachment của bảo hành.
                context.put(
                                "warrantyQuality",
                                analyticsService.getWarrantyAnalytics(
                                                overview.period().fromDate(), overview.period().toDate(),
                                                PRODUCT_LIMIT));
                try {
                        // Marcus thêm: AI chỉ nhận thống kê hành vi đã tổng hợp, không nhận
                        // sessionId hay nội dung câu hỏi của từng khách.
                        context.put(
                                        "aiAdvisorUsage",
                                        aiUsageEventService.summarize(
                                                        overview.period().fromDate(),
                                                        overview.period().toDate()));
                } catch (RuntimeException ignored) {
                        // Chưa chạy migration telemetry vẫn cho phép phân tích số liệu bán hàng.
                }
                try {
                        return objectMapper.writeValueAsString(context);
                } catch (Exception exception) {
                        throw new IllegalStateException("Không thể chuẩn bị dữ liệu tổng hợp cho AI.");
                }
        }

        private Map<String, Object> buildAlgorithmEvidence(List<Map<String, Object>> buckets) {
                List<Double> values = buckets.stream()
                                .map(bucket -> ((java.math.BigDecimal) bucket.get("completedSales")).doubleValue())
                                .toList();
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("formula", "OLS yHat = intercept + slope*x; bounds = yHat +/- RMSE");
                evidence.put("sampleSize", values.size());
                if (values.size() < 3) {
                        evidence.put("available", false);
                        evidence.put("reason", "Cần ít nhất 3 giai đoạn doanh thu.");
                        return evidence;
                }

                RegressionMetric full = regression(values);
                evidence.put("available", true);
                evidence.put("slope", roundMetric(full.slope()));
                evidence.put("rSquared", roundMetric(full.rSquared()));
                evidence.put("rmse", Math.round(full.rmse()));

                if (values.size() >= 6) {
                        int holdout = Math.min(3, Math.max(1, values.size() / 5));
                        List<Double> training = values.subList(0, values.size() - holdout);
                        RegressionMetric trained = regression(training);
                        double absolutePercentageError = 0;
                        int percentageSamples = 0;
                        for (int index = 0; index < holdout; index++) {
                                double actual = values.get(training.size() + index);
                                double predicted = Math.max(0,
                                                trained.intercept() + trained.slope() * (training.size() + index));
                                if (actual > 0) {
                                        absolutePercentageError += Math.abs(actual - predicted) / actual * 100;
                                        percentageSamples++;
                                }
                        }
                        evidence.put("backtestPeriods", holdout);
                        evidence.put("backtestMape", percentageSamples == 0 ? null
                                        : roundMetric(absolutePercentageError / percentageSamples));
                }

                List<Map<String, Object>> anomalies = new ArrayList<>();
                if (full.rmse() > 0 && values.size() >= 5) {
                        for (int index = 0; index < values.size(); index++) {
                                double expected = Math.max(0, full.intercept() + full.slope() * index);
                                double score = Math.abs(values.get(index) - expected) / full.rmse();
                                if (score >= 2) {
                                        anomalies.add(Map.of(
                                                        "fromDate", buckets.get(index).get("fromDate"),
                                                        "toDate", buckets.get(index).get("toDate"),
                                                        "actualSales", Math.round(values.get(index)),
                                                        "expectedSales", Math.round(expected),
                                                        "deviationRmse", roundMetric(score)));
                                }
                        }
                }
                evidence.put("anomalies", anomalies.stream().limit(3).toList());
                return evidence;
        }

        private RegressionMetric regression(List<Double> values) {
                double xMean = (values.size() - 1) / 2D;
                double yMean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double numerator = 0;
                double denominator = 0;
                for (int index = 0; index < values.size(); index++) {
                        numerator += (index - xMean) * (values.get(index) - yMean);
                        denominator += Math.pow(index - xMean, 2);
                }
                double slope = denominator == 0 ? 0 : numerator / denominator;
                double intercept = yMean - slope * xMean;
                double residualSum = 0;
                double totalSum = 0;
                for (int index = 0; index < values.size(); index++) {
                        residualSum += Math.pow(values.get(index) - (intercept + slope * index), 2);
                        totalSum += Math.pow(values.get(index) - yMean, 2);
                }
                double rmse = Math.sqrt(residualSum / values.size());
                double rSquared = totalSum == 0 ? 0 : Math.max(0, 1 - residualSum / totalSum);
                return new RegressionMetric(slope, intercept, rmse, rSquared);
        }

        private double roundMetric(double value) {
                return Math.round(value * 100D) / 100D;
        }

        private List<Map<String, Object>> summarizeSalesTrend(List<AnalyticsTrendPoint> trend) {
                if (trend == null || trend.isEmpty()) {
                        return List.of();
                }
                int bucketSize = Math.max(1, (int) Math.ceil(trend.size() / 12D));
                List<Map<String, Object>> buckets = new ArrayList<>();
                for (int start = 0; start < trend.size(); start += bucketSize) {
                        int end = Math.min(start + bucketSize, trend.size());
                        List<AnalyticsTrendPoint> points = trend.subList(start, end);
                        Map<String, Object> bucket = new LinkedHashMap<>();
                        bucket.put("fromDate", points.getFirst().date());
                        bucket.put("toDate", points.getLast().date());
                        bucket.put("completedSales", points.stream()
                                        .map(AnalyticsTrendPoint::completedSales)
                                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
                        bucket.put("completedOrders", points.stream()
                                        .mapToLong(AnalyticsTrendPoint::completedOrders)
                                        .sum());
                        bucket.put("unitsSold", points.stream()
                                        .mapToLong(AnalyticsTrendPoint::unitsSold)
                                        .sum());
                        buckets.add(bucket);
                }
                return buckets;
        }

        private JsonNode callGemini(String context) {
                try {
                        return geminiClient.generate(
                                        systemInstructions(),
                                        "DỮ LIỆU TỔNG HỢP ĐÃ KIỂM DUYỆT:\n" + context,
                                        analyticsResponseSchema(), 2_400);
                } catch (GeminiClient.GeminiClientException exception) {
                        throw new IllegalStateException(providerErrorMessage(exception.statusCode()));
                }
        }

        private String systemInstructions() {
                return """
                                Bạn là Marcus AI Business Analyst dành cho chủ cửa hàng thương mại điện tử.
                                Chỉ phân tích JSON tổng hợp được cung cấp. Không yêu cầu hoặc suy đoán dữ liệu khách hàng.
                                Không gọi doanh thu là lợi nhuận vì hệ thống không có giá nhập.
                                completedSales chỉ là tiền đã thu SUCCESS của đơn COMPLETED, không phải tổng final_amount.
                                successfulRefundAmount là tiền REFUND đã SUCCESS; PENDING/FAILED không được coi là đã hoàn.
                                Không bịa tin thị trường, tồn kho, nguyên nhân hoặc con số tương lai.
                                aiAdvisorUsage chỉ là số liệu tổng hợp ẩn danh; dùng để đánh giá mức khách tương tác với tư vấn AI.
                                cancellationReasons là thống kê lý do đã chuẩn hóa; dùng nó để giải thích tỷ lệ hủy, không tự bịa nguyên nhân.
                                warrantyQuality là dữ liệu bảo hành tổng hợp theo ngày tạo yêu cầu và theo sản phẩm.
                                Dùng warrantyQuality để phát hiện áp lực hậu mãi, nhóm lý do nổi bật và sản phẩm cần kiểm tra chất lượng.
                                Không gọi số yêu cầu bảo hành là tỷ lệ lỗi tuyệt đối vì yêu cầu có thể phát sinh sau kỳ bán hàng.
                                approvalRate chỉ tính trên yêu cầu đã APPROVED hoặc REJECTED; PENDING/CONFIRMED chưa có kết luận.
                                Nếu một sản phẩm có ít yêu cầu, phải ghi rõ dữ liệu còn ít và không kết luận chất lượng kém.
                                salesTrendBuckets là chuỗi thời gian đã nén theo thứ tự cũ đến mới;
                                dùng để nhận diện đà tăng/giảm, điểm bứt phá và mức biến động thay vì chỉ đọc tổng KPI.
                                algorithmEvidence là kết quả OLS/backtest/phát hiện bất thường do backend tính sẵn.
                                Chỉ được nhắc hệ số, sai số hoặc ngày bất thường có trong algorithmEvidence;
                                dùng backtestMape và rSquared để hạ mức tin cậy khi mô hình yếu.
                                Không đưa ra con số dự báo doanh thu tuyệt đối nếu chuỗi biến động mạnh hoặc dữ liệu quá ít.
                                Mọi kết luận phải gắn với evidence có số liệu trong JSON.
                                Mỗi signal bắt buộc có confidence, action và verification; cách kiểm chứng phải chỉ rõ số liệu cần xem ở kỳ sau.
                                Có thể dự đoán HƯỚNG tăng/đi ngang/giảm của sản phẩm, nhưng phải dùng UNCERTAIN khi dữ liệu yếu.
                                headline và executiveSummary phải ưu tiên triển vọng sắp tới, rủi ro chính và quyết định quản lý;
                                không chỉ liệt kê lại KPI giống dashboard.
                                Headline phải là một KẾT LUẬN có hướng hành động, không dùng tiêu đề chung như “Tổng quan kinh doanh”.
                                ExecutiveSummary phải trả lời đủ ba ý: điều gì có khả năng xảy ra, bằng chứng mạnh nhất là gì,
                                và chủ cửa hàng nên ưu tiên quyết định nào ngay trong kỳ kế tiếp.
                                Tuyệt đối không in tên khóa JSON hoặc thuật ngữ nội bộ như completedSales, successfulRefundAmount,
                                rSquared, backtestMape, productId, salesTrendBuckets, algorithmEvidence, slope.
                                Dùng từ quản trị dễ hiểu tương ứng: doanh thu đã thu của đơn hoàn tất, tiền hoàn thành công,
                                độ phù hợp của mô hình, mức sai lệch dự báo khi đối chiếu dữ liệu cũ, sản phẩm và xu hướng tăng/giảm/đi ngang.
                                Không in hệ số xu hướng dạng số khoa học như -2.23e7; chỉ diễn giải hướng và mức độ biến động.
                                Mọi tỷ lệ phải dùng ký hiệu %, không viết chữ “phần trăm”. Tiền phải dùng dấu chấm phân cách
                                hàng nghìn và kết thúc bằng VNĐ. Không dùng dấu chấm phẩy trong câu trả lời.
                                ExecutiveSummary chỉ 1-2 câu, tối đa khoảng 280 ký tự. Mỗi evidence, interpretation,
                                action, verification và reason chỉ 1 câu ngắn, ưu tiên con số dễ đọc và việc có thể làm ngay.
                                Không lặp cùng một nhận xét ở headline, signal và action.
                                Sắp xếp productOutlooks theo mức cần hành động: xu hướng tăng rõ, xu hướng giảm rõ, rồi mới chưa chắc chắn.
                                Signals ưu tiên tín hiệu có giá trị dự báo, biến động lý do hủy và hành vi khách; tránh lặp cùng một KPI.
                                Actions phải cụ thể, có thể thực hiện trong kỳ kế tiếp và nói rõ căn cứ dữ liệu.
                                Nếu changePercent là null, hiểu là kỳ trước bằng 0; không tự biến thành phần trăm tăng.
                                Viết tiếng Việt rõ, ngắn, phù hợp người quản lý cửa hàng.
                                Chỉ nhắc productId có trong productTrends.
                                Tối đa 4 signals, 3 actions và 5 productOutlooks.
                                Trả JSON thuần, không Markdown, đúng cấu trúc:
                                {
                                  "headline":"một kết luận nổi bật",
                                  "executiveSummary":"tóm tắt 1-2 câu ngắn: xu hướng, căn cứ và ưu tiên",
                                  "outlook":"GROWTH|STEADY|DECLINE|UNCERTAIN",
                                  "confidence":"HIGH|MEDIUM|LOW",
                                  "signals":[
                                    {"title":"...","evidence":"...","interpretation":"...","severity":"POSITIVE|INFO|WARNING|CRITICAL"}
                                  ],
                                  "actions":[
                                    {"title":"...","reason":"...","priority":"HIGH|MEDIUM|LOW"}
                                  ],
                                  "productOutlooks":[
                                    {"productId":1,"direction":"UP|STEADY|DOWN|UNCERTAIN","reason":"..."}
                                  ]
                                }
                                """;
        }

        private String cacheModelName() {
                return METRIC_SCHEMA_VERSION + "|"
                                + (geminiClient == null ? "algorithm" : geminiClient.modelName());
        }

        // Marcus thêm: structured output khóa hình dạng báo cáo ngay từ Gemini,
        // thay vì chỉ hy vọng model làm đúng phần mô tả trong prompt.
        private Map<String, Object> analyticsResponseSchema() {
                Map<String, Object> signal = Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                                "title", Map.of("type", "string"),
                                                "evidence", Map.of("type", "string"),
                                                "interpretation", Map.of("type", "string"),
                                                "confidence", Map.of("type", "string", "enum", List.copyOf(PRIORITIES)),
                                                "action", Map.of("type", "string"),
                                                "verification", Map.of("type", "string"),
                                                "severity", Map.of("type", "string", "enum", List.copyOf(SEVERITIES))),
                                "required", List.of("title", "evidence", "interpretation", "confidence", "action",
                                                "verification", "severity"));
                Map<String, Object> action = Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                                "title", Map.of("type", "string"),
                                                "reason", Map.of("type", "string"),
                                                "priority", Map.of("type", "string", "enum", List.copyOf(PRIORITIES))),
                                "required", List.of("title", "reason", "priority"));
                Map<String, Object> productOutlook = Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                                "productId", Map.of("type", "integer"),
                                                "direction", Map.of("type", "string", "enum", List.copyOf(DIRECTIONS)),
                                                "reason", Map.of("type", "string")),
                                "required", List.of("productId", "direction", "reason"));

                return Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                                "headline", Map.of("type", "string"),
                                                "executiveSummary", Map.of("type", "string"),
                                                "outlook", Map.of("type", "string", "enum", List.copyOf(OUTLOOKS)),
                                                "confidence", Map.of("type", "string", "enum", List.copyOf(PRIORITIES)),
                                                "signals", Map.of("type", "array", "maxItems", 4, "items", signal),
                                                "actions", Map.of("type", "array", "maxItems", 3, "items", action),
                                                "productOutlooks", Map.of(
                                                                "type", "array",
                                                                "maxItems", 5,
                                                                "items", productOutlook)),
                                "required", List.of(
                                                "headline",
                                                "executiveSummary",
                                                "outlook",
                                                "confidence",
                                                "signals",
                                                "actions",
                                                "productOutlooks"));
        }

        private AiAnalyticsReportResponse parseReport(
                        JsonNode response,
                        List<ProductTrendResponse> products,
                        LocalDateTime generatedAt,
                        LocalDateTime cachedUntil) {
                String output = extractOutputText(response);
                try {
                        JsonNode root = objectMapper.readTree(output);
                        Map<Integer, ProductTrendResponse> allowedProducts = products.stream()
                                        .collect(Collectors.toMap(ProductTrendResponse::productId,
                                                        Function.identity()));

                        List<Signal> signals = new ArrayList<>();
                        root.path("signals").forEach(node -> {
                                if (signals.size() < 4) {
                                        signals.add(new Signal(
                                                        "AI-SIGNAL-" + (signals.size() + 1),
                                                        safeText(node, "title", 100),
                                                        safeText(node, "evidence", 160),
                                                        safeText(node, "interpretation", 180),
                                                        allowedValue(node.path("confidence").asText(), PRIORITIES,
                                                                        "LOW"),
                                                        safeText(node, "action", 160),
                                                        safeText(node, "verification", 160),
                                                        allowedValue(node.path("severity").asText(), SEVERITIES,
                                                                        "INFO")));
                                }
                        });

                        List<Action> actions = new ArrayList<>();
                        root.path("actions").forEach(node -> {
                                if (actions.size() < 3) {
                                        actions.add(new Action(
                                                        safeText(node, "title", 120),
                                                        safeText(node, "reason", 160),
                                                        allowedValue(node.path("priority").asText(), PRIORITIES,
                                                                        "MEDIUM")));
                                }
                        });

                        List<ProductOutlook> productOutlooks = new ArrayList<>();
                        root.path("productOutlooks").forEach(node -> {
                                if (productOutlooks.size() >= 5) {
                                        return;
                                }
                                int productId = node.path("productId").asInt(-1);
                                ProductTrendResponse product = allowedProducts.get(productId);
                                if (product != null) {
                                        productOutlooks.add(new ProductOutlook(
                                                        productId,
                                                        product.productName(),
                                                        allowedValue(node.path("direction").asText(), DIRECTIONS,
                                                                        "UNCERTAIN"),
                                                        safeText(node, "reason", 160)));
                                }
                        });

                        return new AiAnalyticsReportResponse(
                                        generatedAt,
                                        cachedUntil,
                                        false,
                                        "AI",
                                        requiredText(root, "headline", 180),
                                        requiredText(root, "executiveSummary", 280),
                                        allowedValue(root.path("outlook").asText(), OUTLOOKS, "UNCERTAIN"),
                                        allowedValue(root.path("confidence").asText(), PRIORITIES, "LOW"),
                                        List.copyOf(signals),
                                        List.copyOf(actions),
                                        List.copyOf(productOutlooks),
                                        "Nhận định AI dựa trên dữ liệu tổng hợp và không thay thế quyết định của chủ cửa hàng.");
                } catch (Exception exception) {
                        throw new IllegalStateException("Gemini trả về bản phân tích chưa đúng định dạng.");
                }
        }

        private AiAnalyticsReportResponse algorithmFallback(
                        AnalyticsOverviewResponse overview,
                        List<ProductTrendResponse> products,
                        LocalDateTime generatedAt) {
                double salesChange = overview.completedSales().changePercent() == null
                                ? 0
                                : overview.completedSales().changePercent();
                String outlook = salesChange > 5 ? "GROWTH" : salesChange < -5 ? "DECLINE" : "STEADY";
                String direction = salesChange > 5 ? "tăng" : salesChange < -5 ? "giảm" : "đi ngang";
                List<Signal> signals = List.of(new Signal(
                                "ALG-SALES-CHANGE",
                                "Xu hướng doanh thu đã thu đang " + direction,
                                "Doanh thu kỳ này " + overview.completedSales().currentValue()
                                                + ", thay đổi "
                                                + String.format(java.util.Locale.ROOT, "%.1f%%", salesChange)
                                                + " so với kỳ trước.",
                                "Đây là phép so sánh kỳ từ giao dịch SUCCESS của đơn COMPLETED, không phải lợi nhuận.",
                                Math.abs(salesChange) >= 15 ? "MEDIUM" : "LOW",
                                salesChange < 0 ? "Rà nhóm sản phẩm giảm và lý do hủy trước khi điều chỉnh bán hàng."
                                                : "Theo dõi sản phẩm tăng và bảo đảm tồn kho cho kỳ tiếp theo.",
                                "So sánh doanh thu đã thu, số đơn hoàn thành và tỷ lệ hủy ở cùng độ dài kỳ tiếp theo.",
                                salesChange < -5 ? "WARNING" : salesChange > 5 ? "POSITIVE" : "INFO"));
                List<ProductOutlook> outlooks = products.stream().limit(3)
                                .map(product -> new ProductOutlook(
                                                product.productId(), product.productName(),
                                                product.currentUnits() > product.previousUnits() ? "UP"
                                                                : product.currentUnits() < product.previousUnits()
                                                                                ? "DOWN"
                                                                                : "STEADY",
                                                "Thuật toán so sánh " + product.currentUnits() + " sản phẩm kỳ này với "
                                                                + product.previousUnits() + " sản phẩm kỳ trước."))
                                .toList();
                return new AiAnalyticsReportResponse(
                                generatedAt, generatedAt, false, "ALGORITHM",
                                "Gemini tạm gián đoạn – hệ thống vẫn phát hiện xu hướng " + direction,
                                "Báo cáo này được tạo bằng thuật toán từ dữ liệu đã thu. Hãy dùng như tín hiệu vận hành và kiểm chứng lại ở kỳ tiếp theo.",
                                outlook, "LOW", signals,
                                List.of(new Action("Kiểm chứng xu hướng trong kỳ kế tiếp",
                                                "Đối chiếu doanh thu đã thu, đơn hoàn thành, hủy đơn và refund trên cùng khoảng thời gian.",
                                                "HIGH")),
                                outlooks,
                                "Kết quả fallback do thuật toán tạo vì Gemini timeout, hết quota hoặc trả sai định dạng; không phải nhận định AI.");
        }

        private String extractOutputText(JsonNode response) {
                String text = response == null
                                ? ""
                                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text")
                                                .asText("");
                if (text.isBlank()) {
                        throw new IllegalStateException("Gemini không trả về nội dung phân tích.");
                }
                String normalized = text
                                .replaceFirst("^```(?:json)?\\s*", "")
                                .replaceFirst("\\s*```$", "")
                                .trim();
                // Marcus sửa: chấp nhận trường hợp provider bọc JSON bằng một câu dẫn.
                int firstBrace = normalized.indexOf('{');
                int lastBrace = normalized.lastIndexOf('}');
                return firstBrace >= 0 && lastBrace > firstBrace
                                ? normalized.substring(firstBrace, lastBrace + 1)
                                : normalized;
        }

        private String requiredText(JsonNode node, String field, int maxLength) {
                String value = safeText(node, field, maxLength);
                if (value.isBlank()) {
                        throw new IllegalArgumentException("Thiếu trường " + field);
                }
                return value;
        }

        private String safeText(JsonNode node, String field, int maxLength) {
                String value = sanitizeManagementLanguage(node.path(field).asText(""));
                if (value.length() <= maxLength) {
                        return value;
                }
                // Marcus sửa: cắt tại ranh giới từ để câu kết luận không bị cụt giữa chữ.
                String shortened = value.substring(0, maxLength);
                int lastSpace = shortened.lastIndexOf(' ');
                return (lastSpace > maxLength * 0.7 ? shortened.substring(0, lastSpace) : shortened).trim() + "…";
        }

        /**
         * Marcus thêm: Gemini đọc JSON kỹ thuật nhưng màn quản trị chỉ được hiển thị
         * ngôn ngữ kinh doanh. Đây là lớp chặn cuối, không phụ thuộc model có tuân
         * prompt hay không.
         */
        private String sanitizeManagementLanguage(String raw) {
                String value = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
                value = replaceTrendCoefficient(value);
                value = replaceModelFit(value);
                value = replaceBacktestError(value);
                value = value.replaceAll("(?i)\\bcompletedSales\\b", "doanh thu đã thu của đơn hoàn tất")
                                .replaceAll("(?i)\\bsuccessfulRefundAmount\\b", "tiền hoàn thành công")
                                .replaceAll("(?i)\\brSquared\\b", "độ phù hợp của mô hình")
                                .replaceAll("(?i)\\bbacktestMape\\b", "mức sai lệch dự báo khi đối chiếu dữ liệu cũ")
                                .replaceAll("(?i)\\bproductId\\b", "sản phẩm")
                                .replaceAll("(?i)\\bsalesTrendBuckets\\b", "chuỗi doanh thu theo thời gian")
                                .replaceAll("(?i)\\balgorithmEvidence\\b", "kết quả thuật toán kiểm chứng")
                                .replaceAll("(?i)\\baiAdvisorUsage\\b", "hiệu quả tư vấn AI")
                                .replaceAll("(?i)\\bwarrantyQuality\\b", "dữ liệu bảo hành tổng hợp")
                                .replaceAll("(?i)(-?\\d+(?:[.,]\\d+)?)\\s*phần trăm", "$1%")
                                .replaceAll("(?i)\\bVND\\b", "VNĐ")
                                .replace(';', '.');
                return normalizePercentages(formatCurrencyAmounts(expandVietnameseCurrency(value)))
                                .replaceAll("\\s+", " ").trim();
        }

        private String normalizePercentages(String value) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                .compile("(\\d+)\\.(\\d+)%").matcher(value);
                return matcher.replaceAll("$1,$2%");
        }

        private String expandVietnameseCurrency(String value) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                .compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(tỷ|triệu)\\s*(?:đồng|VNĐ)?")
                                .matcher(value);
                StringBuffer result = new StringBuffer();
                while (matcher.find()) {
                        java.math.BigDecimal amount = new java.math.BigDecimal(matcher.group(1).replace(',', '.'));
                        long multiplier = matcher.group(2).equalsIgnoreCase("tỷ") ? 1_000_000_000L : 1_000_000L;
                        String formatted = new java.text.DecimalFormat("#,###")
                                        .format(amount.multiply(java.math.BigDecimal.valueOf(multiplier))
                                                        .toBigInteger())
                                        .replace(',', '.');
                        matcher.appendReplacement(result,
                                        java.util.regex.Matcher.quoteReplacement(formatted + " VNĐ"));
                }
                matcher.appendTail(result);
                return result.toString();
        }

        private String replaceBacktestError(String value) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                .compile("(?i)\\bbacktestMape\\s*[:=]?\\s*(\\d+(?:[.,]\\d+)?)%?")
                                .matcher(value);
                StringBuffer result = new StringBuffer();
                while (matcher.find()) {
                        String replacement = "mức sai lệch dự báo khi đối chiếu dữ liệu cũ "
                                        + matcher.group(1).replace('.', ',') + "%";
                        matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                }
                matcher.appendTail(result);
                return result.toString();
        }

        private String formatCurrencyAmounts(String value) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                .compile("(?<![\\d.,])(\\d{4,})(?:\\.0+)?\\s*(VNĐ|đồng)",
                                                java.util.regex.Pattern.CASE_INSENSITIVE
                                                                | java.util.regex.Pattern.UNICODE_CASE)
                                .matcher(value);
                StringBuffer result = new StringBuffer();
                while (matcher.find()) {
                        String formatted = new java.text.DecimalFormat("#,###")
                                        .format(new java.math.BigInteger(matcher.group(1))).replace(',', '.');
                        matcher.appendReplacement(result,
                                        java.util.regex.Matcher.quoteReplacement(formatted + " VNĐ"));
                }
                matcher.appendTail(result);
                return result.toString();
        }

        private String replaceModelFit(String value) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                .compile("(?i)\\brSquared\\s*[:=]?\\s*(0(?:\\.\\d+)?|1(?:\\.0+)?)")
                                .matcher(value);
                StringBuffer result = new StringBuffer();
                while (matcher.find()) {
                        double percent = Double.parseDouble(matcher.group(1)) * 100;
                        String replacement = "độ phù hợp của mô hình "
                                        + String.format(java.util.Locale.forLanguageTag("vi-VN"), "%.0f%%", percent);
                        matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                }
                matcher.appendTail(result);
                return result.toString();
        }

        private String replaceTrendCoefficient(String value) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                .compile("(?i)\\bslope\\s*[:=]?\\s*(-?\\d+(?:\\.\\d+)?(?:e[+-]?\\d+)?)")
                                .matcher(value);
                StringBuffer result = new StringBuffer();
                while (matcher.find()) {
                        double coefficient;
                        try {
                                coefficient = Double.parseDouble(matcher.group(1));
                        } catch (NumberFormatException ignored) {
                                coefficient = 0;
                        }
                        String replacement = coefficient < 0 ? "xu hướng đang giảm"
                                        : coefficient > 0 ? "xu hướng đang tăng" : "xu hướng đi ngang";
                        matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                }
                matcher.appendTail(result);
                return result.toString();
        }

        private String allowedValue(String value, Set<String> allowed, String fallback) {
                String normalized = value == null ? "" : value.trim().toUpperCase();
                return allowed.contains(normalized) ? normalized : fallback;
        }

        private String providerErrorMessage(int statusCode) {
                return switch (statusCode) {
                        case 400 -> "Gemini từ chối dữ liệu phân tích hoặc cấu hình model chưa hợp lệ.";
                        case 401, 403 -> "Gemini API key không hợp lệ hoặc chưa được cấp quyền.";
                        case 404 -> "Model Gemini phân tích không còn khả dụng.";
                        case 429 -> "Đã chạm quota Gemini miễn phí. Hãy dùng lại báo cáo cache hoặc thử sau.";
                        default -> "Dịch vụ Gemini đang gián đoạn. Vui lòng thử lại sau.";
                };
        }

        private record CacheKey(LocalDate fromDate, LocalDate toDate) {
        }

        private record RegressionMetric(double slope, double intercept, double rmse, double rSquared) {
        }

}
