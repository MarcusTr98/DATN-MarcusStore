package com.fpoly.marcusstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderAssignmentScheduler {
    private final OrderAssignmentService orderAssignmentService;

    @Scheduled(initialDelayString = "${order.assignment.initial-delay-ms:60000}", fixedDelayString = "${order.assignment.scan-delay-ms:30000}")
    public void assignDueOrders() {
        orderAssignmentService.assignDueOrders();
    }
}
