package com.fpoly.marcusstore.service.analytics;

import com.fpoly.marcusstore.repository.analytics.BehaviorEventRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Marcus thêm: bảo vệ độ tin cậy của funnel hành vi do module Marcus ghi nhận.
class BehaviorEventServiceTest {

    private BehaviorEventRepository behaviorRepository;
    private ProductRepository productRepository;
    private BehaviorEventService service;

    @BeforeEach
    void setUp() {
        behaviorRepository = mock(BehaviorEventRepository.class);
        productRepository = mock(ProductRepository.class);
        service = new BehaviorEventService(behaviorRepository, productRepository);
    }

    @Test
    void rejectsProductViewWhenProductDoesNotExist() {
        when(productRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> service.recordClient("PRODUCT_VIEW", validSession(), 999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không tồn tại");

        verify(behaviorRepository, never()).insert(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordsExistingProductViewAndAllowsCheckoutWithoutProductId() {
        when(productRepository.existsById(10)).thenReturn(true);

        service.recordClient("PRODUCT_VIEW", validSession(), 10);
        service.recordClient("CHECKOUT_STARTED", validSession(), null);

        verify(behaviorRepository).insert("PRODUCT_VIEW", validSession(), 10, null);
        verify(behaviorRepository).insert("CHECKOUT_STARTED", validSession(), null, null);
    }

    private String validSession() {
        return "123e4567-e89b-42d3-a456-426614174000";
    }
}
