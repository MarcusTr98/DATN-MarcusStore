package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderTransactionService {
    private final OrderTransactionRepository transactionRepository;

    @Transactional
    public void recordTransaction(Order order, BigDecimal amount, String type, String status, String note) {
        OrderTransaction trans = OrderTransaction.builder()
                .order(order)
                .amount(amount)
                .type(type)
                .status(status)
                .note(note)
                .build();
        transactionRepository.save(trans);
    }
}