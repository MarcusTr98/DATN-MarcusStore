package com.fpoly.marcusstore.service.ai;

import com.fpoly.marcusstore.dto.ai.AiProductClickRequest;
import com.fpoly.marcusstore.entity.analytics.AiProductClick;
import com.fpoly.marcusstore.repository.analytics.AiProductClickRepository;
import com.fpoly.marcusstore.repository.analytics.AiProductClickRepository.AiProductClickStatProjection;
import com.fpoly.marcusstore.repository.core.HomeProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AiProductClickService {

    private final AiProductClickRepository clickRepository;
    private final HomeProductRepository productRepository;

    @Transactional
    public void track(AiProductClickRequest request) {
        if (!productRepository.existsById(request.getProductId())) {
            throw new ResponseStatusException(NOT_FOUND, "Sản phẩm không tồn tại.");
        }

        // Marcus thêm: chống double-click và refresh làm sai thống kê.
        boolean duplicated = clickRepository.existsBySessionIdAndProductIdAndClickedAtAfter(
                request.getSessionId(), request.getProductId(), LocalDateTime.now().minusHours(1));
        if (duplicated) {
            return;
        }

        AiProductClick click = new AiProductClick();
        click.setProductId(request.getProductId());
        click.setSessionId(request.getSessionId());
        clickRepository.save(click);
    }

    @Transactional(readOnly = true)
    public List<AiProductClickStatProjection> topProducts() {
        return clickRepository.findTopClickedProducts();
    }
}
