package com.fpoly.marcusstore.controller.publicapi;

import com.fpoly.marcusstore.dto.analytics.BehaviorEventRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.analytics.BehaviorEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/behavior")
@RequiredArgsConstructor
public class BehaviorEventController {
    private final BehaviorEventService service;

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<String>> record(@Valid @RequestBody BehaviorEventRequest request) {
        service.recordClient(request.getEventType(), request.getSessionId(), request.getProductId());
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận."));
    }
}
