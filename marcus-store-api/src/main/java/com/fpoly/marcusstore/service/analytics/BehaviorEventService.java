package com.fpoly.marcusstore.service.analytics;

import com.fpoly.marcusstore.repository.analytics.BehaviorEventRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.time.LocalDate;
import com.fpoly.marcusstore.dto.analytics.BehaviorFunnelResponse;
import java.util.List;
import com.fpoly.marcusstore.repository.analytics.BehaviorEventRepository.TopAiProductClick;

@Service
@RequiredArgsConstructor
public class BehaviorEventService {
    private static final Set<String> CLIENT_EVENTS = Set.of("PRODUCT_VIEW", "CART_ADDED", "CHECKOUT_STARTED");
    private final BehaviorEventRepository repository;
    private final ProductRepository productRepository;

    @Value("${analytics.behavior.retention-days:365}")
    private int retentionDays;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordClient(String eventType, String sessionId, Integer productId) {
        if (!CLIENT_EVENTS.contains(eventType))
            throw new IllegalArgumentException("Sự kiện hành vi không hợp lệ");
        // Marcus thêm: PRODUCT_VIEW phải trỏ tới sản phẩm thật; không cho request
        // public tự bơm productId rác làm sai báo cáo hành vi.
        if ("PRODUCT_VIEW".equals(eventType)) {
            if (productId == null || productId <= 0) {
                throw new IllegalArgumentException("Sản phẩm cần ghi nhận không hợp lệ");
            }
            if (!productRepository.existsById(productId)) {
                throw new IllegalArgumentException("Sản phẩm cần ghi nhận không tồn tại");
            }
        }
        save(eventType, sessionId, productId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSystem(String eventType, Integer orderId) {
        if (!Set.of("ORDER_CREATED", "PAYMENT_SUCCESS").contains(eventType))
            return;
        String sessionId = "PAYMENT_SUCCESS".equals(eventType)
                ? repository.findSessionByOrderId(orderId)
                : null;
        save(eventType, sessionId, null, orderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrderCreated(Integer orderId, String sessionId) {
        save("ORDER_CREATED", sessionId, null, orderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAi(String eventType, String sessionId, Integer productId) {
        if (!Set.of("AI_QUESTION", "AI_PRODUCT_CLICK").contains(eventType))
            return;
        save(eventType, sessionId, productId, null);
    }

    private void save(String type, String sessionId, Integer productId, Integer orderId) {
        // Marcus thêm: chỉ UUID ngẫu nhiên; không lưu IP, token, câu hỏi hay userId.
        if (sessionId != null && !sessionId.matches("^[0-9a-fA-F-]{36}$"))
            sessionId = null;
        repository.insert(type, sessionId, productId, orderId);
    }

    @Scheduled(cron = "0 40 3 * * *")
    @Transactional
    public void cleanupExpired() {
        repository.deleteExpired(Math.max(30, retentionDays));
    }

    @Transactional(readOnly = true)
    public BehaviorFunnelResponse funnel(LocalDate from, LocalDate to) {
        LocalDate safeTo = to == null ? LocalDate.now() : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(29) : from;
        if (safeFrom.isAfter(safeTo) || safeFrom.isBefore(safeTo.minusYears(2)))
            throw new IllegalArgumentException("Khoảng ngày funnel không hợp lệ");
        long[] v = repository.funnel(safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay());
        return new BehaviorFunnelResponse(v[0], v[1], v[2], v[3], v[4], v[5],
                rate(v[1], v[0]), rate(v[2], v[1]), rate(v[3], v[2]), rate(v[5], v[4]));
    }

    @Transactional(readOnly = true)
    public List<TopAiProductClick> topAiProductClicks() {
        return repository.findTopAiProductClicks();
    }

    private double rate(long value, long base) {
        return base == 0 ? 0 : Math.round(value * 1000.0 / base) / 10.0;
    }
}
